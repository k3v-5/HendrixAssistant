package com.asistente.celular.nlu.deepapp

import android.net.Uri
import java.net.URLEncoder

/**
 * Categorías de aplicaciones de terceros soportadas en el ecosistema profundo.
 */
enum class DeepAppCategory(val displayName: String) {
    NAVIGATION("Navegación GPS"),
    TRANSPORT("Transporte y Movilidad"),
    SHOPPING("Comercio y Compras"),
    MEDIA("Video y Streaming"),
    COMMUNICATION("Correo y Mensajería")
}

/**
 * Representa una acción profunda resuelta para ejecutar en una app externa.
 */
sealed interface DeepAppAction {
    data class Navigate(
        val destination: String,
        val preferredApp: String? = null // "maps", "waze", null (cualquiera)
    ) : DeepAppAction

    data class RideHailing(
        val destination: String,
        val serviceName: String = "uber"
    ) : DeepAppAction

    data class SearchProduct(
        val query: String,
        val platform: String = "mercadolibre" // "mercadolibre", "amazon"
    ) : DeepAppAction

    data class PlayVideo(
        val query: String
    ) : DeepAppAction

    data class SendEmail(
        val recipientEmail: String? = null,
        val subject: String? = null,
        val body: String? = null
    ) : DeepAppAction
}

/**
 * Definición técnica de una aplicación de terceros para generación de Deep Links e Intents con fallback web.
 */
data class DeepAppDefinition(
    val id: String,
    val name: String,
    val category: DeepAppCategory,
    val packageName: String,
    val uriBuilder: (Map<String, String>) -> String,
    val webFallbackBuilder: (Map<String, String>) -> String
) {
    fun buildNativeUri(params: Map<String, String>): Uri = Uri.parse(uriBuilder(params))
    fun buildWebFallbackUri(params: Map<String, String>): Uri = Uri.parse(webFallbackBuilder(params))
}

/**
 * Catálogo central y extensible de aplicaciones de terceros (OCP).
 * Permite registrar nuevas apps sin alterar el núcleo de resolución.
 */
object DeepAppRegistry {

    private val apps = mutableMapOf<String, DeepAppDefinition>()

    init {
        registerDefaultApps()
    }

    fun registerApp(definition: DeepAppDefinition) {
        apps[definition.id] = definition
    }

    fun getApp(id: String): DeepAppDefinition? = apps[id]

    fun getAllApps(): List<DeepAppDefinition> = apps.values.toList()

    private fun registerDefaultApps() {
        // 1. Google Maps
        registerApp(
            DeepAppDefinition(
                id = "google_maps",
                name = "Google Maps",
                category = DeepAppCategory.NAVIGATION,
                packageName = "com.google.android.apps.maps",
                uriBuilder = { params ->
                    val query = encodeParam(params["destination"] ?: "")
                    "google.navigation:q=$query"
                },
                webFallbackBuilder = { params ->
                    val query = encodeParam(params["destination"] ?: "")
                    "https://www.google.com/maps/dir/?api=1&destination=$query"
                }
            )
        )

        // 2. Waze
        registerApp(
            DeepAppDefinition(
                id = "waze",
                name = "Waze",
                category = DeepAppCategory.NAVIGATION,
                packageName = "com.waze",
                uriBuilder = { params ->
                    val query = encodeParam(params["destination"] ?: "")
                    "waze://?q=$query&navigate=yes"
                },
                webFallbackBuilder = { params ->
                    val query = encodeParam(params["destination"] ?: "")
                    "https://waze.com/ul?q=$query&navigate=yes"
                }
            )
        )

        // 3. Uber
        registerApp(
            DeepAppDefinition(
                id = "uber",
                name = "Uber",
                category = DeepAppCategory.TRANSPORT,
                packageName = "com.ubercab",
                uriBuilder = { params ->
                    val dest = encodeParam(params["destination"] ?: "")
                    "uber://?action=setPickup&pickup=my_location&dropoff[formatted_address]=$dest"
                },
                webFallbackBuilder = { params ->
                    val dest = encodeParam(params["destination"] ?: "")
                    "https://m.uber.com/ul/?action=setPickup&pickup=my_location&dropoff[formatted_address]=$dest"
                }
            )
        )

        // 4. MercadoLibre
        registerApp(
            DeepAppDefinition(
                id = "mercadolibre",
                name = "MercadoLibre",
                category = DeepAppCategory.SHOPPING,
                packageName = "com.mercadolibre",
                uriBuilder = { params ->
                    val q = encodeParam(params["query"] ?: "")
                    "meli://search?q=$q"
                },
                webFallbackBuilder = { params ->
                    val q = encodeParam(params["query"] ?: "")
                    "https://listado.mercadolibre.com.mx/$q"
                }
            )
        )

        // 5. Amazon
        registerApp(
            DeepAppDefinition(
                id = "amazon",
                name = "Amazon",
                category = DeepAppCategory.SHOPPING,
                packageName = "com.amazon.mShop.android.shopping",
                uriBuilder = { params ->
                    val q = encodeParam(params["query"] ?: "")
                    "com.amazon.mobile.shopping.web://amazon.com/s?k=$q"
                },
                webFallbackBuilder = { params ->
                    val q = encodeParam(params["query"] ?: "")
                    "https://www.amazon.com/s?k=$q"
                }
            )
        )

        // 6. YouTube
        registerApp(
            DeepAppDefinition(
                id = "youtube",
                name = "YouTube",
                category = DeepAppCategory.MEDIA,
                packageName = "com.google.android.youtube",
                uriBuilder = { params ->
                    val q = encodeParam(params["query"] ?: "")
                    "vnd.youtube://results?search_query=$q"
                },
                webFallbackBuilder = { params ->
                    val q = encodeParam(params["query"] ?: "")
                    "https://www.youtube.com/results?search_query=$q"
                }
            )
        )

        // 7. Gmail / Correo
        registerApp(
            DeepAppDefinition(
                id = "gmail",
                name = "Gmail",
                category = DeepAppCategory.COMMUNICATION,
                packageName = "com.google.android.gm",
                uriBuilder = { params ->
                    val email = params["recipient"] ?: ""
                    val subject = encodeParam(params["subject"] ?: "")
                    val body = encodeParam(params["body"] ?: "")
                    "mailto:$email?subject=$subject&body=$body"
                },
                webFallbackBuilder = { params ->
                    val email = params["recipient"] ?: ""
                    val subject = encodeParam(params["subject"] ?: "")
                    val body = encodeParam(params["body"] ?: "")
                    "https://mail.google.com/mail/?view=cm&fs=1&to=$email&su=$subject&body=$body"
                }
            )
        )
    }

    private fun encodeParam(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (_: Exception) {
            value
        }
    }
}
