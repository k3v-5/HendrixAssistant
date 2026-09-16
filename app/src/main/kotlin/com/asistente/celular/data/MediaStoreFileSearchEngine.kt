package com.asistente.celular.data

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.asistente.celular.nlu.files.FileCategory
import com.asistente.celular.nlu.files.FileSearchEngine
import com.asistente.celular.nlu.files.FileSearchQuery
import com.asistente.celular.nlu.files.LocalFileItem

/**
 * Motor de búsqueda de archivos locales en Android utilizando ContentResolver y MediaStore.
 * Permite buscar documentos, PDFs, imágenes y descargas offline por coincidencia de texto y fecha.
 */
class MediaStoreFileSearchEngine(
    private val context: Context
) : FileSearchEngine {

    override fun isAvailable(): Boolean = true

    override suspend fun searchFiles(query: FileSearchQuery): List<LocalFileItem> {
        val results = mutableListOf<LocalFileItem>()
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        // 1. Filtro por nombre / query
        if (query.query.isNotBlank() && query.query != "reciente") {
            selectionParts.add("${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?")
            selectionArgs.add("%${query.query}%")
        }

        // 2. Filtro por categoría MIME
        if (query.category != null) {
            when (query.category) {
                FileCategory.DOCUMENT -> {
                    selectionParts.add("(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?)")
                    selectionArgs.add("%pdf%")
                    selectionArgs.add("%.doc%")
                    selectionArgs.add("%.pdf%")
                }
                FileCategory.IMAGE -> {
                    selectionParts.add("${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?")
                    selectionArgs.add("image/%")
                }
                FileCategory.AUDIO -> {
                    selectionParts.add("${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?")
                    selectionArgs.add("audio/%")
                }
                FileCategory.VIDEO -> {
                    selectionParts.add("${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?")
                    selectionArgs.add("video/%")
                }
                else -> { /* sin filtro extra */ }
            }
        }

        val selection = if (selectionParts.isNotEmpty()) selectionParts.joinToString(" AND ") else null
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT ${query.limit}"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null,
                sortOrder
            )?.use { cursor ->
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext() && results.size < query.limit) {
                    val name = cursor.getString(nameCol) ?: continue
                    val path = if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                    val size = cursor.getLong(sizeCol)
                    val dateModifiedSec = cursor.getLong(dateCol)

                    val inferredCat = when {
                        mime?.startsWith("image/") == true -> FileCategory.IMAGE
                        mime?.startsWith("audio/") == true -> FileCategory.AUDIO
                        mime?.startsWith("video/") == true -> FileCategory.VIDEO
                        name.endsWith(".pdf", ignoreCase = true) || name.endsWith(".docx", ignoreCase = true) ||
                        name.endsWith(".txt", ignoreCase = true) -> FileCategory.DOCUMENT
                        path.contains("Download", ignoreCase = true) -> FileCategory.DOWNLOAD
                        else -> FileCategory.OTHER
                    }

                    results.add(
                        LocalFileItem(
                            name = name,
                            path = path,
                            mimeType = mime,
                            sizeBytes = size,
                            lastModifiedEpochMs = dateModifiedSec * 1000L,
                            category = inferredCat
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando MediaStore", e)
        }

        return results
    }

    companion object {
        private const val TAG = "MediaStoreFileSearch"
    }
}
