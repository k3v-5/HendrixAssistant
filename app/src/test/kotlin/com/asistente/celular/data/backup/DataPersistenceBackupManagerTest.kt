package com.asistente.celular.data.backup

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DataPersistenceBackupManagerTest {

    private lateinit var tempDir: File
    private lateinit var testContext: FakeBackupTestContext
    private lateinit var backupManager: DataPersistenceBackupManager

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "hendrix_backup_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        testContext = FakeBackupTestContext(tempDir)
        backupManager = DataPersistenceBackupManager(testContext)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testCreateFullBackup_capturesPreferencesAndFiles() = runBlocking {
        val settingsPref = testContext.getSharedPreferences("asistente_celular_settings", Context.MODE_PRIVATE)
        settingsPref.edit()
            .putString("ai_provider", "GEMINI")
            .putString("gemini_api_key", "AIzaSySecretTestKey")
            .putBoolean("wake_word_active", true)
            .putInt("llm_max_tokens", 800)
            .commit()

        val pcPref = testContext.getSharedPreferences("pc_remote_prefs", Context.MODE_PRIVATE)
        pcPref.edit()
            .putString("pc_host", "192.168.100.159")
            .putString("pc_tailscale_ip", "100.85.20.14")
            .putInt("pc_port", 8899)
            .commit()

        val sampleNotesJson = """[{"id":"note_1","title":"Nota Test","content":"Contenido protegido"}]"""
        File(testContext.filesDir, "notes.json").writeText(sampleNotesJson)

        val backup = backupManager.createFullBackup("TEST_RUN")

        assertNotNull(backup)
        assertEquals("TEST_RUN", backup.metadata.source)
        assertEquals("s:GEMINI", backup.settingsPrefs["ai_provider"])
        assertEquals("s:AIzaSySecretTestKey", backup.settingsPrefs["gemini_api_key"])
        assertEquals("b:true", backup.settingsPrefs["wake_word_active"])
        assertEquals("i:800", backup.settingsPrefs["llm_max_tokens"])
        assertEquals("s:100.85.20.14", backup.pcRemotePrefs["pc_tailscale_ip"])
        assertEquals(sampleNotesJson, backup.notesJson)
    }

    @Test
    fun testSaveAndRestoreFromBackup_restoresExactState() = runBlocking {
        val settingsPref = testContext.getSharedPreferences("asistente_celular_settings", Context.MODE_PRIVATE)
        settingsPref.edit()
            .putString("ai_provider", "CLAUDE")
            .putFloat("tts_pitch", 1.25f)
            .putLong("test_timestamp", 9876543210L)
            .commit()

        File(testContext.filesDir, "tasks.json").writeText("""[{"id":"t1","title":"Hacer compras"}]""")

        val backup = backupManager.createFullBackup("BEFORE_WIPE")
        val saved = backupManager.saveBackupToPersistentStorage(backup)
        assertTrue(saved)

        // Simular reinstalación borrando preferencias y archivos
        settingsPref.edit().clear().commit()
        File(testContext.filesDir, "tasks.json").delete()

        assertEquals("", settingsPref.getString("ai_provider", ""))

        // Restaurar desde el backup
        val result = backupManager.restoreFromBackup(backup)
        assertTrue(result.success)
        assertTrue(result.restoredEntitiesCount > 0)

        // Validar restauración exacta
        assertEquals("CLAUDE", settingsPref.getString("ai_provider", ""))
        assertEquals(1.25f, settingsPref.getFloat("tts_pitch", 1.0f), 0.001f)
        assertEquals(9876543210L, settingsPref.getLong("test_timestamp", 0L))
        assertTrue(File(testContext.filesDir, "tasks.json").exists())
        assertEquals("""[{"id":"t1","title":"Hacer compras"}]""", File(testContext.filesDir, "tasks.json").readText())
    }

    @Test
    fun testExportAndImportJson_roundTripSuccess() = runBlocking {
        val settingsPref = testContext.getSharedPreferences("asistente_celular_settings", Context.MODE_PRIVATE)
        settingsPref.edit().putString("ai_provider", "LOCAL_SLM").commit()

        val jsonString = backupManager.exportBackupJson()
        assertTrue(jsonString.contains("LOCAL_SLM"))

        settingsPref.edit().clear().commit()

        val importRes = backupManager.importBackupJson(jsonString)
        assertTrue(importRes.success)
        assertEquals("LOCAL_SLM", settingsPref.getString("ai_provider", ""))
    }

    @Test
    fun testAutoRestoreIfCleanInstall_triggersOnlyWhenEmpty() = runBlocking {
        // 1. Guardar un snapshot persistente
        val settingsPref = testContext.getSharedPreferences("asistente_celular_settings", Context.MODE_PRIVATE)
        settingsPref.edit().putString("gemini_api_key", "PERSISTENT_KEY").commit()
        val backup = backupManager.createFullBackup("INITIAL_SETUP")
        backupManager.saveBackupToPersistentStorage(backup)

        // 2. Simular app con datos: no debe sobrescribir
        settingsPref.edit().putString("gemini_api_key", "CURRENT_KEY").commit()
        val noOpResult = backupManager.autoRestoreIfCleanInstall()
        assertEquals(null, noOpResult)
        assertEquals("CURRENT_KEY", settingsPref.getString("gemini_api_key", ""))

        // 3. Simular instalación limpia
        settingsPref.edit().clear().commit()
        val autoRestored = backupManager.autoRestoreIfCleanInstall()
        assertNotNull(autoRestored)
        assertTrue(autoRestored!!.success)
        assertEquals("PERSISTENT_KEY", settingsPref.getString("gemini_api_key", ""))
    }
}

class FakeBackupTestContext(baseDir: File) : ContextWrapper(null) {
    private val prefs = mutableMapOf<String, TestFakePrefs>()
    private val filesDirectory = File(baseDir, "files").apply { mkdirs() }
    private val extFilesDirectory = File(baseDir, "ext_files").apply { mkdirs() }

    override fun getFilesDir(): File = filesDirectory

    override fun getExternalFilesDir(type: String?): File = extFilesDirectory

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        return prefs.getOrPut(name ?: "default") { TestFakePrefs() }
    }

    override fun getPackageName(): String = "com.asistente.celular"
}

class TestFakePrefs : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST")
        (data[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        data[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        data[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        data[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = TestFakeEditor(data)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class TestFakeEditor(private val backingData: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) removed.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) {
                backingData.clear()
            }
            removed.forEach { backingData.remove(it) }
            backingData.putAll(temp)
        }
    }
}
