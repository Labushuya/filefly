package de.filefly.feature.upload

import android.net.Uri

// Ein zum Upload vorgemerktes Medium (aus dem Share-Sheet oder Picker). Die Bytes
// werden erst beim Upload über den ContentResolver gestreamt (kein Vorab-Kopieren).
data class MediaItem(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String?,
)

// Fortschritt eines einzelnen Uploads.
data class UploadProgress(
    val item: MediaItem,
    val bytesSent: Long,
    val status: Status,
    val serverStatus: String? = null,
    val error: String? = null,
) {
    enum class Status { PENDING, UPLOADING, SUCCESS, FAILED, SKIPPED }

    val fraction: Float
        get() = if (item.sizeBytes > 0) (bytesSent.toFloat() / item.sizeBytes).coerceIn(0f, 1f) else 0f
}
