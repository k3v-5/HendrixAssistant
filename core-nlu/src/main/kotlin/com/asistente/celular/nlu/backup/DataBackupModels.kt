package com.asistente.celular.nlu.backup

import kotlinx.serialization.Serializable

/**
 * Metadatos asociados a una instantánea de respaldo de la aplicación móvil.
 */
@Serializable
data class BackupMetadata(
    val timestamp: Long = System.currentTimeMillis(),
    val versionName: String = "1.0.0",
    val versionCode: Int = 1000,
    val source: String = "LOCAL_AUTO", // "LOCAL_AUTO", "DESKTOP_MIRROR", "MANUAL_EXPORT", "PRE_OTA_UPDATE"
    val itemsCount: Int = 0,
    val deviceModel: String = "Android Device"
)

/**
 * Instantánea integral del estado y todos los repositorios de la aplicación móvil.
 * Permite restaurar el asistente al 100% en caso de reinstalación o actualización limpia.
 */
@Serializable
data class FullAppStateBackup(
    val metadata: BackupMetadata = BackupMetadata(),
    val settingsPrefs: Map<String, String> = emptyMap(),
    val pcRemotePrefs: Map<String, String> = emptyMap(),
    val pcModulesPrefs: Map<String, String> = emptyMap(),
    val notesJson: String? = null,
    val tasksJson: String? = null,
    val routinesJson: String? = null,
    val expensesJson: String? = null,
    val smarthomeJson: String? = null,
    val vaultJson: String? = null,
    val automatedRoutinesJson: String? = null,
    val macroDeckJson: String? = null,
    val knowledgeGraphJson: String? = null
)

/**
 * Resultado de una operación de respaldo o restauración.
 */
data class BackupOperationResult(
    val success: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val restoredEntitiesCount: Int = 0
)

/**
 * Contrato formal para el coordinador de persistencia resiliente y auto-recuperación de datos.
 */
interface DataBackupCoordinator {
    suspend fun createFullBackup(source: String = "LOCAL_AUTO"): FullAppStateBackup
    suspend fun saveBackupToPersistentStorage(backup: FullAppStateBackup): Boolean
    suspend fun restoreFromBackup(backup: FullAppStateBackup): BackupOperationResult
    suspend fun autoRestoreIfCleanInstall(): BackupOperationResult?
    suspend fun exportBackupJson(): String
    suspend fun importBackupJson(jsonString: String): BackupOperationResult
}
