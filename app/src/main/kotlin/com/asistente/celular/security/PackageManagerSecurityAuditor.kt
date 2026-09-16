package com.asistente.celular.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.asistente.celular.nlu.security.DeviceSecurityAuditor
import com.asistente.celular.nlu.security.RiskyAppInfo
import com.asistente.celular.nlu.security.SecurityAuditReport

/**
 * Auditor de permisos y seguridad local utilizando el PackageManager del sistema Android.
 */
class PackageManagerSecurityAuditor(
    private val context: Context
) : DeviceSecurityAuditor {

    private var lastReport: SecurityAuditReport? = null

    private val dangerousPermissionsList = listOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.READ_CALL_LOG",
        "android.permission.READ_CONTACTS"
    )

    override suspend fun performSecurityAudit(): SecurityAuditReport {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val riskyList = mutableListOf<RiskyAppInfo>()

        for (app in installedApps) {
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem) continue

            try {
                val packageInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
                val dangerousFound = requestedPermissions.filter { dangerousPermissionsList.contains(it) }

                if (dangerousFound.size >= 3) {
                    val appLabel = pm.getApplicationLabel(app).toString()
                    val riskLevel = if (dangerousFound.size >= 5) "ALTO" else "MEDIO"
                    riskyList.add(
                        RiskyAppInfo(
                            appName = appLabel,
                            packageName = app.packageName,
                            riskLevel = riskLevel,
                            dangerousPermissions = dangerousFound,
                            isSystemApp = false
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // Deducción de puntaje de seguridad (base 100)
        val penalty = (riskyList.count { it.riskLevel == "ALTO" } * 15) + (riskyList.count { it.riskLevel == "MEDIO" } * 5)
        val finalScore = (100 - penalty).coerceIn(40, 98)

        val report = SecurityAuditReport(
            overallScore = finalScore,
            riskyAppsCount = riskyList.size,
            riskyApps = riskyList.take(5)
        )
        lastReport = report
        return report
    }

    override fun getLastAuditReport(): SecurityAuditReport? = lastReport
}
