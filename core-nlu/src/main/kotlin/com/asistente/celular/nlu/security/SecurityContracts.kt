package com.asistente.celular.nlu.security

data class NetworkConnectionAudit(
    val appName: String,
    val packageName: String,
    val destinationDomain: String,
    val isTracker: Boolean = false,
    val blocked: Boolean = false,
    val timestampEpoch: Long = System.currentTimeMillis()
)

/**
 * Contrato para el firewall de privacidad y detección de rastreadores salientes en red local.
 */
interface PrivacyFirewallEngine {
    fun recordConnection(audit: NetworkConnectionAudit)
    fun getRecentConnections(limit: Int = 20): List<NetworkConnectionAudit>
    fun getBlockedTrackersCount(): Int
    fun isFirewallActive(): Boolean
}

data class RiskyAppInfo(
    val appName: String,
    val packageName: String,
    val riskLevel: String, // "ALTO", "MEDIO", "SEGURO"
    val dangerousPermissions: List<String>,
    val isSystemApp: Boolean = false
)

data class SecurityAuditReport(
    val overallScore: Int, // 0 - 100
    val riskyAppsCount: Int,
    val riskyApps: List<RiskyAppInfo>,
    val scanTimestampEpoch: Long = System.currentTimeMillis()
)

/**
 * Contrato para el auditor de permisos y seguridad del dispositivo.
 */
interface DeviceSecurityAuditor {
    suspend fun performSecurityAudit(): SecurityAuditReport
    fun getLastAuditReport(): SecurityAuditReport?
}
