package com.asistente.celular.skills.applauncher

import android.content.Intent
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
import java.util.Locale

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
            WordConstruct("abre", "abrir", "inicia", "iniciar", "ejecuta", "lanza", "corre"),
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
        val pm = context.androidContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        var matchedPackage: String? = null
        var matchedLabel: String? = null

        for (info in resolveInfos) {
            val label = normalize(info.loadLabel(pm).toString())
            if (label == cleanQuery || label.contains(cleanQuery) || cleanQuery.contains(label)) {
                matchedPackage = info.activityInfo.packageName
                matchedLabel = info.loadLabel(pm).toString()
                break
            }
        }

        if (matchedPackage == null) {
            val notFound = "No encontré la aplicación '$appQuery' en tu dispositivo."
            return SkillOutput(speech = notFound, displayText = notFound, success = false)
        }

        return try {
            val launchIntent = pm.getLaunchIntentForPackage(matchedPackage)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (launchIntent != null) {
                context.androidContext.startActivity(launchIntent)
                val msg = "Abriendo $matchedLabel."
                SkillOutput(speech = msg, displayText = msg, success = true)
            } else {
                val err = "No se pudo abrir $matchedLabel."
                SkillOutput(speech = err, displayText = err, success = false)
            }
        } catch (e: Exception) {
            val err = "Error al abrir la aplicación: ${e.message}"
            SkillOutput(speech = err, displayText = err, success = false)
        }
    }

    private fun normalize(text: String): String {
        return Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .trim()
    }
}
