package com.asistente.celular.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.util.Log
import com.asistente.celular.nlu.backup.BackupMetadata
import com.asistente.celular.nlu.backup.BackupOperationResult
import com.asistente.celular.nlu.backup.DataBackupCoordinator
import com.asistente.celular.nlu.backup.FullAppStateBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Gestor centralizado de persistencia resiliente, respaldos atómicos y auto-restauración
 * para la aplicación móvil Hendrix Assistant.
 *
 * Resuelve definitivamente la pérdida de datos al actualizar o reinstalar la app:
 * 1. Respaldo continuo de todas las preferencias y bases de datos JSON.
 * 2. Almacenamiento redundante en almacenamiento privado y en almacenamiento persistente de documentos.
 * 3. Detección proactiva de instalación limpia con auto-restauración en milisegundos.
 * 4. Exportación e importación manual a formato JSON estándar.
 */
class DataPersistenceBackupManager(
    private val context: Context,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
) : DataBackupCoordinator {

    companion object {
        private const val TAG = "DataPersistenceBackup"
        private const val BACKUP_FILE_NAME = "hendrix_latest_backup.json"
        private const val PREFS_SETTINGS = "asistente_celular_settings"
        private const val PREFS_PC_REMOTE = "pc_remote_prefs"
        private const val PREFS_PC_MODULES = "pc_modules_prefs"

        private val DATA_JSON_FILES = listOf(
            "notes.json",
            "tasks.json",
            "routines.json",
            "expenses.json",
            "smarthome_devices.json",
            "hendrix_vault_encrypted.json",
            "automated_routines.json",
            "macro_deck_profiles.json",
            "knowledge_graph.json"
        )
    }

    private val mutex = Mutex()

    /**
     * Directorios donde se guarda el snapshot persistente en orden de prioridad.
     */
    private fun getBackupDirectories(): List<File> {
        val dirs = mutableListOf<File>()

        // 1. Almacenamiento externo de la app (Scoped Storage)
        context.getExternalFilesDir(null)?.let { ext ->
            val extBackupDir = File(ext, "backups")
            if (extBackupDir.exists() || extBackupDir.mkdirs()) {
                dirs.add(extBackupDir)
            }
        }

        // 2. Almacenamiento público en Documentos/HendrixAssistant/backups (si es accesible)
        try {
            val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (docs != null) {
                val publicDir = File(docs, "HendrixAssistant/backups")
                if (publicDir.exists() || publicDir.mkdirs()) {
                    dirs.add(publicDir)
                }
            }
        } catch (_: Exception) {
            // Ignorar restricciones en versiones estrictas de Android
        }

        // 3. Fallback en filesDir interno
        val internalDir = File(context.filesDir, "backups")
        if (internalDir.exists() || internalDir.mkdirs()) {
            dirs.add(internalDir)
        }

        return dirs
    }

    override suspend fun createFullBackup(source: String): FullAppStateBackup = withContext(Dispatchers.IO) {
        mutex.withLock {
            val (versionName, versionCode) = try {
                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                Pair(pInfo.versionName ?: "1.0.0", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode.toInt() else @Suppress("DEPRECATION") pInfo.versionCode)
            } catch (_: Exception) {
                Pair("1.0.0", 1000)
            }

            val settingsMap = encodePrefs(context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE))
            val pcRemoteMap = encodePrefs(context.getSharedPreferences(PREFS_PC_REMOTE, Context.MODE_PRIVATE))
            val pcModulesMap = encodePrefs(context.getSharedPreferences(PREFS_PC_MODULES, Context.MODE_PRIVATE))

            val filesMap = mutableMapOf<String, String>()
            for (fname in DATA_JSON_FILES) {
                val f = File(context.filesDir, fname)
                if (f.exists() && f.length() > 0) {
                    try {
                        filesMap[fname] = f.readText()
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo leer archivo de datos $fname: ${e.message}")
                    }
                }
            }

            val totalItems = settingsMap.size + pcRemoteMap.size + pcModulesMap.size + filesMap.size

            val metadata = BackupMetadata(
                timestamp = System.currentTimeMillis(),
                versionName = versionName,
                versionCode = versionCode,
                source = source,
                itemsCount = totalItems,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            )

            FullAppStateBackup(
                metadata = metadata,
                settingsPrefs = settingsMap,
                pcRemotePrefs = pcRemoteMap,
                pcModulesPrefs = pcModulesMap,
                notesJson = filesMap["notes.json"],
                tasksJson = filesMap["tasks.json"],
                routinesJson = filesMap["routines.json"],
                expensesJson = filesMap["expenses.json"],
                smarthomeJson = filesMap["smarthome_devices.json"],
                vaultJson = filesMap["hendrix_vault_encrypted.json"],
                automatedRoutinesJson = filesMap["automated_routines.json"],
                macroDeckJson = filesMap["macro_deck_profiles.json"],
                knowledgeGraphJson = filesMap["knowledge_graph.json"]
            )
        }
    }

    override suspend fun saveBackupToPersistentStorage(backup: FullAppStateBackup): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val jsonString = json.encodeToString(backup)
                val targetDirs = getBackupDirectories()
                var anySaved = false

                for (dir in targetDirs) {
                    try {
                        val file = File(dir, BACKUP_FILE_NAME)
                        val tmp = File(dir, "$BACKUP_FILE_NAME.tmp")
                        tmp.writeText(jsonString)
                        if (tmp.renameTo(file) || run { file.delete(); tmp.renameTo(file) }) {
                            anySaved = true
                            Log.d(TAG, "Respaldo persistido en ${file.absolutePath}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error guardando respaldo en ${dir.absolutePath}: ${e.message}")
                    }
                }
                anySaved
            } catch (e: Exception) {
                Log.e(TAG, "Error serializando respaldo completo: ${e.message}", e)
                false
            }
        }
    }

    override suspend fun restoreFromBackup(backup: FullAppStateBackup): BackupOperationResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            var restoredCount = 0
            try {
                // 1. Restaurar SharedPreferences
                if (backup.settingsPrefs.isNotEmpty()) {
                    decodePrefs(context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE), backup.settingsPrefs)
                    restoredCount += backup.settingsPrefs.size
                }

                if (backup.pcRemotePrefs.isNotEmpty()) {
                    decodePrefs(context.getSharedPreferences(PREFS_PC_REMOTE, Context.MODE_PRIVATE), backup.pcRemotePrefs)
                    restoredCount += backup.pcRemotePrefs.size
                }

                if (backup.pcModulesPrefs.isNotEmpty()) {
                    decodePrefs(context.getSharedPreferences(PREFS_PC_MODULES, Context.MODE_PRIVATE), backup.pcModulesPrefs)
                    restoredCount += backup.pcModulesPrefs.size
                }

                // 2. Restaurar archivos JSON de repositorios
                val filePayloads = mapOf(
                    "notes.json" to backup.notesJson,
                    "tasks.json" to backup.tasksJson,
                    "routines.json" to backup.routinesJson,
                    "expenses.json" to backup.expensesJson,
                    "smarthome_devices.json" to backup.smarthomeJson,
                    "hendrix_vault_encrypted.json" to backup.vaultJson,
                    "automated_routines.json" to backup.automatedRoutinesJson,
                    "macro_deck_profiles.json" to backup.macroDeckJson,
                    "knowledge_graph.json" to backup.knowledgeGraphJson
                )

                for ((fname, content) in filePayloads) {
                    if (!content.isNullOrBlank()) {
                        val target = File(context.filesDir, fname)
                        val tmp = File(context.filesDir, "$fname.tmp")
                        tmp.writeText(content)
                        if (tmp.renameTo(target) || run { target.delete(); tmp.renameTo(target) }) {
                            restoredCount++
                        }
                    }
                }

                Log.i(TAG, "Restauración completada con éxito. $restoredCount elementos restaurados.")
                BackupOperationResult(
                    success = true,
                    message = "Restauración exitosa desde ${backup.metadata.source}",
                    restoredEntitiesCount = restoredCount
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error restaurando datos desde respaldo", e)
                BackupOperationResult(
                    success = false,
                    message = "Error en la restauración: ${e.message}",
                    restoredEntitiesCount = restoredCount
                )
            }
        }
    }

    override suspend fun autoRestoreIfCleanInstall(): BackupOperationResult? = withContext(Dispatchers.IO) {
        // Verificar si la instalación actual carece de datos esenciales
        val settingsPref = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        val hasSettings = settingsPref.contains("ai_provider") || settingsPref.contains("gemini_api_key")
        val notesFile = File(context.filesDir, "notes.json")
        val tasksFile = File(context.filesDir, "tasks.json")

        val isCleanInstall = !hasSettings && (!notesFile.exists() || notesFile.length() < 30) && (!tasksFile.exists() || tasksFile.length() < 30)
        if (!isCleanInstall) {
            return@withContext null // Ya contiene datos legítimos, no sobrescribir
        }

        // Buscar respaldo persistente previo en almacenamiento
        for (dir in getBackupDirectories()) {
            val file = File(dir, BACKUP_FILE_NAME)
            if (file.exists() && file.length() > 50) {
                try {
                    val raw = file.readText()
                    val backup = json.decodeFromString<FullAppStateBackup>(raw)
                    Log.i(TAG, "Auto-restauración detectada tras instalación limpia. Restaurando snapshot de ${backup.metadata.timestamp}...")
                    return@withContext restoreFromBackup(backup)
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo decodificar snapshot de ${file.absolutePath}: ${e.message}")
                }
            }
        }

        null
    }

    override suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val backup = createFullBackup("MANUAL_EXPORT")
        json.encodeToString(backup)
    }

    override suspend fun importBackupJson(jsonString: String): BackupOperationResult = withContext(Dispatchers.IO) {
        try {
            val backup = json.decodeFromString<FullAppStateBackup>(jsonString)
            val res = restoreFromBackup(backup)
            if (res.success) {
                saveBackupToPersistentStorage(backup)
            }
            res
        } catch (e: Exception) {
            BackupOperationResult(
                success = false,
                message = "Formato de archivo de respaldo inválido: ${e.message}"
            )
        }
    }

    // --- Utilitarios de Codificación Tipada para SharedPreferences ---

    private fun encodePrefs(prefs: SharedPreferences): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for ((k, v) in prefs.all) {
            val encoded = when (v) {
                is Boolean -> "b:$v"
                is Int -> "i:$v"
                is Long -> "l:$v"
                is Float -> "f:$v"
                is String -> "s:$v"
                else -> null
            }
            if (encoded != null) {
                map[k] = encoded
            }
        }
        return map
    }

    private fun decodePrefs(prefs: SharedPreferences, map: Map<String, String>) {
        val editor = prefs.edit()
        for ((k, encoded) in map) {
            if (encoded.length < 3 || encoded[1] != ':') continue
            val type = encoded[0]
            val valueStr = encoded.substring(2)
            when (type) {
                'b' -> editor.putBoolean(k, valueStr.toBoolean())
                'i' -> valueStr.toIntOrNull()?.let { editor.putInt(k, it) }
                'l' -> valueStr.toLongOrNull()?.let { editor.putLong(k, it) }
                'f' -> valueStr.toFloatOrNull()?.let { editor.putFloat(k, it) }
                's' -> editor.putString(k, valueStr)
            }
        }
        editor.apply()
    }
}
