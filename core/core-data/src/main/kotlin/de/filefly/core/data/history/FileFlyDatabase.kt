package de.filefly.core.data.history

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UploadRecord::class], version = 1, exportSchema = true)
abstract class FileFlyDatabase : RoomDatabase() {
    abstract fun uploadDao(): UploadDao
}
