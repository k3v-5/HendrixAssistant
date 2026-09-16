package com.asistente.celular.nlu.files

/**
 * Categorías de archivos detectables mediante lenguaje natural.
 */
enum class FileCategory(val displayName: String) {
    DOCUMENT("Documento"),
    IMAGE("Imagen"),
    AUDIO("Audio"),
    VIDEO("Video"),
    DOWNLOAD("Descarga"),
    ARCHIVE("Comprimido"),
    OTHER("Archivo")
}

/**
 * Representación inmutable de un archivo encontrado en el dispositivo móvil.
 */
data class LocalFileItem(
    val name: String,
    val path: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val lastModifiedEpochMs: Long,
    val category: FileCategory
)

/**
 * Consulta estructurada para el motor de búsqueda de archivos.
 */
data class FileSearchQuery(
    val query: String,
    val category: FileCategory? = null,
    val minModifiedTime: Long? = null,
    val maxModifiedTime: Long? = null,
    val limit: Int = 10
)

/**
 * Contrato desacoplado para búsqueda de archivos locales sin salir a la nube.
 */
interface FileSearchEngine {
    fun isAvailable(): Boolean
    suspend fun searchFiles(query: FileSearchQuery): List<LocalFileItem>
}
