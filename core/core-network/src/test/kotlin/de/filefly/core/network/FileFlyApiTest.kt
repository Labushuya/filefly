package de.filefly.core.network

import com.google.common.truth.Truth.assertThat
import de.filefly.common.ConflictStrategy
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

// Testet FileFlyApi + ChunkedUploader gegen einen MockWebServer: Invite-Validierung,
// Fehler-Mapping und der komplette init->chunk->complete Upload-Flow.
class FileFlyApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: FileFlyApi
    private lateinit var credentials: CredentialProvider
    private val client = OkHttpClient()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        credentials =
            object : CredentialProvider {
                override fun baseUrl() = server.url("").toString().trimEnd('/')

                override fun token() = "test-jwt"
            }
        api = FileFlyApi(client, credentials)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `validateInvite parses token response`() =
        runBlocking {
            server.enqueue(
                MockResponse().setBody(
                    """{"access_token":"jwt123","token_type":"bearer","role":"user","base_path":"/fotos","permissions":["upload","mkdir"]}""",
                ),
            )
            val result = api.validateInvite("ABCD1234")
            assertThat(result).isInstanceOf(ApiResult.Ok::class.java)
            val token = (result as ApiResult.Ok).value
            assertThat(token.accessToken).isEqualTo("jwt123")
            assertThat(token.role).isEqualTo("user")
            assertThat(token.basePath).isEqualTo("/fotos")
        }

    @Test
    fun `http error maps to Failure with detail`() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"Invite abgelaufen"}"""))
            val result = api.validateInvite("EXPIRED")
            assertThat(result).isInstanceOf(ApiResult.Failure::class.java)
            val failure = result as ApiResult.Failure
            assertThat(failure.code).isEqualTo(403)
            assertThat(failure.message).isEqualTo("Invite abgelaufen")
        }

    @Test
    fun `health check works without auth`() =
        runBlocking {
            server.enqueue(MockResponse().setBody("""{"status":"ok","version":"0.1.0"}"""))
            val result = api.health()
            assertThat((result as ApiResult.Ok).value.status).isEqualTo("ok")
        }

    @Test
    fun `chunked upload runs init chunk complete`() =
        runBlocking {
            // init -> chunk (x2) -> complete
            server.enqueue(MockResponse().setBody("""{"upload_id":"up1","chunk_size":4,"conflict":"none"}"""))
            server.enqueue(MockResponse().setResponseCode(200))
            server.enqueue(MockResponse().setResponseCode(200))
            server.enqueue(MockResponse().setBody("""{"path":"/fotos/x.jpg","status":"stored"}"""))

            val uploader = ChunkedUploader(client, api, credentials)
            val data = "abcdef".toByteArray() // 6 bytes -> chunkSize 4 => 2 chunks
            var lastSent = 0L
            val result =
                uploader.upload(
                    filename = "x.jpg",
                    totalSize = data.size.toLong(),
                    destPath = "/fotos",
                    conflictStrategy = ConflictStrategy.RENAME,
                    chunkSize = 4,
                    openStream = { data.inputStream() },
                    onProgress = { sent, _ -> lastSent = sent },
                )
            assertThat(result).isInstanceOf(ApiResult.Ok::class.java)
            assertThat((result as ApiResult.Ok).value.status).isEqualTo("stored")
            assertThat(lastSent).isEqualTo(6L)
            // init + 2 chunks + complete = 4 requests
            assertThat(server.requestCount).isEqualTo(4)
        }

    @Test
    fun `skip strategy short-circuits on init conflict`() =
        runBlocking {
            server.enqueue(MockResponse().setBody("""{"upload_id":"up1","chunk_size":4,"conflict":"exists"}"""))
            val uploader = ChunkedUploader(client, api, credentials)
            val data = "abc".toByteArray()
            val result =
                uploader.upload(
                    filename = "x.jpg",
                    totalSize = data.size.toLong(),
                    destPath = "/fotos",
                    conflictStrategy = ConflictStrategy.SKIP,
                    chunkSize = 4,
                    openStream = { data.inputStream() },
                )
            assertThat((result as ApiResult.Ok).value.status).isEqualTo("skipped")
            // nur init, keine chunks/complete
            assertThat(server.requestCount).isEqualTo(1)
        }
}
