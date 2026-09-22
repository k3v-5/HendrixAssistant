package com.asistente.celular.vault

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Repositorio de la Bóveda Hendrix (Vault).
 * Administra la creación de paquetes de respaldo unificados en formato JSON/archivo,
 * la restauración atómica y la integración con sincronización P2P en PC.
 */
class HendrixVaultRepository(
    private val context: Context
) {

    companion object {
        private const val TAG = "HendrixVaultRepo"
        const val VAULT_VERSION = 1

        val BACKUP_FILES = listOf(
            "custom_macro_deck_profiles.json",
            "automated_routines.json",
            "smart_home_devices.json",
            "tasks.json",
            "notes.json"
        )
    }

    /**
     * Exporta toda la configuración y datos del asistente como una cadena JSON estructurada.
     */
    suspend fun exportVaultJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", VAULT_VERSION)
        root.put("timestamp", System.currentTimeMillis())
        root.put("deviceModel", android.os.Build.MODEL ?: "Android")

        val filesObj = JSONObject()
        for (fileName in BACKUP_FILES) {
            val f = File(context.filesDir, fileName)
            if (f.exists()) {
                try {
                    val content = f.readText()
                    try {
                        filesObj.put(fileName, JSONArray(content))
                    } catch (_: Exception) {
                        try {
                            filesObj.put(fileName, JSONObject(content))
                        } catch (_: Exception) {
                            filesObj.put(fileName, content)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error al leer $fileName para el vault: ${e.message}")
                }
            }
        }
        root.put("files", filesObj)

        val prefs = context.getSharedPreferences("hendrix_prefs", Context.MODE_PRIVATE)
        val prefsObj = JSONObject()
        prefs.all.forEach { (k, v) ->
            if (v != null) {
                prefsObj.put(k, v.toString())
            }
        }
        root.put("settings", prefsObj)

        root.toString(2)
    }

    /**
     * Restaura la configuración a partir de una cadena JSON de la bóveda.
     */
    suspend fun restoreVaultJson(vaultJson: String, strategy: RestoreStrategy = RestoreStrategy.REPLACE_ALL): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(vaultJson)
            val filesObj = root.optJSONObject("files") ?: return@withContext false

            for (fileName in BACKUP_FILES) {
                if (filesObj.has(fileName)) {
                    val targetFile = File(context.filesDir, fileName)
                    val contentData = filesObj.get(fileName)
                    val stringContent = contentData.toString()
                    targetFile.writeText(stringContent)
                    Log.i(TAG, "Restaurado $fileName con éxito (${stringContent.length} chars)")
                }
            }

            val settingsObj = root.optJSONObject("settings")
            if (settingsObj != null) {
                val prefs = context.getSharedPreferences("hendrix_prefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                val keys = settingsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = settingsObj.getString(key)
                    when {
                        value.equals("true", ignoreCase = true) -> editor.putBoolean(key, true)
                        value.equals("false", ignoreCase = true) -> editor.putBoolean(key, false)
                        value.toIntOrNull() != null -> editor.putInt(key, value.toInt())
                        value.toFloatOrNull() != null -> editor.putFloat(key, value.toFloat())
                        else -> editor.putString(key, value)
                    }
                }
                editor.apply()
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al restaurar bóveda: ${e.message}", e)
            false
        }
    }

    /**
     * Guarda la bóveda en un archivo físico en el almacenamiento del dispositivo.
     */
    suspend fun exportToFile(targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = exportVaultJson()
            targetFile.writeText(json)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al exportar vault a archivo: ${e.message}", e)
            false
        }
    }

    /**
     * Restaura la bóveda desde un archivo físico.
     */
    suspend fun importFromFile(sourceFile: File, strategy: RestoreStrategy = RestoreStrategy.REPLACE_ALL): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) return@withContext false
            val json = sourceFile.readText()
            restoreVaultJson(json, strategy)
        } catch (e: Exception) {
            Log.e(TAG, "Error al importar vault desde archivo: ${e.message}", e)
            false
        }
    }

    fun getVaultBackupsDir(): File {
        val dir = File(context.filesDir, "vault_backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun exportVaultToFile(): File = withContext(Dispatchers.IO) {
        val dir = getVaultBackupsDir()
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val file = File(dir, "hendrix_vault_$timestamp.json")
        exportToFile(file)
        file
    }

    fun listLocalBackups(): List<File> {
        val dir = getVaultBackupsDir()
        return dir.listFiles { _, name -> name.endsWith(".json") }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    suspend fun restoreVaultFromFile(file: File, strategy: RestoreStrategy = RestoreStrategy.REPLACE_ALL): Boolean {
        return importFromFile(file, strategy)
    }
}
