package com.asistente.celular.nlu.context

import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Tipos de entidades de dominio rastreables en el diálogo multiturno.
 */
enum class EntityType {
    LIGHT,
    MEDIA_TRACK,
    VOLUME_STREAM,
    APP,
    TIMER,
    ALARM,
    NOTE,
    CONTACT,
    GENERIC
}

/**
 * Entidad capturada en un turno de conversación para resolución de correferencias.
 */
data class DialogEntity(
    val name: String,
    val type: EntityType,
    val attributes: Map<String, String> = emptyMap()
)

/**
 * Representa un turno individual en la conversación.
 */
data class DialogTurn(
    val userInput: String,
    val executedSkillId: String? = null,
    val primaryEntity: DialogEntity? = null,
    val assistantResponse: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Gestor reactivo de memoria de conversación multiturno a corto plazo.
 * Mantiene la ventana de contexto de los últimos turnos para resolver anáforas y referencias deícticas.
 */
class ConversationSessionTracker(
    private val maxHistorySize: Int = 6,
    private val sessionTimeoutMillis: Long = 180_000L // 3 minutos de expiración de contexto
) {
    private val history = ConcurrentLinkedDeque<DialogTurn>()

    fun recordTurn(
        userInput: String,
        executedSkillId: String? = null,
        primaryEntity: DialogEntity? = null,
        assistantResponse: String? = null
    ) {
        pruneExpired()
        history.addLast(
            DialogTurn(
                userInput = userInput,
                executedSkillId = executedSkillId,
                primaryEntity = primaryEntity,
                assistantResponse = assistantResponse
            )
        )
        while (history.size > maxHistorySize) {
            history.removeFirst()
        }
    }

    fun getLastEntity(): DialogEntity? {
        pruneExpired()
        return history.descendingIterator().asSequence()
            .mapNotNull { it.primaryEntity }
            .firstOrNull()
    }

    fun getLastEntityOfType(type: EntityType): DialogEntity? {
        pruneExpired()
        return history.descendingIterator().asSequence()
            .mapNotNull { it.primaryEntity }
            .firstOrNull { it.type == type }
    }

    fun getRecentTurns(): List<DialogTurn> {
        pruneExpired()
        return history.toList()
    }

    fun clear() {
        history.clear()
    }

    private fun pruneExpired() {
        val now = System.currentTimeMillis()
        while (history.isNotEmpty() && now - history.first.timestamp > sessionTimeoutMillis) {
            history.removeFirst()
        }
    }
}

/**
 * Resultado de la resolución de anáforas y correferencias de un comando.
 */
data class ResolvedInput(
    val resolvedText: String,
    val wasModified: Boolean,
    val resolvedEntity: DialogEntity? = null
)

/**
 * Resolutor de anáforas y elipsis en lenguaje natural en español.
 * Traduce expresiones como "apágala", "hazla más tenue", "súbele", "paúsala"
 * al comando explícito contextual según el estado del diálogo anterior.
 */
object CoreferenceResolver {

    fun resolve(input: String, tracker: ConversationSessionTracker): ResolvedInput {
        val lower = input.lowercase().trim()
        if (lower.isBlank()) return ResolvedInput(input, false)

        val lastEntity = tracker.getLastEntity() ?: return ResolvedInput(input, false)

        // 1. Anáforas de luz / foco inteligente
        if (lastEntity.type == EntityType.LIGHT) {
            when {
                // "apágala", "apágalo", "apagarla", "apagarlo"
                lower in listOf("apágala", "apagala", "apágalo", "apagalo", "apagarla", "apagarlo", "apágala por favor", "apaga eso") -> {
                    return ResolvedInput("apaga el ${lastEntity.name}", true, lastEntity)
                }
                // "enciéndela", "enciéndelo", "préndela", "préndelo"
                lower in listOf("enciéndela", "enciendela", "enciéndelo", "enciendelo", "préndela", "prendela", "préndelo", "prendelo") -> {
                    return ResolvedInput("enciende el ${lastEntity.name}", true, lastEntity)
                }
                // "hazla más tenue", "hazlo más tenue", "ponla más tenue", "más tenue", "más baja"
                lower.contains("tenue") || lower.contains("mas baja") || lower.contains("más baja") -> {
                    return ResolvedInput("baja el brillo del ${lastEntity.name}", true, lastEntity)
                }
                // "más brillante", "más clara", "aumenta su brillo"
                lower.contains("mas brillante") || lower.contains("más brillante") || lower.contains("mas clara") -> {
                    return ResolvedInput("sube el brillo del ${lastEntity.name}", true, lastEntity)
                }
                // "cámbiala a azul", "ponla verde", "hazla cálida"
                lower.startsWith("cambiala a ") || lower.startsWith("cámbiala a ") ||
                lower.startsWith("ponla ") || lower.startsWith("hazla ") -> {
                    val target = lower.substringAfter("a ").ifBlank { lower.substringAfter("ponla ").substringAfter("hazla ") }
                    return ResolvedInput("pon el ${lastEntity.name} $target", true, lastEntity)
                }
            }
        }

        // 2. Anáforas de multimedia / reproducción
        val mediaEntity = tracker.getLastEntityOfType(EntityType.MEDIA_TRACK) ?: lastEntity
        if (mediaEntity.type == EntityType.MEDIA_TRACK) {
            when {
                lower in listOf("paúsala", "pausala", "deténla", "detenla", "párala", "parala") -> {
                    return ResolvedInput("pausa la música", true, mediaEntity)
                }
                lower in listOf("reanúdala", "reanudala", "síguela", "siguela", "continúala", "continuala", "reprodúcela", "reproducela") -> {
                    return ResolvedInput("reanuda la música", true, mediaEntity)
                }
                lower in listOf("siguiente", "pásala", "pasala", "otra") -> {
                    return ResolvedInput("siguiente canción", true, mediaEntity)
                }
                lower in listOf("anterior", "regRésala", "regresala") -> {
                    return ResolvedInput("canción anterior", true, mediaEntity)
                }
            }
        }

        // 3. Anáforas de volumen
        if (lower in listOf("súbele", "subele", "auméntale", "aumentale", "más fuerte", "mas fuerte")) {
            return ResolvedInput("sube el volumen", true, lastEntity)
        }
        if (lower in listOf("bájale", "bajale", "disminúyele", "disminuyele", "más bajo", "mas bajo", "más despacio", "mas despacio")) {
            return ResolvedInput("baja el volumen", true, lastEntity)
        }

        return ResolvedInput(input, false)
    }
}
