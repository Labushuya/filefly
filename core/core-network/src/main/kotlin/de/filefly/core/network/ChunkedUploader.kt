package de.filefly.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InputStream

private val OCTET_STREAM = "application/octet-stream".toMediaType()

// Lädt eine Datei gechunked hoch: init -> N x chunk -> complete. Die Bytes kommen
// aus einem InputStream (die App reicht ContentResolver.openInputStream(uri) durch),
// damit core-network Android-URI-frei bleibt und JVM-testbar ist.
//
// Fortschritt wird pro Chunk über onProgress(bytesSent, totalBytes) gemeldet.
// Konflikt-Behandlung ist bereits in UploadInitRequest.conflictStrategy kodiert;
// der Server entscheidet endgültig bei complete (Status im Ergebnis).
class ChunkedUploader(
    private val client: OkHttpClient,
    private val api: FileFlyApi,
    private val credentials: CredentialProvider,
) {
    data class UploadOutcome(
        val path: String,
        val status: String,
    )

    // Lädt [input] (Länge [totalSize]) nach [destPath]/[filename]. Ruft [onProgress]
    // nach jedem übertragenen Chunk. Gibt das Server-Ergebnis oder einen Fehler zurück.
    suspend fun upload(
        filename: String,
        totalSize: Long,
        destPath: String,
        conflictStrategy: de.filefly.common.ConflictStrategy,
        chunkSize: Int,
        openStream: () -> InputStream,
        onProgress: (bytesSent: Long, total: Long) -> Unit = { _, _ -> },
    ): ApiResult<UploadOutcome> {
        // 1) Session initialisieren.
        val initResult =
            api.uploadInit(
                UploadInitRequest(
                    filename = filename,
                    size = totalSize,
                    destPath = destPath,
                    conflictStrategy = conflictStrategy.wireValue,
                    chunkSize = chunkSize,
                ),
            )
        val init =
            when (initResult) {
                is ApiResult.Ok -> initResult.value
                is ApiResult.Failure -> return initResult
            }

        // Server kann den Konflikt schon bei init melden; bei SKIP dann gar nicht senden.
        if (init.conflict == "exists" && conflictStrategy == de.filefly.common.ConflictStrategy.SKIP) {
            return ApiResult.Ok(UploadOutcome(path = "$destPath/$filename", status = "skipped"))
        }

        val effectiveChunk = init.chunkSize.takeIf { it > 0 } ?: chunkSize

        // 2) Chunks streamen.
        val sendResult =
            withContext(Dispatchers.IO) {
                sendChunks(init.uploadId, effectiveChunk, totalSize, openStream, onProgress)
            }
        if (sendResult is ApiResult.Failure) return sendResult

        // 3) Abschließen (Server reassembled + wendet Konflikt-Strategie an).
        return api.uploadComplete(init.uploadId).map { UploadOutcome(it.path, it.status) }
    }

    private fun sendChunks(
        uploadId: String,
        chunkSize: Int,
        totalSize: Long,
        openStream: () -> InputStream,
        onProgress: (Long, Long) -> Unit,
    ): ApiResult<Unit> {
        var index = 0
        var sent = 0L
        val buffer = ByteArray(chunkSize)
        try {
            openStream().use { stream ->
                while (true) {
                    val read = stream.readFully(buffer)
                    if (read <= 0) break
                    val chunkResult = postChunk(uploadId, index, buffer, read)
                    if (chunkResult is ApiResult.Failure) return chunkResult
                    sent += read
                    index++
                    onProgress(sent, totalSize)
                    if (read < chunkSize) break // letzter (kurzer) Chunk
                }
            }
        } catch (e: IOException) {
            return ApiResult.networkError("Lesefehler beim Upload: ${e.message}")
        }
        return ApiResult.Ok(Unit)
    }

    private fun postChunk(
        uploadId: String,
        index: Int,
        buffer: ByteArray,
        length: Int,
    ): ApiResult<Unit> {
        val part = buffer.copyOf(length).toRequestBody(OCTET_STREAM)
        val body =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_id", uploadId)
                .addFormDataPart("chunk_index", index.toString())
                .addFormDataPart("chunk", "chunk_$index", part)
                .build()
        val builder =
            Request.Builder()
                .url(credentials.baseUrl() + "/files/upload/chunk")
                .post(body)
        credentials.token()?.let { builder.header("Authorization", "Bearer $it") }
        return try {
            client.newCall(builder.build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    ApiResult.Ok(Unit)
                } else {
                    ApiResult.Failure(resp.code, "Chunk $index abgelehnt: HTTP ${resp.code}")
                }
            }
        } catch (e: IOException) {
            ApiResult.networkError("Chunk $index fehlgeschlagen: ${e.message}")
        }
    }
}

// Liest so viele Bytes wie möglich in [buffer] (bis voll oder Stream-Ende). Ein
// einzelner read() darf weniger liefern; hier wird bis zur Puffergröße aufgefüllt.
private fun InputStream.readFully(buffer: ByteArray): Int {
    var offset = 0
    while (offset < buffer.size) {
        val n = read(buffer, offset, buffer.size - offset)
        if (n < 0) break
        offset += n
    }
    return offset
}
