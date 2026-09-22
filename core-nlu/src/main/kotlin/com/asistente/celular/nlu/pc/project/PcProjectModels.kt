package com.asistente.celular.nlu.pc.project

/**
 * Categorías de proyectos creativos en la estación de trabajo.
 */
enum class PcProjectCategory(val displayName: String, val iconEmoji: String) {
    AUDIO_DAW("Audio & DAWs", "🎹"),
    THREE_D_VFX("3D & VFX", "🧊"),
    VIDEO_DESIGN("Video & Diseño", "🎨"),
    CODE_DEV("Desarrollo & Código", "💻"),
    OTHER("Otros", "📁")
}

/**
 * Representa un archivo de proyecto creativo descubierto en la PC.
 */
data class PcProjectItem(
    val id: String,
    val name: String,
    val path: String,
    val category: PcProjectCategory,
    val extension: String,
    val sizeBytes: Long = 0L,
    val lastModifiedEpoch: Long = System.currentTimeMillis(),
    val iconEmoji: String = resolveIcon(category, extension)
) {
    companion object {
        fun resolveCategory(extension: String): PcProjectCategory {
            val ext = extension.lowercase().removePrefix(".")
            return when (ext) {
                "als", "flp", "cpr", "rpp", "ptx", "logic" -> PcProjectCategory.AUDIO_DAW
                "blend", "uproject", "unity", "c4d", "max", "ma", "mb", "fbx", "obj" -> PcProjectCategory.THREE_D_VFX
                "prproj", "aep", "psd", "ai", "drp", "veg" -> PcProjectCategory.VIDEO_DESIGN
                "py", "kt", "ts", "js", "cpp", "rs", "workspace" -> PcProjectCategory.CODE_DEV
                else -> PcProjectCategory.OTHER
            }
        }

        fun resolveIcon(category: PcProjectCategory, extension: String): String {
            val ext = extension.lowercase().removePrefix(".")
            return when (ext) {
                "als" -> "🎹"
                "flp" -> "🍊"
                "blend" -> "🧊"
                "uproject" -> "🎮"
                "prproj" -> "🎬"
                "aep" -> "✨"
                "psd" -> "🖼️"
                else -> category.iconEmoji
            }
        }
    }
}
