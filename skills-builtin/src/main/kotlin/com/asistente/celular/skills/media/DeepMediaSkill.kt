package com.asistente.celular.skills.media

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput

/**
 * Habilidad para búsqueda y reproducción profunda de música en Spotify, YouTube Music y YouTube.
 */
class DeepMediaSkill : Skill {

    override val info: SkillInfo = SkillInfo(
        id = "deep_media_skill",
        name = "Búsqueda y Reproducción Musical Profunda",
        description = "Busca y reproduce canciones, artistas y listas en Spotify, YouTube Music o YouTube."
    )

    override val specificity: Specificity = Specificity.HIGH

    private val platformPatterns: List<Construct> = listOf(
        // "pon {query} en spotify", "reproduce {query} en youtube"
        SequenceConstruct(
            WordConstruct("pon", "ponme", "reproduce", "reproducir", "reproduceme", "escuchar", "toca", "sonar", "haz"),
            OptionalConstruct(WordConstruct("la", "el", "sonar")),
            OptionalConstruct(WordConstruct("cancion", "musica", "disco", "pista")),
            OptionalConstruct(WordConstruct("de")),
            CapturingConstruct("query", stopAtConstruct = WordConstruct("en", "por")),
            WordConstruct("en", "por"),
            CapturingConstruct("platform")
        ),
        // "en spotify pon {query}"
        SequenceConstruct(
            WordConstruct("en", "por"),
            CapturingConstruct("platform", stopAtConstruct = WordConstruct("pon", "reproduce", "busca")),
            WordConstruct("pon", "ponme", "reproduce", "busca"),
            CapturingConstruct("query")
        ),
        // "reproduce la cancion {query}", "pon la cancion {query}"
        SequenceConstruct(
            WordConstruct("pon", "ponme", "reproduce", "reproducir", "reproduceme", "escuchar"),
            OptionalConstruct(WordConstruct("la", "el")),
            WordConstruct("cancion", "tema", "pista"),
            OptionalConstruct(WordConstruct("de")),
            CapturingConstruct("query")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val normalized = MatchContext.normalize(input)
        if (normalized.isBlank()) return SkillScore.NO_MATCH

        val hasVerb = normalized.contains("pon") || normalized.contains("reproduce") || normalized.contains("escuchar") || normalized.contains("toca")
        val hasPlatform = normalized.contains("spotify") || normalized.contains("youtube") || normalized.contains("music")

        if (!hasVerb && !hasPlatform) return SkillScore.NO_MATCH

        for (pattern in platformPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx) && ctx.isAtEnd) {
                return SkillScore(
                    confidence = 0.95f,
                    matchedWords = ctx.tokenIndex,
                    totalWords = ctx.tokens.size,
                    specificity = specificity,
                    capturedSlots = ctx.capturedSlots
                )
            }
        }

        if (hasPlatform) {
            val lower = normalized
            val platform = when {
                lower.contains("youtube music") -> "youtube music"
                lower.contains("spotify") -> "spotify"
                lower.contains("youtube") -> "youtube"
                else -> "spotify"
            }
            val query = extractQueryFromFreeText(normalized, platform)
            if (query.isNotBlank()) {
                return SkillScore(
                    confidence = 0.90f,
                    specificity = specificity,
                    capturedSlots = mapOf("query" to query, "platform" to platform)
                )
            }
        }

        return SkillScore.NO_MATCH
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val rawQuery = score.capturedSlots["query"]?.trim() ?: extractFallbackQuery(input)
        val platformSlot = score.capturedSlots["platform"]?.trim()?.lowercase() ?: "spotify"

        if (rawQuery.isBlank()) {
            val msg = "¿Qué canción o artista deseas reproducir?"
            return SkillOutput(speech = msg, displayText = msg, success = false)
        }

        val platform = when {
            platformSlot.contains("youtube music") -> Platform.YOUTUBE_MUSIC
            platformSlot.contains("youtube") -> Platform.YOUTUBE
            else -> Platform.SPOTIFY
        }

        val launched = launchMediaIntent(context, rawQuery, platform)
        val platformName = when (platform) {
            Platform.SPOTIFY -> "Spotify"
            Platform.YOUTUBE_MUSIC -> "YouTube Music"
            Platform.YOUTUBE -> "YouTube"
        }

        return if (launched) {
            val speech = "Reproduciendo $rawQuery en $platformName."
            val display = "🎵 **Reproduciendo en $platformName:**\n• \"$rawQuery\""
            SkillOutput(speech = speech, displayText = display, success = true)
        } else {
            val speech = "Buscando $rawQuery."
            SkillOutput(speech = speech, displayText = speech, success = true)
        }
    }

    private fun launchMediaIntent(context: SkillContext, query: String, platform: Platform): Boolean {
        val pm = context.androidContext.packageManager

        when (platform) {
            Platform.SPOTIFY -> {
                // 1. Intent directo de Spotify
                val spotifyIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("spotify:search:${Uri.encode(query)}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setPackage("com.spotify.music")
                }
                if (spotifyIntent.resolveActivity(pm) != null) {
                    context.androidContext.startActivity(spotifyIntent)
                    return true
                }

                // 2. Play from search
                val searchIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    putExtra(SearchManager.QUERY, query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setPackage("com.spotify.music")
                }
                if (searchIntent.resolveActivity(pm) != null) {
                    context.androidContext.startActivity(searchIntent)
                    return true
                }

                // 3. Fallback web
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://open.spotify.com/search/${Uri.encode(query)}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.androidContext.startActivity(webIntent)
                return true
            }

            Platform.YOUTUBE_MUSIC -> {
                val ytmIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://music.youtube.com/search?q=${Uri.encode(query)}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setPackage("com.google.android.apps.youtube.music")
                }
                if (ytmIntent.resolveActivity(pm) != null) {
                    context.androidContext.startActivity(ytmIntent)
                    return true
                }

                ytmIntent.setPackage(null)
                context.androidContext.startActivity(ytmIntent)
                return true
            }

            Platform.YOUTUBE -> {
                val ytIntent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (ytIntent.resolveActivity(pm) != null) {
                    context.androidContext.startActivity(ytIntent)
                    return true
                }

                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.androidContext.startActivity(webIntent)
                return true
            }
        }
    }

    private fun extractQueryFromFreeText(text: String, platform: String): String {
        var clean = text.replace(platform, "")
        val prefixes = listOf("pon", "ponme", "reproduce", "reproduceme", "escuchar", "toca", "cancion", "musica", "en", "de", "la", "el", "un", "una")
        val words = clean.split("\\s+".toRegex()).filter { it.isNotBlank() && it !in prefixes }
        return words.joinToString(" ")
    }

    private fun extractFallbackQuery(input: String): String {
        return input.replace("(?i)(pon|reproduce|en spotify|en youtube music|en youtube|cancion|musica)".toRegex(), "").trim()
    }

    private enum class Platform {
        SPOTIFY,
        YOUTUBE_MUSIC,
        YOUTUBE
    }
}
