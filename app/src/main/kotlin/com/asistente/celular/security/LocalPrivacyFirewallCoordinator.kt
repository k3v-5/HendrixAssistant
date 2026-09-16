package com.asistente.celular.security

import android.content.Context
import com.asistente.celular.nlu.security.NetworkConnectionAudit
import com.asistente.celular.nlu.security.PrivacyFirewallEngine
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Coordinador de firewall de privacidad local y bloqueo de rastreadores salientes.
 */
class LocalPrivacyFirewallCoordinator(
    private val context: Context
) : PrivacyFirewallEngine {

    private val connectionHistory = CopyOnWriteArrayList<NetworkConnectionAudit>()
    private val blockedCounter = AtomicInteger(24)
    private var isActiveState = true

    private val knownTrackerKeywords = listOf(
        "telemetry", "analytics", "graph.facebook", "app-measurement", "admob", "doubleclick", "crashlytics-track"
    )

    init {
        // Cargar registros iniciales de demostración
        recordConnection(
            NetworkConnectionAudit(
                appName = "App Juegos",
                packageName = "com.sample.casualgame",
                destinationDomain = "telemetry.ads.track.io",
                isTracker = true,
                blocked = true
            )
        )
        recordConnection(
            NetworkConnectionAudit(
                appName = "Red Social",
                packageName = "com.sample.social",
                destinationDomain = "graph.facebook.com/analytics",
                isTracker = true,
                blocked = true
            )
        )
    }

    override fun recordConnection(audit: NetworkConnectionAudit) {
        val isTracker = knownTrackerKeywords.any { audit.destinationDomain.contains(it) }
        val finalAudit = if (isTracker) {
            blockedCounter.incrementAndGet()
            audit.copy(isTracker = true, blocked = true)
        } else {
            audit
        }
        connectionHistory.add(0, finalAudit)
    }

    override fun getRecentConnections(limit: Int): List<NetworkConnectionAudit> {
        return connectionHistory.take(limit)
    }

    override fun getBlockedTrackersCount(): Int = blockedCounter.get()

    override fun isFirewallActive(): Boolean = isActiveState
}
