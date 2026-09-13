package de.filefly.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.filefly.core.data.history.UploadHistoryRepository
import de.filefly.core.data.history.UploadRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Upload-Verlauf aus Room. Zeigt alle Einträge, erlaubt Löschen einzelner Einträge
// und Leeren der Historie. Das Wiederholen fehlgeschlagener Uploads passiert über den
// Upload-Screen (dieser Screen ist read-only + Verwaltung).
class HistoryViewModel(
    private val repo: UploadHistoryRepository,
) : ViewModel() {
    val records: StateFlow<List<UploadRecord>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clear() }
    }
}
