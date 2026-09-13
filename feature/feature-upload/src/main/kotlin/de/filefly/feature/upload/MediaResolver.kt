package de.filefly.feature.upload

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

// Löst aus einem content:// URI den Anzeigenamen + die Größe auf (OpenableColumns).
// Fällt auf den letzten Pfadsegment-Namen und Größe 0 zurück, wenn Metadaten fehlen.
object MediaResolver {
    fun resolve(
        resolver: ContentResolver,
        uri: Uri,
    ): MediaItem {
        var name = uri.lastPathSegment ?: "datei"
        var size = 0L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0 && !cursor.isNull(nameIdx)) name = cursor.getString(nameIdx)
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                    }
                }
        }
        return MediaItem(
            uri = uri,
            displayName = name,
            sizeBytes = size,
            mimeType = resolver.getType(uri),
        )
    }
}
