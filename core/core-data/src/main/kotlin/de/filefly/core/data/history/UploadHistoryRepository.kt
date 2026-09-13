package de.filefly.core.data.history

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

// Kapselt Room hinter einer schlanken API — :app kennt keine Room-Typen direkt.
class UploadHistoryRepository(
    private val dao: UploadDao,
) {
    fun observeAll(): Flow<List<UploadRecord>> = dao.observeAll()

    suspend fun record(record: UploadRecord) = dao.upsert(record)

    suspend fun byId(id: String): UploadRecord? = dao.byId(id)

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun clear() = dao.clear()

    companion object {
        fun create(context: Context): UploadHistoryRepository {
            val db =
                Room.databaseBuilder(
                    context.applicationContext,
                    FileFlyDatabase::class.java,
                    "filefly.db",
                ).build()
            return UploadHistoryRepository(db.uploadDao())
        }
    }
}
