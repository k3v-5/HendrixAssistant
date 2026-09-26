package com.asistente.celular.nlu.memory.episodic

import java.util.UUID

/**
 * Utilidad atómica para parsear títulos de ventana y ejecutables de Windows en sesiones
 * de proyectos creativos bien estructuradas (Unreal Engine, Blender, Ableton, FL Studio, VS Code).
 */
object CreativeProjectTitleParser {

    fun parseWindowTitle(windowTitle: String, executableName: String? = null): CreativeSessionEntry? {
        val title = windowTitle.trim()
        if (title.isBlank()) return null
        val lower = title.lowercase()
        val exeLower = executableName?.lowercase() ?: ""

        return when {
            // 1. Unreal Engine
            lower.contains("unreal") || exeLower.contains("unrealeditor") -> {
                val projectName = cleanTitle(title, listOf("unreal editor", "unrealeditor", "unreal engine", "-"))
                CreativeSessionEntry(
                    id = "session_ue_${UUID.randomUUID().toString().take(8)}",
                    appName = "Unreal Engine",
                    projectTitle = projectName.ifBlank { "Proyecto Unreal" },
                    projectPath = extractPath(title)
                )
            }

            // 2. Blender
            lower.contains("blender") || exeLower.contains("blender") -> {
                val projectName = cleanTitle(title, listOf("blender", "*", "-", "[", "]"))
                    .removeSuffix(".blend")
                    .trim()
                CreativeSessionEntry(
                    id = "session_bl_${UUID.randomUUID().toString().take(8)}",
                    appName = "Blender",
                    projectTitle = projectName.ifBlank { "Escena 3D" },
                    projectPath = extractPath(title)
                )
            }

            // 3. Ableton Live
            lower.contains("ableton") || (lower.contains("live") && lower.contains(".als")) || exeLower.contains("ableton") -> {
                val projectName = cleanTitle(title, listOf("ableton live 11 suite", "ableton live", "ableton", "live", "-"))
                    .removeSuffix(".als")
                    .trim()
                CreativeSessionEntry(
                    id = "session_abl_${UUID.randomUUID().toString().take(8)}",
                    appName = "Ableton Live",
                    projectTitle = projectName.ifBlank { "Sesión de Audio" },
                    projectPath = extractPath(title)
                )
            }

            // 4. FL Studio
            lower.contains("fl studio") || lower.contains(".flp") || exeLower.contains("fl64") -> {
                val projectName = cleanTitle(title, listOf("fl studio 21", "fl studio 20", "fl studio", "-"))
                    .removeSuffix(".flp")
                    .trim()
                CreativeSessionEntry(
                    id = "session_fl_${UUID.randomUUID().toString().take(8)}",
                    appName = "FL Studio",
                    projectTitle = projectName.ifBlank { "Proyecto Musical" },
                    projectPath = extractPath(title)
                )
            }

            // 5. Visual Studio Code / Antigravity
            lower.contains("visual studio code") || lower.contains("antigravity") || exeLower.contains("code") -> {
                val projectName = cleanTitle(title, listOf("visual studio code", "antigravity", "-"))
                CreativeSessionEntry(
                    id = "session_code_${UUID.randomUUID().toString().take(8)}",
                    appName = "Visual Studio Code",
                    projectTitle = projectName.ifBlank { "Workspace" },
                    projectPath = extractPath(title)
                )
            }

            else -> null
        }
    }

    private fun cleanTitle(raw: String, removeTerms: List<String>): String {
        var clean = raw
        for (term in removeTerms) {
            clean = clean.replace(Regex("(?i)" + Regex.escape(term)), "")
        }
        // Limpiar corchetes, comillas y barras
        clean = clean.replace(Regex("""[\[\]\(\)"']"""), " ")
        clean = clean.replace(Regex("""^[\\/]+|[\\/]+$"""), "")
        clean = clean.replace(Regex("""\s+"""), " ").trim()

        // Si contiene una ruta de archivo (ej. D:\Assets\Model.blend), extraer el nombre del archivo
        if (clean.contains("\\") || clean.contains("/")) {
            val fileName = clean.substringAfterLast('\\').substringAfterLast('/')
            if (fileName.isNotBlank()) return fileName.trim()
        }

        return clean.trim()
    }

    private fun extractPath(raw: String): String? {
        val regex = Regex("""[a-zA-Z]:\\[^\\/:*?"<>|\r\n]+""")
        return regex.find(raw)?.value
    }
}
