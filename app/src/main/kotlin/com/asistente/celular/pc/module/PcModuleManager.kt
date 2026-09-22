package com.asistente.celular.pc.module

import android.content.Context
import android.content.SharedPreferences
import com.asistente.celular.nlu.pc.module.PcModuleDefinition
import com.asistente.celular.nlu.pc.module.PcModuleId
import com.asistente.celular.nlu.pc.module.PcModuleRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor de persistencia y estado para los módulos y plugins de control de PC.
 * Permite al usuario activar o desactivar qué suites de software desea tener
 * visibles en su Centro de Control y Quick Command Deck.
 */
class PcModuleManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _modules = MutableStateFlow<List<PcModuleDefinition>>(emptyList())
    val modules: StateFlow<List<PcModuleDefinition>> = _modules.asStateFlow()

    private val _enabledModules = MutableStateFlow<List<PcModuleDefinition>>(emptyList())
    val enabledModules: StateFlow<List<PcModuleDefinition>> = _enabledModules.asStateFlow()

    init {
        loadModules()
    }

    private fun loadModules() {
        val all = PcModuleRegistry.ALL_MODULES.map { def ->
            val key = "module_enabled_${def.id.name}"
            val isEnabled = prefs.getBoolean(key, def.isEnabledByDefault)
            def.copy(isEnabledByDefault = isEnabled)
        }
        _modules.value = all
        _enabledModules.value = all.filter { it.isEnabledByDefault }
    }

    fun isModuleEnabled(id: PcModuleId): Boolean {
        val def = PcModuleRegistry.findById(id) ?: return false
        return prefs.getBoolean("module_enabled_${id.name}", def.isEnabledByDefault)
    }

    fun setModuleEnabled(id: PcModuleId, enabled: Boolean) {
        prefs.edit().putBoolean("module_enabled_${id.name}", enabled).apply()
        loadModules()
    }

    fun toggleModule(id: PcModuleId) {
        val current = isModuleEnabled(id)
        setModuleEnabled(id, !current)
    }

    companion object {
        private const val PREFS_NAME = "hendrix_pc_modules_prefs"
    }
}
