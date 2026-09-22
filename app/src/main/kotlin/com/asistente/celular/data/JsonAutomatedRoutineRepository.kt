package com.asistente.celular.data

import android.content.Context
import android.util.Log
import com.asistente.celular.nlu.automation.AutomatedRoutine
import com.asistente.celular.nlu.automation.AutomatedRoutineAction
import com.asistente.celular.nlu.automation.AutomatedRoutineEngine
import com.asistente.celular.nlu.automation.AutomatedRoutineTrigger
import com.asistente.celular.nlu.automation.GeofenceTransition
import com.asistente.celular.nlu.automation.WifiTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

import com.asistente.celular.nlu.automation.AutomatedRoutineRepository

/**
 * Repositorio persistente en JSON para las rutinas automatizadas "Zero-Touch".
 */
class JsonAutomatedRoutineRepository(
    private val context: Context,
    private val scope: CoroutineScope
) : AutomatedRoutineRepository {
    companion object {
        private const val TAG = "JsonAutomatedRoutineRepo"
        private const val FILE_NAME = "automated_routines.json"
    }

    private val file = File(context.filesDir, FILE_NAME)
    override val engine = AutomatedRoutineEngine()

    private val _routines = MutableStateFlow<List<AutomatedRoutine>>(emptyList())
    override val routines: StateFlow<List<AutomatedRoutine>> = _routines.asStateFlow()

    init {
        scope.launch {
            loadRoutines()
        }
    }

    private suspend fun loadRoutines() = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            val defaults = createDefaultRoutines()
            defaults.forEach { engine.registerRoutine(it) }
            saveToFile(defaults)
            _routines.value = defaults
            return@withContext
        }

        try {
            val content = file.readText()
            val arr = JSONArray(content)
            val list = mutableListOf<AutomatedRoutine>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(parseRoutine(obj))
            }
            list.forEach { engine.registerRoutine(it) }
            _routines.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando rutinas automatizadas: ${e.message}", e)
            val defaults = createDefaultRoutines()
            defaults.forEach { engine.registerRoutine(it) }
            _routines.value = defaults
        }
    }

    override suspend fun saveRoutine(routine: AutomatedRoutine) = withContext(Dispatchers.IO) {
        engine.registerRoutine(routine)
        val all = engine.getRoutines()
        _routines.value = all
        saveToFile(all)
    }

    override suspend fun deleteRoutine(id: String) = withContext(Dispatchers.IO) {
        engine.deleteRoutine(id)
        val all = engine.getRoutines()
        _routines.value = all
        saveToFile(all)
    }

    override suspend fun toggleRoutine(id: String): Boolean = withContext(Dispatchers.IO) {
        val newState = engine.toggleRoutine(id)
        val all = engine.getRoutines()
        _routines.value = all
        saveToFile(all)
        newState
    }

    private fun saveToFile(routines: List<AutomatedRoutine>) {
        try {
            val arr = JSONArray()
            for (r in routines) {
                arr.put(serializeRoutine(r))
            }
            val tempFile = File(context.filesDir, "${FILE_NAME}.tmp")
            tempFile.writeText(arr.toString(2))
            if (!tempFile.renameTo(file)) {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando rutinas automatizadas de forma atómica: ${e.message}", e)
        }
    }

    private fun createDefaultRoutines(): List<AutomatedRoutine> {
        return listOf(
            AutomatedRoutine(
                id = "routine_studio_arrival",
                name = "Llegada al Estudio",
                description = "Enciende la PC por Wake-on-LAN, desbloquea la sesión y saluda por voz.",
                iconEmoji = "🏡",
                isEnabled = true,
                triggers = listOf(
                    AutomatedRoutineTrigger.WifiSsidTrigger("Home_WiFi_5G", WifiTransition.CONNECTED),
                    AutomatedRoutineTrigger.GeofenceTrigger("Casa", GeofenceTransition.ENTER),
                    AutomatedRoutineTrigger.VoicePhraseTrigger(listOf("llegada al estudio", "estudio listo", "buenos dias estudio"))
                ),
                actions = listOf(
                    AutomatedRoutineAction.PcQuickCommandAction("wake_on_lan"),
                    AutomatedRoutineAction.DelayAction(3000),
                    AutomatedRoutineAction.PcQuickCommandAction("unlock"),
                    AutomatedRoutineAction.SpeakTtsAction("Bienvenido al estudio. Tu estación de trabajo está encendida y lista.")
                )
            ),
            AutomatedRoutine(
                id = "routine_leave_home",
                name = "Salida de Casa",
                description = "Suspende la PC automáticamente y activa el modo seguro al desconectarse del Wi-Fi.",
                iconEmoji = "🚗",
                isEnabled = true,
                triggers = listOf(
                    AutomatedRoutineTrigger.WifiSsidTrigger("Home_WiFi_5G", WifiTransition.DISCONNECTED),
                    AutomatedRoutineTrigger.GeofenceTrigger("Casa", GeofenceTransition.EXIT),
                    AutomatedRoutineTrigger.VoicePhraseTrigger(listOf("me voy de casa", "salida de casa", "cerrar estudio"))
                ),
                actions = listOf(
                    AutomatedRoutineAction.PcQuickCommandAction("sleep"),
                    AutomatedRoutineAction.SpeakTtsAction("Modo seguro activado. Tu computadora ha sido suspendida.")
                )
            ),
            AutomatedRoutine(
                id = "routine_render_night_guard",
                name = "Vigilancia de Render Nocturno",
                description = "Suspende la PC tras finalizar un render pesado y avisa al celular.",
                iconEmoji = "🌙",
                isEnabled = true,
                triggers = listOf(
                    AutomatedRoutineTrigger.PcEventTrigger("RENDER_COMPLETED"),
                    AutomatedRoutineTrigger.VoicePhraseTrigger(listOf("vigila render nocturno", "render noche"))
                ),
                actions = listOf(
                    AutomatedRoutineAction.SpeakTtsAction("El render ha finalizado exitosamente. Suspendiendo el equipo."),
                    AutomatedRoutineAction.DelayAction(2000),
                    AutomatedRoutineAction.PcQuickCommandAction("sleep")
                )
            )
        )
    }

    private fun serializeRoutine(r: AutomatedRoutine): JSONObject {
        return JSONObject().apply {
            put("id", r.id)
            put("name", r.name)
            put("description", r.description)
            put("iconEmoji", r.iconEmoji)
            put("isEnabled", r.isEnabled)
            put("lastTriggeredEpoch", r.lastTriggeredEpoch)

            val tArr = JSONArray()
            r.triggers.forEach { t ->
                val to = JSONObject()
                when (t) {
                    is AutomatedRoutineTrigger.WifiSsidTrigger -> {
                        to.put("type", "WIFI")
                        to.put("ssid", t.ssid)
                        to.put("transition", t.transition.name)
                    }
                    is AutomatedRoutineTrigger.GeofenceTrigger -> {
                        to.put("type", "GEOFENCE")
                        to.put("zone", t.zoneName)
                        to.put("transition", t.transition.name)
                    }
                    is AutomatedRoutineTrigger.PcEventTrigger -> {
                        to.put("type", "PC_EVENT")
                        to.put("event", t.eventType)
                    }
                    is AutomatedRoutineTrigger.ChargingTrigger -> {
                        to.put("type", "CHARGING")
                        to.put("isCharging", t.isCharging)
                    }
                    is AutomatedRoutineTrigger.ScheduleTrigger -> {
                        to.put("type", "SCHEDULE")
                        to.put("time", t.timeString)
                    }
                    is AutomatedRoutineTrigger.VoicePhraseTrigger -> {
                        to.put("type", "VOICE")
                        val pArr = JSONArray()
                        t.phrases.forEach { pArr.put(it) }
                        to.put("phrases", pArr)
                    }
                }
                tArr.put(to)
            }
            put("triggers", tArr)

            val aArr = JSONArray()
            r.actions.forEach { a ->
                val ao = JSONObject()
                when (a) {
                    is AutomatedRoutineAction.AssistantCommandAction -> {
                        ao.put("type", "COMMAND")
                        ao.put("command", a.commandText)
                    }
                    is AutomatedRoutineAction.PcQuickCommandAction -> {
                        ao.put("type", "PC_COMMAND")
                        ao.put("command", a.command)
                    }
                    is AutomatedRoutineAction.PcStudioSceneAction -> {
                        ao.put("type", "PC_SCENE")
                        ao.put("sceneId", a.sceneId)
                    }
                    is AutomatedRoutineAction.PcPluginAction -> {
                        ao.put("type", "PC_PLUGIN")
                        ao.put("pluginId", a.pluginId)
                        ao.put("actionId", a.actionId)
                    }
                    is AutomatedRoutineAction.SpeakTtsAction -> {
                        ao.put("type", "SPEAK")
                        ao.put("text", a.text)
                    }
                    is AutomatedRoutineAction.DelayAction -> {
                        ao.put("type", "DELAY")
                        ao.put("millis", a.delayMillis)
                    }
                    is AutomatedRoutineAction.EnterDeskStandbyAction -> {
                        ao.put("type", "DESK_STANDBY")
                        ao.put("keepScreenOn", a.isKeepScreenOn)
                    }
                }
                aArr.put(ao)
            }
            put("actions", aArr)
        }
    }

    private fun parseRoutine(obj: JSONObject): AutomatedRoutine {
        val id = obj.optString("id")
        val name = obj.optString("name", "Rutina")
        val desc = obj.optString("description", "")
        val icon = obj.optString("iconEmoji", "⚡")
        val isEnabled = obj.optBoolean("isEnabled", true)
        val lastTriggered = obj.optLong("lastTriggeredEpoch", 0L)

        val triggers = mutableListOf<AutomatedRoutineTrigger>()
        val tArr = obj.optJSONArray("triggers") ?: JSONArray()
        for (i in 0 until tArr.length()) {
            val to = tArr.getJSONObject(i)
            when (to.optString("type")) {
                "WIFI" -> triggers.add(
                    AutomatedRoutineTrigger.WifiSsidTrigger(
                        ssid = to.optString("ssid", ""),
                        transition = try { WifiTransition.valueOf(to.optString("transition")) } catch (e: Exception) { WifiTransition.CONNECTED }
                    )
                )
                "GEOFENCE" -> triggers.add(
                    AutomatedRoutineTrigger.GeofenceTrigger(
                        zoneName = to.optString("zone", ""),
                        transition = try { GeofenceTransition.valueOf(to.optString("transition")) } catch (e: Exception) { GeofenceTransition.ENTER }
                    )
                )
                "PC_EVENT" -> triggers.add(
                    AutomatedRoutineTrigger.PcEventTrigger(to.optString("event", ""))
                )
                "CHARGING" -> triggers.add(
                    AutomatedRoutineTrigger.ChargingTrigger(to.optBoolean("isCharging", true))
                )
                "SCHEDULE" -> triggers.add(
                    AutomatedRoutineTrigger.ScheduleTrigger(to.optString("time", "08:00"))
                )
                "VOICE" -> {
                    val pArr = to.optJSONArray("phrases") ?: JSONArray()
                    val plist = mutableListOf<String>()
                    for (j in 0 until pArr.length()) plist.add(pArr.getString(j))
                    triggers.add(AutomatedRoutineTrigger.VoicePhraseTrigger(plist))
                }
            }
        }

        val actions = mutableListOf<AutomatedRoutineAction>()
        val aArr = obj.optJSONArray("actions") ?: JSONArray()
        for (i in 0 until aArr.length()) {
            val ao = aArr.getJSONObject(i)
            when (ao.optString("type")) {
                "COMMAND" -> actions.add(AutomatedRoutineAction.AssistantCommandAction(ao.optString("command", "")))
                "PC_COMMAND" -> actions.add(AutomatedRoutineAction.PcQuickCommandAction(ao.optString("command", "")))
                "PC_SCENE" -> actions.add(AutomatedRoutineAction.PcStudioSceneAction(ao.optString("sceneId", "")))
                "PC_PLUGIN" -> actions.add(AutomatedRoutineAction.PcPluginAction(ao.optString("pluginId", ""), ao.optString("actionId", "")))
                "SPEAK" -> actions.add(AutomatedRoutineAction.SpeakTtsAction(ao.optString("text", "")))
                "DELAY" -> actions.add(AutomatedRoutineAction.DelayAction(ao.optLong("millis", 1000L)))
                "DESK_STANDBY" -> actions.add(AutomatedRoutineAction.EnterDeskStandbyAction(ao.optBoolean("keepScreenOn", true)))
            }
        }

        return AutomatedRoutine(
            id = id,
            name = name,
            description = desc,
            iconEmoji = icon,
            isEnabled = isEnabled,
            triggers = triggers,
            actions = actions,
            lastTriggeredEpoch = lastTriggered
        )
    }
}
