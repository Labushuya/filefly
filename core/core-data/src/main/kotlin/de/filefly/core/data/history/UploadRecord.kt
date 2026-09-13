package de.filefly.core.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

// Ein Eintrag der Upload-Historie. status: PENDING | UPLOADING | SUCCESS | FAILED | SKIPPED.
@Entity(tableName = "upload_history")
data class UploadRecord(
    @PrimaryKey val id: String,
    val fileName: String,
    val sizeBytes: Long,
    val destPath: String,
    val status: String,
    val serverStatus: String? = null, // stored | renamed | overwritten | skipped
    val errorMessage: String? = null,
    val createdAt: Long,
    val finishedAt: Long? = null,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_UPLOADING = "UPLOADING"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_SKIPPED = "SKIPPED"
    }
}
