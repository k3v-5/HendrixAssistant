package com.asistente.celular.nlu.construct

import java.text.Normalizer
import java.util.Locale

/**
 * Contexto de evaluación para los combinadores gramaticales.
 */
class MatchContext(val rawInput: String) {
    val normalizedInput: String = normalize(rawInput)
    val tokens: List<String> = normalizedInput.split("\\s+".toRegex()).filter { it.isNotBlank() }
    
    var tokenIndex: Int = 0
    val capturedSlots: MutableMap<String, String> = mutableMapOf()

    val currentToken: String?
        get() = if (tokenIndex < tokens.size) tokens[tokenIndex] else null

    val isAtEnd: Boolean
        get() = tokenIndex >= tokens.size

    val remainingTokens: List<String>
        get() = if (tokenIndex < tokens.size) tokens.subList(tokenIndex, tokens.size) else emptyList()

    fun advance(): String? {
        return if (!isAtEnd) tokens[tokenIndex++] else null
    }

    fun snapshot(): State = State(tokenIndex, HashMap(capturedSlots))

    fun restore(state: State) {
        tokenIndex = state.tokenIndex
        capturedSlots.clear()
        capturedSlots.putAll(state.capturedSlots)
    }

    data class State(val tokenIndex: Int, val capturedSlots: Map<String, String>)

    companion object {
        fun normalize(text: String): String {
            val normalized = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            // Remover diacríticos pero mantener letras y números básicos
            return normalized.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
                .replace("[^a-z0-9\\s]".toRegex(), " ")
                .trim()
        }
    }
}

/**
 * Interfaz base para cualquier nodo de árbol gramatical (Construct).
 * Inspirado en la arquitectura construct de Dicio.
 */
sealed interface Construct {
    fun match(context: MatchContext): Boolean
}

/**
 * Coincide con una palabra específica o un conjunto de sinónimos directos.
 */
class WordConstruct(private val words: Set<String>) : Construct {
    constructor(vararg words: String) : this(words.map { MatchContext.normalize(it) }.toSet())

    override fun match(context: MatchContext): Boolean {
        val current = context.currentToken ?: return false
        if (words.contains(current)) {
            context.advance()
            return true
        }
        return false
    }
}

/**
 * Coincide con cualquiera de sus constructores hijos (Disyunción: a | b | c).
 */
class OrConstruct(private val alternatives: List<Construct>) : Construct {
    constructor(vararg alternatives: Construct) : this(alternatives.toList())

    override fun match(context: MatchContext): Boolean {
        val state = context.snapshot()
        for (alt in alternatives) {
            if (alt.match(context)) {
                return true
            }
            context.restore(state)
        }
        return false
    }
}

/**
 * Coincide con una secuencia obligatoria en orden (Concatenación: a + b + c).
 */
class SequenceConstruct(private val elements: List<Construct>) : Construct {
    constructor(vararg elements: Construct) : this(elements.toList())

    override fun match(context: MatchContext): Boolean {
        val state = context.snapshot()
        for (element in elements) {
            if (!element.match(context)) {
                context.restore(state)
                return false
            }
        }
        return true
    }
}

/**
 * Elemento opcional. Si coincide, avanza el contexto; si no coincide, continúa sin error.
 */
class OptionalConstruct(private val inner: Construct) : Construct {
    override fun match(context: MatchContext): Boolean {
        val state = context.snapshot()
        if (!inner.match(context)) {
            context.restore(state)
        }
        return true
    }
}

/**
 * Captura tokens para un slot específico (por ejemplo nombre de app, consulta de búsqueda o texto).
 * @param slotName Nombre del slot donde se almacenará el valor capturado.
 * @param stopAtConstruct Si se define, captura tokens hasta que este constructor coincida. Si es null, captura el resto.
 */
class CapturingConstruct(
    private val slotName: String,
    private val stopAtConstruct: Construct? = null
) : Construct {
    override fun match(context: MatchContext): Boolean {
        val captured = mutableListOf<String>()

        while (!context.isAtEnd) {
            if (stopAtConstruct != null) {
                val state = context.snapshot()
                if (stopAtConstruct.match(context)) {
                    context.restore(state)
                    break
                }
            }
            captured.add(context.advance()!!)
        }

        if (captured.isEmpty()) return false
        val value = captured.joinToString(" ")
        context.capturedSlots[slotName] = value
        return true
    }
}

/**
 * Constructor de expresión regular compilada para patrones complejos o de extracción directa.
 */
class RegexConstruct(
    private val regex: Regex,
    private val groupMapping: Map<String, String> = emptyMap()
) : Construct {
    constructor(pattern: String, groupMapping: Map<String, String> = emptyMap()) : this(
        pattern.toRegex(RegexOption.IGNORE_CASE),
        groupMapping
    )

    override fun match(context: MatchContext): Boolean {
        val matchResult = regex.find(context.normalizedInput) ?: return false
        for ((groupName, slotName) in groupMapping) {
            matchResult.groups[groupName]?.value?.let {
                context.capturedSlots[slotName] = it
            }
        }
        return true
    }
}
