package com.asistente.celular.data

import android.content.Context
import android.util.Log
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckAction
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckControl
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfile
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfileRegistry
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Implementación de repositorio persistente en JSON para los perfiles del Macro Deck (Custom Deck Studio).
 * Permite guardar, clonar, editar y restaurar perfiles creados por el usuario junto con los perfiles del sistema.
 */
class JsonMacroDeckProfileRepository(
    private val context: Context,
    private val scope: CoroutineScope
) : MacroDeckProfileRepository {

    companion object {
        private const val TAG = "JsonMacroDeckRepo"
        private const val FILE_NAME = "custom_macro_deck_profiles.json"
    }

    private val file = File(context.filesDir, FILE_NAME)
    private val _profiles = MutableStateFlow<List<MacroDeckProfile>>(MacroDeckProfileRegistry.ALL_PROFILES)
    override val profiles: StateFlow<List<MacroDeckProfile>> = _profiles.asStateFlow()

    val initJob: Job = scope.launch {
        loadProfiles()
    }

    private suspend fun loadProfiles() = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            val defaults = MacroDeckProfileRegistry.ALL_PROFILES
            saveToFile(defaults)
            _profiles.value = defaults
            return@withContext
        }

        try {
            val content = file.readText()
            val arr = JSONArray(content)
            val list = mutableListOf<MacroDeckProfile>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(parseProfile(obj))
            }
            if (list.isEmpty()) {
                val defaults = MacroDeckProfileRegistry.ALL_PROFILES
                saveToFile(defaults)
                _profiles.value = defaults
            } else {
                _profiles.value = list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando perfiles Macro Deck: ${e.message}", e)
            val defaults = MacroDeckProfileRegistry.ALL_PROFILES
            _profiles.value = defaults
        }
    }

    override suspend fun saveProfile(profile: MacroDeckProfile) = withContext(Dispatchers.IO) {
        initJob.join()
        val current = _profiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(0, profile)
        }
        _profiles.value = current
        saveToFile(current)
    }

    override suspend fun deleteProfile(id: String) = withContext(Dispatchers.IO) {
        initJob.join()
        val current = _profiles.value.toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            // Asegurar que al menos el perfil por defecto persista
            if (current.isEmpty()) {
                current.addAll(MacroDeckProfileRegistry.ALL_PROFILES)
            }
            _profiles.value = current
            saveToFile(current)
        }
    }

    override suspend fun resetToDefaults() = withContext(Dispatchers.IO) {
        initJob.join()
        val defaults = MacroDeckProfileRegistry.ALL_PROFILES
        _profiles.value = defaults
        saveToFile(defaults)
    }

    override fun getProfileById(id: String): MacroDeckProfile? {
        return _profiles.value.firstOrNull { it.id == id }
    }

    override fun findProfileForProcess(processName: String?): MacroDeckProfile {
        val list = _profiles.value
        val fallback = list.firstOrNull { it.id == MacroDeckProfileRegistry.DEFAULT_DESKTOP_PROFILE.id }
            ?: list.firstOrNull()
            ?: MacroDeckProfileRegistry.DEFAULT_DESKTOP_PROFILE

        if (processName.isNullOrBlank()) return fallback

        for (profile in list) {
            if (profile.id != MacroDeckProfileRegistry.DEFAULT_DESKTOP_PROFILE.id) {
                try {
                    if (processName.matches(Regex(profile.targetProcessRegex))) {
                        return profile
                    }
                } catch (ignored: Exception) {}
            }
        }
        return fallback
    }

    private fun saveToFile(profiles: List<MacroDeckProfile>) {
        try {
            val arr = JSONArray()
            for (p in profiles) {
                arr.put(profileToJson(p))
            }
            file.writeText(arr.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando perfiles en disco: ${e.message}", e)
        }
    }

    private fun profileToJson(p: MacroDeckProfile): JSONObject {
        val obj = JSONObject()
        obj.put("id", p.id)
        obj.put("name", p.name)
        obj.put("iconEmoji", p.iconEmoji)
        obj.put("targetProcessRegex", p.targetProcessRegex)
        obj.put("headerSubtitle", p.headerSubtitle)
        obj.put("themeAccentColorHex", p.themeAccentColorHex)
        val controlsArr = JSONArray()
        p.controls.forEach { controlsArr.put(controlToJson(it)) }
        obj.put("controls", controlsArr)
        return obj
    }

    private fun parseProfile(obj: JSONObject): MacroDeckProfile {
        val id = obj.optString("id", UUID.randomUUID().toString())
        val name = obj.optString("name", "Perfil")
        val iconEmoji = obj.optString("iconEmoji", "🎮")
        val targetProcessRegex = obj.optString("targetProcessRegex", ".*")
        val headerSubtitle = obj.optString("headerSubtitle", "")
        val themeAccentColorHex = obj.optLong("themeAccentColorHex", 0xFF6366F1)
        val controlsList = mutableListOf<MacroDeckControl>()
        val arr = obj.optJSONArray("controls")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                controlsList.add(parseControl(arr.getJSONObject(i)))
            }
        }
        return MacroDeckProfile(
            id = id,
            name = name,
            iconEmoji = iconEmoji,
            targetProcessRegex = targetProcessRegex,
            headerSubtitle = headerSubtitle,
            themeAccentColorHex = themeAccentColorHex,
            controls = controlsList
        )
    }

    private fun controlToJson(control: MacroDeckControl): JSONObject {
        val obj = JSONObject()
        obj.put("id", control.id)
        obj.put("label", control.label)
        obj.put("iconEmoji", control.iconEmoji)
        obj.put("colorHex", control.colorHex)

        when (control) {
            is MacroDeckControl.MacroButton -> {
                obj.put("type", "button")
                obj.put("action", actionToJson(control.action))
                obj.put("subtitle", control.subtitle ?: "")
                obj.put("isFullWidth", control.isFullWidth)
            }
            is MacroDeckControl.MacroFader -> {
                obj.put("type", "fader")
                obj.put("targetParameter", control.targetParameter)
                obj.put("minValue", control.minValue.toDouble())
                obj.put("maxValue", control.maxValue.toDouble())
                obj.put("initialValue", control.initialValue.toDouble())
            }
            is MacroDeckControl.MacroJogWheel -> {
                obj.put("type", "jogwheel")
                obj.put("onStepForwardAction", actionToJson(control.onStepForwardAction))
                obj.put("onStepBackwardAction", actionToJson(control.onStepBackwardAction))
            }
        }
        return obj
    }

    private fun parseControl(obj: JSONObject): MacroDeckControl {
        val id = obj.optString("id", UUID.randomUUID().toString())
        val label = obj.optString("label", "Control")
        val iconEmoji = obj.optString("iconEmoji", "🔘")
        val colorHex = obj.optLong("colorHex", 0xFF6366F1)
        val type = obj.optString("type", "button")

        return when (type) {
            "fader" -> MacroDeckControl.MacroFader(
                id = id,
                label = label,
                iconEmoji = iconEmoji,
                colorHex = colorHex,
                targetParameter = obj.optString("targetParameter", "volume"),
                minValue = obj.optDouble("minValue", 0.0).toFloat(),
                maxValue = obj.optDouble("maxValue", 1.0).toFloat(),
                initialValue = obj.optDouble("initialValue", 0.8).toFloat()
            )
            "jogwheel" -> MacroDeckControl.MacroJogWheel(
                id = id,
                label = label,
                iconEmoji = iconEmoji,
                colorHex = colorHex,
                onStepForwardAction = parseAction(obj.optJSONObject("onStepForwardAction") ?: JSONObject()),
                onStepBackwardAction = parseAction(obj.optJSONObject("onStepBackwardAction") ?: JSONObject())
            )
            else -> MacroDeckControl.MacroButton(
                id = id,
                label = label,
                iconEmoji = iconEmoji,
                colorHex = colorHex,
                action = parseAction(obj.optJSONObject("action") ?: JSONObject()),
                subtitle = obj.optString("subtitle").takeIf { it.isNotBlank() },
                isFullWidth = obj.optBoolean("isFullWidth", false)
            )
        }
    }

    private fun actionToJson(action: MacroDeckAction): JSONObject {
        val obj = JSONObject()
        when (action) {
            is MacroDeckAction.ShortcutAction -> {
                obj.put("type", "shortcut")
                obj.put("keySequence", action.keySequence)
                obj.put("description", action.description)
            }
            is MacroDeckAction.QuickCommandAction -> {
                obj.put("type", "quick_command")
                obj.put("command", action.command)
            }
            is MacroDeckAction.StudioSceneAction -> {
                obj.put("type", "studio_scene")
                obj.put("sceneId", action.sceneId)
            }
            is MacroDeckAction.PluginAction -> {
                obj.put("type", "plugin")
                obj.put("pluginId", action.pluginId)
                obj.put("actionId", action.actionId)
                val paramsObj = JSONObject()
                action.params.forEach { (k, v) -> paramsObj.put(k, v) }
                obj.put("params", paramsObj)
            }
            is MacroDeckAction.RoutineAction -> {
                obj.put("type", "routine")
                obj.put("routineId", action.routineId)
            }
        }
        return obj
    }

    private fun parseAction(obj: JSONObject): MacroDeckAction {
        return when (obj.optString("type")) {
            "shortcut" -> MacroDeckAction.ShortcutAction(
                keySequence = obj.optString("keySequence", "Enter"),
                description = obj.optString("description", "")
            )
            "quick_command" -> MacroDeckAction.QuickCommandAction(
                command = obj.optString("command", "noop")
            )
            "studio_scene" -> MacroDeckAction.StudioSceneAction(
                sceneId = obj.optString("sceneId", "main")
            )
            "plugin" -> {
                val params = mutableMapOf<String, Any>()
                val pObj = obj.optJSONObject("params")
                if (pObj != null) {
                    val keys = pObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        params[k] = pObj.get(k)
                    }
                }
                MacroDeckAction.PluginAction(
                    pluginId = obj.optString("pluginId", "core"),
                    actionId = obj.optString("actionId", "trigger"),
                    params = params
                )
            }
            "routine" -> MacroDeckAction.RoutineAction(
                routineId = obj.optString("routineId", "")
            )
            else -> MacroDeckAction.QuickCommandAction(
                command = obj.optString("command", "noop")
            )
        }
    }
}
