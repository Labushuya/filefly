package de.filefly.feature.updater

import de.filefly.common.isUpdateAvailable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

// Prüft latest.json und entscheidet, ob ein Update verfügbar ist. Downgrade wird
// abgelehnt. Netzwerk über OkHttp; JSON-Parsing über kotlinx.serialization.
// Die eigentliche Installation macht PackageInstallerSession (braucht Android).
class UpdateChecker(
    private val client: OkHttpClient,
    private val json: Json,
    private val latestJsonUrl: String,
) {
    sealed interface Result {
        data class UpdateAvailable(
            val manifest: LatestManifest,
        ) : Result

        data object UpToDate : Result

        data class Error(
            val message: String,
        ) : Result
    }

    // currentVersion = installierte versionName.
    fun check(currentVersion: String): Result {
        val request = Request.Builder().url(latestJsonUrl).build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.Error("HTTP ${resp.code}")
                }
                val body = resp.body?.string() ?: return Result.Error("Leere Antwort")
                val manifest = json.decodeFromString(LatestManifest.serializer(), body)
                if (isUpdateAvailable(current = currentVersion, remote = manifest.versionName)) {
                    Result.UpdateAvailable(manifest)
                } else {
                    Result.UpToDate
                }
            }
        } catch (e: IOException) {
            Result.Error("Netzwerkfehler: ${e.message}")
        } catch (e: kotlinx.serialization.SerializationException) {
            Result.Error("Manifest-Fehler: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Result.Error("Manifest ungültig: ${e.message}")
        }
    }
}
