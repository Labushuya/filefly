package de.filefly.feature.upload

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.filefly.common.ConflictStrategy
import de.filefly.common.Role
import de.filefly.core.data.SettingsStore
import de.filefly.core.data.history.UploadHistoryRepository
import de.filefly.core.data.history.UploadRecord
import de.filefly.core.network.ApiResult
import de.filefly.core.network.ChunkedUploader
import de.filefly.core.network.FileEntry
import de.filefly.core.network.FileFlyApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

// Orchestriert den Bulk-Upload: Verzeichniswahl (Server-Listing, Rechte beachten),
// Konflikt-Auflösung (pro Datei oder "für alle"), gechunkter Upload mit Fortschritt,
// Persistenz in die Upload-Historie. Der ContentResolver kommt vom Screen.
class UploadViewModel(
    private val resolver: ContentResolver,
    private val api: FileFlyApi,
    private val uploader: ChunkedUploader,
    private val settings: SettingsStore,
    private val history: UploadHistoryRepository,
) : ViewModel() {
    data class UiState(
        val items: List<UploadProgress> = emptyList(),
        val currentPath: String = "",
        val basePath: String = "",
        val role: Role = Role.UNKNOWN,
        val entries: List<FileEntry> = emptyList(),
        val existingNames: Set<String> = emptySet(),
        val writable: Boolean = false,
        val loadingListing: Boolean = false,
        val uploading: Boolean = false,
        val error: String? = null,
        // Aktiver Konflikt-Dialog (falls eine Datei bereits existiert).
        val pendingConflict: PendingConflict? = null,
        val allDone: Boolean = false,
    )

    data class PendingConflict(
        val item: MediaItem,
        val onResolve: (ConflictStrategy, applyToAll: Boolean) -> Unit,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // "Für alle übernehmen" gewählte Strategie (gilt für restliche Konflikte im Batch).
    private var bulkStrategy: ConflictStrategy? = null

    // Vom Share-Sheet übergebene URIs vormerken.
    fun setItems(items: List<MediaItem>) {
        _state.value =
            _state.value.copy(
                items = items.map { UploadProgress(it, 0, UploadProgress.Status.PENDING) },
            )
    }

    // Startzustand: base_path + Rolle laden, dann Wurzel listen.
    fun start() {
        viewModelScope.launch {
            val base = settings.basePath.first()
            val role = settings.role.first()
            _state.value = _state.value.copy(basePath = base, role = role, currentPath = base)
            loadListing(base)
        }
    }

    fun loadListing(path: String) {
        _state.value = _state.value.copy(loadingListing = true, error = null)
        viewModelScope.launch {
            when (val result = api.list(path)) {
                is ApiResult.Ok ->
                    _state.value =
                        _state.value.copy(
                            loadingListing = false,
                            currentPath = result.value.path,
                            entries = result.value.entries.filter { it.isDir },
                            // Alle Namen im Zielverzeichnis (für Konflikt-Erkennung vor Upload).
                            existingNames = result.value.entries.map { it.name }.toSet(),
                            writable = result.value.writable,
                        )
                is ApiResult.Failure ->
                    _state.value = _state.value.copy(loadingListing = false, error = result.message)
            }
        }
    }

    fun navigateInto(dir: FileEntry) = loadListing(dir.path)

    // Ein Verzeichnis nach oben, aber nie über base_path hinaus.
    fun navigateUp() {
        val current = _state.value.currentPath
        val base = _state.value.basePath
        if (current == base || current.isEmpty()) return
        val parent = current.substringBeforeLast('/', missingDelimiterValue = base).ifEmpty { base }
        loadListing(if (parent.length < base.length) base else parent)
    }

    fun createDirectory(name: String) {
        if (!_state.value.role.canMkdir || name.isBlank()) return
        viewModelScope.launch {
            when (val result = api.mkdir(_state.value.currentPath, name.trim())) {
                is ApiResult.Ok -> loadListing(_state.value.currentPath)
                is ApiResult.Failure -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }

    // Startet den Bulk-Upload aller PENDING-Items ins aktuelle Verzeichnis.
    fun uploadAll() {
        if (_state.value.uploading) return
        if (!_state.value.role.canUpload) {
            _state.value = _state.value.copy(error = "Diese Einladung erlaubt keinen Upload.")
            return
        }
        bulkStrategy = null
        _state.value = _state.value.copy(uploading = true, error = null, allDone = false)
        viewModelScope.launch {
            val dest = _state.value.currentPath
            val chunkBytes = settings.chunkSizeKb.first() * 1024
            for ((index, progress) in _state.value.items.withIndex()) {
                if (progress.status == UploadProgress.Status.SUCCESS ||
                    progress.status == UploadProgress.Status.SKIPPED
                ) {
                    continue
                }
                uploadOne(index, progress.item, dest, chunkBytes)
            }
            _state.value = _state.value.copy(uploading = false, allDone = true)
        }
    }

    private suspend fun uploadOne(
        index: Int,
        item: MediaItem,
        dest: String,
        chunkBytes: Int,
    ) {
        // Konflikt nur, wenn der Name im Zielverzeichnis existiert. Sonst direkt hochladen
        // (Server bekommt OVERWRITE als No-op-Strategie, da ohnehin kein Konflikt vorliegt).
        val hasConflict = item.displayName in _state.value.existingNames
        val strategy =
            when {
                !hasConflict -> ConflictStrategy.OVERWRITE
                bulkStrategy != null -> bulkStrategy!!
                else -> promptConflict(item)
            }

        // Bei SKIP nicht hochladen — direkt als übersprungen markieren.
        if (hasConflict && strategy == ConflictStrategy.SKIP) {
            updateItem(index) { it.copy(status = UploadProgress.Status.SKIPPED, serverStatus = "skipped") }
            return
        }

        updateItem(index) { it.copy(status = UploadProgress.Status.UPLOADING, bytesSent = 0) }

        val recordId = UUID.randomUUID().toString()
        history.record(
            UploadRecord(
                id = recordId,
                fileName = item.displayName,
                sizeBytes = item.sizeBytes,
                destPath = dest,
                status = UploadRecord.STATUS_UPLOADING,
                createdAt = System.currentTimeMillis(),
            ),
        )

        val result =
            uploader.upload(
                filename = item.displayName,
                totalSize = item.sizeBytes,
                destPath = dest,
                conflictStrategy = strategy,
                chunkSize = chunkBytes,
                openStream = {
                    resolver.openInputStream(item.uri)
                        ?: error("Konnte ${item.displayName} nicht öffnen")
                },
                onProgress = { sent, _ -> updateItem(index) { it.copy(bytesSent = sent) } },
            )

        when (result) {
            is ApiResult.Ok -> {
                val skipped = result.value.status == "skipped"
                updateItem(index) {
                    it.copy(
                        status = if (skipped) UploadProgress.Status.SKIPPED else UploadProgress.Status.SUCCESS,
                        serverStatus = result.value.status,
                        bytesSent = item.sizeBytes,
                    )
                }
                history.record(
                    historyRecord(recordId, item, dest, if (skipped) UploadRecord.STATUS_SKIPPED else UploadRecord.STATUS_SUCCESS)
                        .copy(serverStatus = result.value.status, finishedAt = System.currentTimeMillis()),
                )
            }
            is ApiResult.Failure -> {
                updateItem(index) {
                    it.copy(status = UploadProgress.Status.FAILED, error = result.message)
                }
                history.record(
                    historyRecord(recordId, item, dest, UploadRecord.STATUS_FAILED)
                        .copy(errorMessage = result.message, finishedAt = System.currentTimeMillis()),
                )
            }
        }
    }

    private fun historyRecord(
        id: String,
        item: MediaItem,
        dest: String,
        status: String,
    ) = UploadRecord(
        id = id,
        fileName = item.displayName,
        sizeBytes = item.sizeBytes,
        destPath = dest,
        status = status,
        createdAt = System.currentTimeMillis(),
    )

    // Öffnet den Konflikt-Dialog und wartet (suspend) auf die Nutzerentscheidung.
    // Wird nur bei echtem Namenskonflikt aufgerufen. "Für alle übernehmen" merkt sich
    // die Strategie für die restlichen Konflikte im selben Batch (bulkStrategy).
    private suspend fun promptConflict(item: MediaItem): ConflictStrategy =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            _state.value =
                _state.value.copy(
                    pendingConflict =
                        PendingConflict(item) { strategy, applyToAll ->
                            if (applyToAll) bulkStrategy = strategy
                            _state.value = _state.value.copy(pendingConflict = null)
                            if (cont.isActive) cont.resumeWith(Result.success(strategy))
                        },
                )
            cont.invokeOnCancellation { _state.value = _state.value.copy(pendingConflict = null) }
        }

    private fun updateItem(
        index: Int,
        transform: (UploadProgress) -> UploadProgress,
    ) {
        _state.value =
            _state.value.copy(
                items = _state.value.items.mapIndexed { i, p -> if (i == index) transform(p) else p },
            )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
