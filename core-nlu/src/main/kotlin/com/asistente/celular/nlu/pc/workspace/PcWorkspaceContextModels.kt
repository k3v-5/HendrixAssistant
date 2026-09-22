package com.asistente.celular.nlu.pc.workspace

/**
 * Estado de un repositorio Git activo o detectado en la estación de trabajo PC.
 */
data class PcGitRepositoryStatus(
    val repoName: String,
    val branch: String,
    val path: String,
    val hasUncommittedChanges: Boolean = false,
    val uncommittedFilesCount: Int = 0,
    val lastCommitMessage: String = "",
    val lastCommitAuthor: String = "",
    val lastCommitHash: String = "",
    val lastCommitTimestamp: String = ""
)

/**
 * Proceso creativo o de desarrollo activo en la PC (IDE, editor, DAW, motor 3D).
 */
data class PcRunningCreativeProcess(
    val pid: Int,
    val name: String,
    val title: String,
    val category: String,
    val cpuPercent: Double = 0.0,
    val memoryMb: Double = 0.0
)

/**
 * Alerta estructurada generada por el Watchdog de Terminal de Hendrix Desktop ante fallas en compilaciones o scripts.
 */
data class PcTerminalErrorAlert(
    val errorId: String,
    val source: String,
    val command: String,
    val errorMessage: String,
    val failedFile: String? = null,
    val failedLine: Int? = null,
    val aiDiagnosisPrompt: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Contexto integral y memoria de trabajo cruzada de la estación de trabajo PC.
 */
data class PcWorkspaceContext(
    val foregroundProcess: String = "",
    val foregroundTitle: String = "",
    val activeGitRepos: List<PcGitRepositoryStatus> = emptyList(),
    val runningCreativeProcesses: List<PcRunningCreativeProcess> = emptyList(),
    val recentTerminalErrors: List<PcTerminalErrorAlert> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
