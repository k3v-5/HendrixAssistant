package com.asistente.celular.skills.deepapp

import android.content.Context
import android.content.Intent
import android.util.Log
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.deepapp.DeepAppAction
import com.asistente.celular.nlu.deepapp.DeepAppRegistry
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput

/**
 * Habilidad para disparar Deep Links e Intents profundos hacia aplicaciones populares de Android
 * (Google Maps, Waze, Uber, MercadoLibre, Amazon, YouTube, Gmail).
 * Resuelve navegación, transporte, búsquedas comerciales y mensajería con fallback web seguro.
 */
class DeepAppSkill : Skill {

    override val info: SkillInfo = SkillInfo(
        id = "deep_app_skill",
        name = "Ecosistema de Apps Diarias (Deep Links)",
        description = "Abre rutas en Maps/Waze, pide Uber, busca en MercadoLibre/Amazon y lanza videos en YouTube por voz."
    )

    override val specificity: Specificity = Specificity.HIGH

    override fun score(context: SkillContext, input: String): SkillScore {
        val raw = input.trim()
        val norm = MatchContext.normalize(input)
        if (norm.isBlank()) return SkillScore.NO_MATCH

        // 1. Transporte: Uber
        if (norm.contains("uber")) {
            var dest = ""
            val lowerRaw = raw.lowercase()
            for (sep in listOf("uber al ", "uber a ", "uber hacia ", "uber para ", "uber ")) {
                if (lowerRaw.contains(sep)) {
                    dest = raw.substring(lowerRaw.indexOf(sep) + sep.length).trim()
                    break
                }
            }
            val cleanDest = if (dest.isNotBlank()) dest else "destino"
            return SkillScore(
                confidence = 0.98f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "uber", "destination" to cleanDest)
            )
        }

        // 2. Navegación GPS (Maps / Waze)
        val isWaze = norm.contains("waze")
        val isMaps = norm.contains("maps") || norm.contains("google maps")
        val isNavTrigger = norm.startsWith("navega ") || norm.startsWith("navegar ") ||
                norm.startsWith("como llegar ") || norm.startsWith("llevame ") ||
                norm.startsWith("ruta a ") || norm.startsWith("ruta hacia ") ||
                norm.startsWith("guiame ")

        if (isWaze || (isNavTrigger && !norm.contains("musica") && !norm.contains("cancion"))) {
            var dest = raw
            val lowerRaw = raw.lowercase()
            val navPrefixes = listOf(
                "navegar hacia ", "navega hacia ", "navegar al ", "navega al ", "navegar a ", "navega a ", "navegar ", "navega ",
                "cómo llegar al ", "como llegar al ", "cómo llegar a ", "como llegar a ", "cómo llegar ", "como llegar ",
                "llévame al ", "llevame al ", "llévame a ", "llevame a ", "llévame ", "llevame ",
                "ruta hacia ", "ruta a ",
                "guíame al ", "guiame al ", "guíame a ", "guiame a "
            )
            for (prefix in navPrefixes) {
                if (lowerRaw.startsWith(prefix)) {
                    dest = raw.substring(prefix.length).trim()
                    break
                }
            }
            val lowerDest = dest.lowercase()
            for (suffix in listOf(" en google maps", " en maps", " con google maps", " con maps", " en waze", " con waze")) {
                if (lowerDest.endsWith(suffix)) {
                    dest = dest.substring(0, dest.length - suffix.length).trim()
                    break
                }
            }

            val app = if (isWaze) "waze" else "google_maps"
            return SkillScore(
                confidence = 0.96f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "navigate", "destination" to dest, "app" to app)
            )
        }

        // 3. Compras: MercadoLibre y Amazon
        if (norm.contains("mercadolibre") || norm.contains("mercado libre")) {
            var query = raw
            val lower = raw.lowercase()
            val mlVariants = listOf(" en mercado libre", " en mercadolibre", " de mercado libre", " de mercadolibre")
            var stripped = false
            for (s in mlVariants) {
                if (lower.endsWith(s)) {
                    query = query.substring(0, query.length - s.length).trim()
                    stripped = true
                    break
                }
            }
            if (!stripped) {
                val mlPrefixes = listOf("mercado libre ", "mercadolibre ")
                for (p in mlPrefixes) {
                    if (lower.contains(p)) {
                        query = query.substring(lower.indexOf(p) + p.length).trim()
                        break
                    }
                }
            }
            val lowerQ = query.lowercase()
            for (prefix in listOf("busca en mercadolibre ", "busca en mercado libre ", "buscar en mercadolibre ", "buscar en mercado libre ", "busca ", "buscar ", "precio de ")) {
                if (lowerQ.startsWith(prefix)) {
                    query = query.substring(prefix.length).trim()
                    break
                }
            }
            return SkillScore(
                confidence = 0.97f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "shop", "platform" to "mercadolibre", "query" to query)
            )
        }

        if (norm.contains("amazon")) {
            var query = raw
            val lower = raw.lowercase()
            val amazonVariants = listOf(" en amazon", " de amazon")
            var stripped = false
            for (s in amazonVariants) {
                if (lower.endsWith(s)) {
                    query = query.substring(0, query.length - s.length).trim()
                    stripped = true
                    break
                }
            }
            if (!stripped) {
                val amazonPrefixes = listOf("amazon ")
                for (p in amazonPrefixes) {
                    if (lower.contains(p)) {
                        query = query.substring(lower.indexOf(p) + p.length).trim()
                        break
                    }
                }
            }
            val lowerQ = query.lowercase()
            for (prefix in listOf("busca en amazon ", "buscar en amazon ", "busca ", "buscar ", "precio de ")) {
                if (lowerQ.startsWith(prefix)) {
                    query = query.substring(prefix.length).trim()
                    break
                }
            }
            return SkillScore(
                confidence = 0.97f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "shop", "platform" to "amazon", "query" to query)
            )
        }

        // 4. Video: YouTube
        if (norm.contains("youtube")) {
            var query = raw
            val lower = raw.lowercase()
            if (lower.endsWith(" en youtube")) {
                query = query.substring(0, query.length - " en youtube".length).trim()
            } else if (lower.contains("youtube ")) {
                query = query.substring(lower.indexOf("youtube ") + "youtube ".length).trim()
            }
            val lowerQ = query.lowercase()
            for (prefix in listOf("busca en youtube ", "buscar en youtube ", "busca ", "buscar ", "reproduce ", "ver ")) {
                if (lowerQ.startsWith(prefix)) {
                    query = query.substring(prefix.length).trim()
                    break
                }
            }
            return SkillScore(
                confidence = 0.96f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "youtube", "query" to query)
            )
        }

        // 5. Correo: Gmail
        if (norm.startsWith("manda un correo") || norm.startsWith("envia un correo") || norm.startsWith("escribe un correo")) {
            val lowerRaw = raw.lowercase()
            val recipient = if (lowerRaw.contains(" a ")) {
                val afterA = raw.substring(lowerRaw.indexOf(" a ") + 3).trim()
                if (afterA.lowercase().contains(" con ")) {
                    afterA.substring(0, afterA.lowercase().indexOf(" con ")).trim()
                } else {
                    afterA
                }
            } else ""
            val subject = if (lowerRaw.contains("asunto ")) {
                raw.substring(lowerRaw.indexOf("asunto ") + 7).trim()
            } else ""
            return SkillScore(
                confidence = 0.95f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "email", "recipient" to recipient, "subject" to subject)
            )
        }

        return SkillScore.NO_MATCH
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val action = score.capturedSlots["action"] ?: "navigate"

        return when (action) {
            "navigate" -> {
                val dest = score.capturedSlots["destination"] ?: "destino"
                val appChoice = score.capturedSlots["app"] ?: "google_maps"
                val appDef = DeepAppRegistry.getApp(appChoice) ?: DeepAppRegistry.getApp("google_maps")!!

                val launched = launchAppIntent(context.androidContext, appDef, mapOf("destination" to dest))
                val speech = if (launched) "Calculando ruta a $dest en ${appDef.name}." else "No se pudo abrir ${appDef.name}."
                val display = "🗺️ **${appDef.name}:** Ruta hacia $dest"
                SkillOutput(speech = speech, displayText = display, success = launched)
            }
            "uber" -> {
                val dest = score.capturedSlots["destination"] ?: ""
                val appDef = DeepAppRegistry.getApp("uber")!!
                val launched = launchAppIntent(context.androidContext, appDef, mapOf("destination" to dest))
                val speech = if (dest.isNotBlank()) "Pidiendo Uber hacia $dest." else "Abriendo Uber."
                val display = "🚗 **Uber:** Destino $dest"
                SkillOutput(speech = speech, displayText = display, success = launched)
            }
            "shop" -> {
                val platform = score.capturedSlots["platform"] ?: "mercadolibre"
                val query = score.capturedSlots["query"] ?: ""
                val appDef = DeepAppRegistry.getApp(platform) ?: DeepAppRegistry.getApp("mercadolibre")!!
                val launched = launchAppIntent(context.androidContext, appDef, mapOf("query" to query))
                val speech = "Buscando $query en ${appDef.name}."
                val display = "🛍️ **${appDef.name}:** Búsqueda de '$query'"
                SkillOutput(speech = speech, displayText = display, success = launched)
            }
            "youtube" -> {
                val query = score.capturedSlots["query"] ?: ""
                val appDef = DeepAppRegistry.getApp("youtube")!!
                val launched = launchAppIntent(context.androidContext, appDef, mapOf("query" to query))
                val speech = "Buscando $query en YouTube."
                val display = "▶️ **YouTube:** '$query'"
                SkillOutput(speech = speech, displayText = display, success = launched)
            }
            "email" -> {
                val recipient = score.capturedSlots["recipient"] ?: ""
                val subject = score.capturedSlots["subject"] ?: ""
                val appDef = DeepAppRegistry.getApp("gmail")!!
                val launched = launchAppIntent(context.androidContext, appDef, mapOf("recipient" to recipient, "subject" to subject))
                val speech = "Redactando correo en Gmail."
                val display = "✉️ **Gmail:** Para $recipient"
                SkillOutput(speech = speech, displayText = display, success = launched)
            }
            else -> SkillOutput(speech = "Acción de app no reconocida.", success = false)
        }
    }

    private fun launchAppIntent(
        context: Context,
        appDef: com.asistente.celular.nlu.deepapp.DeepAppDefinition,
        params: Map<String, String>
    ): Boolean {
        return try {
            val nativeUri = appDef.buildNativeUri(params)
            val intent = Intent(Intent.ACTION_VIEW, nativeUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo abrir app nativa '${appDef.name}', probando fallback web: ${e.message}")
            try {
                val webUri = appDef.buildWebFallbackUri(params)
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                true
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Error en fallback web para '${appDef.name}': ${fallbackEx.message}")
                false
            }
        }
    }

    companion object {
        private const val TAG = "DeepAppSkill"
    }
}
