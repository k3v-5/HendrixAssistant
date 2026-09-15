package com.asistente.celular.skills.applauncher

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import java.text.Normalizer

/**
 * Habilidad offline para buscar e iniciar aplicaciones instaladas en Android.
 */
class AppLauncherSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "app_launcher_skill",
        name = "Abrir Aplicación",
        description = "Abre aplicaciones instaladas en el dispositivo."
    ),
    specificity = Specificity.HIGH
) {
    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct(
                "abre", "abreme", "abrir",
                "inicia", "iniciame", "iniciar",
                "ejecuta", "ejecutame", "ejecutar",
                "lanza", "lanzame", "lanzar",
                "corre",
                "pon", "ponme"
            ),
            OptionalConstruct(WordConstruct("la", "el")),
            OptionalConstruct(WordConstruct("aplicacion", "app")),
            CapturingConstruct("appName")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val appQuery = score.capturedSlots["appName"] ?: return SkillOutput(
            speech = "¿Qué aplicación deseas abrir?",
            displayText = "¿Qué aplicación deseas abrir?",
            success = false
        )

        val cleanQuery = normalize(appQuery)
        val packageManager = context.androidContext.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        // Buscar coincidencia por nombre de etiqueta de la app
        var matchedApp: ApplicationInfo? = null
        for (app in installedApps) {
            // Filtrar apps del sistema que no tienen launcher
            if (packageManager.getLaunchIntentForPackage(app.packageName) == null) continue

            val appLabel = normalize(packageManager.getApplicationLabel(app).toString())
            if (appLabel == cleanQuery || appLabel.contains(cleanQuery) || cleanQuery.contains(appLabel)) {
                matchedApp = app
                break
            }
        }

        if (matchedApp != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(matchedApp.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.androidContext.startActivity(launchIntent)
                val label = packageManager.getApplicationLabel(matchedApp).toString()
                val msg = "Abriendo $label."
                return SkillOutput(speech = msg, displayText = msg, success = true)
            }
        }

        val notFoundMsg = "No encontré la aplicación '$appQuery' instalada en tu teléfono."
        return SkillOutput(speech = notFoundMsg, displayText = notFoundMsg, success = false)
    }

    private fun normalize(text: String): String {
        val normalized = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        return normalized.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "").trim()
    }
}
