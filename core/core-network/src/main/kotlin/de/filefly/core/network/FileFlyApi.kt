package de.filefly.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

// HTTP-Client für den filefly-server. Kapselt Auth-Header, JSON-(De)Serialisierung
// und die Endpunkte. Chunk-Upload liegt in ChunkedUploader (streamt Bytes, nicht
// über JSON). Alle Aufrufe geben ApiResult zurück — kein Werfen nach oben.
class FileFlyApi(
    private val client: OkHttpClient,
    private val credentials: CredentialProvider,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) {
    private val jsonMedia = "application/json".toMediaType()

    // --- Auth ---

    suspend fun validateInvite(code: String): ApiResult<TokenResponse> =
        post("/auth/invite/validate", InviteValidateRequest.serializer(), InviteValidateRequest(code), TokenResponse.serializer(), auth = false)

    // --- System ---

    suspend fun health(): ApiResult<HealthResponse> =
        get("/health", HealthResponse.serializer(), auth = false)

    // --- Files ---

    suspend fun list(path: String): ApiResult<FileListResponse> =
        get("/files/list?path=${encode(path)}", FileListResponse.serializer())

    suspend fun uploadInit(request: UploadInitRequest): ApiResult<UploadInitResponse> =
        post("/files/upload/init", UploadInitRequest.serializer(), request, UploadInitResponse.serializer())

    suspend fun uploadComplete(uploadId: String): ApiResult<UploadCompleteResponse> =
        post("/files/upload/complete", UploadCompleteRequest.serializer(), UploadCompleteRequest(uploadId), UploadCompleteResponse.serializer())

    suspend fun mkdir(path: String, name: String): ApiResult<FileEntry> =
        post("/files/mkdir", MkdirRequest.serializer(), MkdirRequest(path, name), FileEntry.serializer())

    suspend fun rename(fromPath: String, toPath: String): ApiResult<FileEntry> =
        post("/files/rename", RenameRequest.serializer(), RenameRequest(fromPath, toPath), FileEntry.serializer())

    suspend fun delete(path: String): ApiResult<Unit> {
        val body = json.encodeToString(DeleteRequest.serializer(), DeleteRequest(path)).toRequestBody(jsonMedia)
        return request(requestBuilder("/files/delete").delete(body)) { }
    }

    // --- interne Helfer ---

    private suspend fun <T> get(
        endpoint: String,
        serializer: KSerializer<T>,
        auth: Boolean = true,
    ): ApiResult<T> =
        request(requestBuilder(endpoint, auth).get()) { body ->
            json.decodeFromString(serializer, body)
        }

    private suspend fun <B, T> post(
        endpoint: String,
        payloadSerializer: KSerializer<B>,
        payload: B,
        responseSerializer: KSerializer<T>,
        auth: Boolean = true,
    ): ApiResult<T> {
        val bodyJson = json.encodeToString(payloadSerializer, payload)
        return request(requestBuilder(endpoint, auth).post(bodyJson.toRequestBody(jsonMedia))) { body ->
            json.decodeFromString(responseSerializer, body)
        }
    }

    // Führt den Call auf dem IO-Dispatcher aus und mappt HTTP/IO-Fehler auf ApiResult.
    private suspend fun <T> request(
        builder: Request.Builder,
        deserialize: (String) -> T,
    ): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                client.newCall(builder.build()).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        ApiResult.Ok(deserialize(bodyStr))
                    } else {
                        ApiResult.Failure(resp.code, parseError(bodyStr) ?: "HTTP ${resp.code}")
                    }
                }
            } catch (e: IOException) {
                ApiResult.networkError("Netzwerkfehler: ${e.message}")
            } catch (e: kotlinx.serialization.SerializationException) {
                ApiResult.networkError("Antwort nicht lesbar: ${e.message}")
            }
        }

    private fun requestBuilder(endpoint: String, auth: Boolean = true): Request.Builder {
        val builder = Request.Builder().url(credentials.baseUrl() + endpoint)
        if (auth) credentials.token()?.let { builder.header("Authorization", "Bearer $it") }
        return builder
    }

    private fun parseError(body: String): String? =
        runCatching { json.decodeFromString(ApiError.serializer(), body).detail }.getOrNull()

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    companion object {
        // Default-OkHttp-Client mit LAN-freundlichen Timeouts (große Uploads).
        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .readTimeout(java.time.Duration.ofMinutes(5))
                .writeTimeout(java.time.Duration.ofMinutes(10))
                .build()
    }
}
