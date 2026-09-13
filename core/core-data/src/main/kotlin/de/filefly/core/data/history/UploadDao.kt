package de.filefly.core.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadDao {
    @Query("SELECT * FROM upload_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UploadRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: UploadRecord)

    @Query("SELECT * FROM upload_history WHERE id = :id")
    suspend fun byId(id: String): UploadRecord?

    @Query("DELETE FROM upload_history WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM upload_history")
    suspend fun clear()
}
