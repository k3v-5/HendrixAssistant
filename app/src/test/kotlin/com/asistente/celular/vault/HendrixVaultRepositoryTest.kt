package com.asistente.celular.vault

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class HendrixVaultRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var mockContext: Context
    private val mockPrefsData = mutableMapOf<String, Any>()

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("hendrix_vault_test").toFile()
        mockPrefsData.clear()

        val fakePrefs = object : SharedPreferences {
            override fun getAll(): MutableMap<String, *> = mockPrefsData
            override fun getString(key: String?, defValue: String?): String? = mockPrefsData[key] as? String ?: defValue
            override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
            override fun getInt(key: String?, defValue: Int): Int = (mockPrefsData[key] as? Int) ?: defValue
            override fun getLong(key: String?, defValue: Long): Long = (mockPrefsData[key] as? Long) ?: defValue
            override fun getFloat(key: String?, defValue: Float): Float = (mockPrefsData[key] as? Float) ?: defValue
            override fun getBoolean(key: String?, defValue: Boolean): Boolean = (mockPrefsData[key] as? Boolean) ?: defValue
            override fun contains(key: String?): Boolean = mockPrefsData.containsKey(key)
            override fun edit(): SharedPreferences.Editor {
                return object : SharedPreferences.Editor {
                    override fun putString(key: String?, value: String?): SharedPreferences.Editor { if (key != null && value != null) mockPrefsData[key] = value; return this }
                    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
                    override fun putInt(key: String?, value: Int): SharedPreferences.Editor { if (key != null) mockPrefsData[key] = value; return this }
                    override fun putLong(key: String?, value: Long): SharedPreferences.Editor { if (key != null) mockPrefsData[key] = value; return this }
                    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { if (key != null) mockPrefsData[key] = value; return this }
                    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { if (key != null) mockPrefsData[key] = value; return this }
                    override fun remove(key: String?): SharedPreferences.Editor { mockPrefsData.remove(key); return this }
                    override fun clear(): SharedPreferences.Editor { mockPrefsData.clear(); return this }
                    override fun commit(): Boolean = true
                    override fun apply() {}
                }
            }
            override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
            override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        }

        mockContext = object : ContextWrapper(null) {
            override fun getFilesDir(): File = tempDir
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = fakePrefs
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `exportVaultJson exports existing files and preferences`() = runBlocking {
        // Create sample data files
        File(tempDir, "tasks.json").writeText("""[{"id":"t1","title":"Comprar leche"}]""")
        mockContext.getSharedPreferences("hendrix_prefs", 0).edit().putString("wake_word_sensitivity", "HIGH").apply()

        val repo = HendrixVaultRepository(mockContext)
        val jsonStr = repo.exportVaultJson()

        assertNotNull(jsonStr)
        val obj = JSONObject(jsonStr)
        assertEquals(1, obj.getInt("version"))
        assertTrue(obj.has("timestamp"))
        val files = obj.getJSONObject("files")
        assertTrue(files.has("tasks.json"))
        val settings = obj.getJSONObject("settings")
        assertEquals("HIGH", settings.getString("wake_word_sensitivity"))
    }

    @Test
    fun `restoreVaultJson restores files and preferences correctly`() = runBlocking {
        val repo = HendrixVaultRepository(mockContext)
        val sampleVault = """
            {
              "version": 1,
              "timestamp": 123456789,
              "files": {
                "notes.json": [{"id":"n1","title":"Nota 1","content":"Prueba vault"}]
              },
              "settings": {
                "tts_speech_rate": "1.15",
                "is_shake_to_wake": "true"
              }
            }
        """.trimIndent()

        val restored = repo.restoreVaultJson(sampleVault)
        assertTrue(restored)

        val restoredNoteFile = File(tempDir, "notes.json")
        assertTrue(restoredNoteFile.exists())
        assertTrue(restoredNoteFile.readText().contains("Prueba vault"))

        val prefs = mockContext.getSharedPreferences("hendrix_prefs", 0)
        assertEquals(true, prefs.getBoolean("is_shake_to_wake", false))
        assertEquals(1.15f, prefs.getFloat("tts_speech_rate", 1.0f), 0.01f)
    }

    @Test
    fun `exportToFile and importFromFile round trip succeeds`() = runBlocking {
        File(tempDir, "automated_routines.json").writeText("""[{"id":"r1","name":"Modo Estudio"}]""")

        val repo = HendrixVaultRepository(mockContext)
        val archiveFile = File(tempDir, "backup_vault.json")

        val exported = repo.exportToFile(archiveFile)
        assertTrue(exported)
        assertTrue(archiveFile.exists())

        // Modify local file
        File(tempDir, "automated_routines.json").writeText("[]")

        // Import back
        val imported = repo.importFromFile(archiveFile)
        assertTrue(imported)

        val finalContent = File(tempDir, "automated_routines.json").readText()
        assertTrue(finalContent.contains("Modo Estudio"))
    }
}
