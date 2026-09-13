package de.filefly.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DTOs für die filefly-server API. Feldnamen matchen die JSON-Schlüssel des Servers.

// --- Auth ---

@Serializable
data class InviteValidateRequest(
    val code: String,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    val role: String,
    @SerialName("base_path") val basePath: String = "",
    val permissions: List<String> = emptyList(),
)

// --- Files ---

@Serializable
data class FileEntry(
    val name: String,
    val path: String,
    @SerialName("is_dir") val isDir: Boolean,
    val size: Long = 0,
    @SerialName("modified_at") val modifiedAt: String? = null,
)

@Serializable
data class FileListResponse(
    val path: String,
    val entries: List<FileEntry> = emptyList(),
    val writable: Boolean = false,
)

@Serializable
data class UploadInitRequest(
    val filename: String,
    val size: Long,
    @SerialName("dest_path") val destPath: String,
    // overwrite | rename | skip
    @SerialName("conflict_strategy") val conflictStrategy: String,
    @SerialName("chunk_size") val chunkSize: Int,
)

@Serializable
data class UploadInitResponse(
    @SerialName("upload_id") val uploadId: String,
    @SerialName("chunk_size") val chunkSize: Int,
    // Server meldet ggf. Konflikt schon hier: none | exists
    val conflict: String = "none",
)

@Serializable
data class UploadCompleteRequest(
    @SerialName("upload_id") val uploadId: String,
)

@Serializable
data class UploadCompleteResponse(
    val path: String,
    // stored | renamed | skipped | overwritten
    val status: String,
)

@Serializable
data class MkdirRequest(
    val path: String,
    val name: String,
)

@Serializable
data class RenameRequest(
    @SerialName("from_path") val fromPath: String,
    @SerialName("to_path") val toPath: String,
)

@Serializable
data class DeleteRequest(
    val path: String,
)

@Serializable
data class HealthResponse(
    val status: String,
    val version: String? = null,
)

@Serializable
data class ApiError(
    val detail: String? = null,
)
