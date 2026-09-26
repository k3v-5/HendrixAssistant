package com.asistente.celular.pc.module

import android.content.SharedPreferences
import com.asistente.celular.nlu.pc.module.PcModuleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para [PcModuleManager].
 * Valida la persistencia, activación/desactivación granular y masiva de módulos de PC.
 */
class PcModuleManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var manager: PcModuleManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        manager = PcModuleManager(fakePrefs)
    }

    @Test
    fun testInitialModulesLoaded() {
        val all = manager.modules.value
        assertEquals(17, all.size)
        assertTrue(manager.isModuleEnabled(PcModuleId.UNREAL_ENGINE))
        assertTrue(manager.isModuleEnabled(PcModuleId.BLENDER))
        assertTrue(manager.isModuleEnabled(PcModuleId.ADOBE_CREATIVE))
    }

    @Test
    fun testToggleModulePersists() {
        val initial = manager.isModuleEnabled(PcModuleId.UNREAL_ENGINE)
        manager.toggleModule(PcModuleId.UNREAL_ENGINE)
        assertEquals(!initial, manager.isModuleEnabled(PcModuleId.UNREAL_ENGINE))

        // Nueva instancia con el mismo almacenamiento debe preservar la selección del usuario
        val manager2 = PcModuleManager(fakePrefs)
        assertEquals(!initial, manager2.isModuleEnabled(PcModuleId.UNREAL_ENGINE))
    }

    @Test
    fun testDisableAndEnableAll() {
        manager.disableAllModules()
        assertEquals(0, manager.enabledModules.value.size)
        assertFalse(manager.isModuleEnabled(PcModuleId.UNREAL_ENGINE))
        assertFalse(manager.isModuleEnabled(PcModuleId.BLENDER))

        manager.enableAllModules()
        assertEquals(17, manager.enabledModules.value.size)
        assertTrue(manager.isModuleEnabled(PcModuleId.UNREAL_ENGINE))
        assertTrue(manager.isModuleEnabled(PcModuleId.BLENDER))
    }

    @Test
    fun testResetToDefaults() {
        manager.setModuleEnabled(PcModuleId.UNREAL_ENGINE, false)
        assertFalse(manager.isModuleEnabled(PcModuleId.UNREAL_ENGINE))

        manager.resetToDefaults()
        assertTrue(manager.isModuleEnabled(PcModuleId.UNREAL_ENGINE))
    }
}

/**
 * Implementación en memoria liviana de [SharedPreferences] para pruebas unitarias sin dependencias de Android framework.
 */
class FakeSharedPreferences : SharedPreferences {
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

    override fun edit(): SharedPreferences.Editor = FakeEditor(data)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val backingData: MutableMap<String, Any?>) : SharedPreferences.Editor {
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
