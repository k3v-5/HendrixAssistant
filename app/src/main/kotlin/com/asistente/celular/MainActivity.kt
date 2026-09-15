package com.asistente.celular

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.asistente.celular.service.AssistantVoiceService
import com.asistente.celular.ui.AssistantScreen
import com.asistente.celular.ui.SettingsScreen
import com.asistente.celular.ui.notes.NotesScreen
import com.asistente.celular.ui.tasks.TasksScreen
import com.asistente.celular.ui.theme.AsistenteTheme
import com.asistente.celular.viewmodel.AssistantViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()
    private val currentScreenFlow = MutableStateFlow<Screen>(Screen.Assistant)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordGranted) {
            viewModel.startListening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)

        if (viewModel.uiState.value.isWakeWordActive) {
            val serviceIntent = Intent(this, AssistantVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        setContent {
            AsistenteTheme {
                val uiState by viewModel.uiState.collectAsState()
                val currentScreen by currentScreenFlow.collectAsState()
                val tasks by viewModel.tasksState.collectAsState()
                val notes by viewModel.notesState.collectAsState()

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentScreen == Screen.Assistant,
                                onClick = { currentScreenFlow.value = Screen.Assistant },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == Screen.Assistant) Icons.Filled.Forum else Icons.Outlined.Forum,
                                        contentDescription = "Asistente"
                                    )
                                },
                                label = { Text("Asistente") }
                            )

                            NavigationBarItem(
                                selected = currentScreen == Screen.Tasks,
                                onClick = { currentScreenFlow.value = Screen.Tasks },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == Screen.Tasks) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                        contentDescription = "Tareas"
                                    )
                                },
                                label = { Text("Tareas") }
                            )

                            NavigationBarItem(
                                selected = currentScreen == Screen.Notes,
                                onClick = { currentScreenFlow.value = Screen.Notes },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == Screen.Notes) Icons.Filled.EditNote else Icons.Outlined.EditNote,
                                        contentDescription = "Notas"
                                    )
                                },
                                label = { Text("Notas") }
                            )

                            NavigationBarItem(
                                selected = currentScreen == Screen.Settings,
                                onClick = { currentScreenFlow.value = Screen.Settings },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == Screen.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Ajustes"
                                    )
                                },
                                label = { Text("Ajustes") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            Screen.Assistant -> {
                                AssistantScreen(
                                    state = uiState,
                                    onStartListening = { ensurePermissionsAndListen() },
                                    onStopListening = { viewModel.stopListening() },
                                    onSendCommand = { text -> viewModel.processCommand(text) },
                                    onStopSpeech = { viewModel.stopSpeech() },
                                    onOpenSettings = { currentScreenFlow.value = Screen.Settings }
                                )
                            }
                            Screen.Tasks -> {
                                TasksScreen(
                                    tasks = tasks,
                                    onToggleTask = { taskId -> viewModel.toggleTask(taskId) },
                                    onAddTask = { task -> viewModel.addTask(task) },
                                    onDeleteTask = { taskId -> viewModel.deleteTask(taskId) },
                                    onClearCompleted = { viewModel.clearCompletedTasks() }
                                )
                            }
                            Screen.Notes -> {
                                NotesScreen(
                                    notes = notes,
                                    onAddNote = { note -> viewModel.addNote(note) },
                                    onUpdateNote = { note -> viewModel.updateNote(note) },
                                    onDeleteNote = { noteId -> viewModel.deleteNote(noteId) },
                                    onTogglePin = { noteId -> viewModel.toggleNotePin(noteId) }
                                )
                            }
                            Screen.Settings -> {
                                SettingsScreen(
                                    currentConfig = uiState.llmConfig,
                                    isWakeWordActive = uiState.isWakeWordActive,
                                    localModelManager = viewModel.localModelManager,
                                    onSaveConfig = { newConfig -> viewModel.updateLlmConfig(newConfig) },
                                    onToggleWakeWord = { enable -> toggleBackgroundWakeWord(enable) },
                                    onDownloadModel = { modelId -> viewModel.startModelDownload(modelId) },
                                    onCancelDownload = { modelId -> viewModel.cancelModelDownload(modelId) },
                                    onDeleteModel = { modelId -> viewModel.deleteLocalModel(modelId) },
                                    onSelectLocalModel = { modelId -> viewModel.selectLocalModel(modelId) },
                                    onBack = { currentScreenFlow.value = Screen.Assistant }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Mientras la app esté en primer plano en pantalla, pausar la escucha de fondo para ceder el micrófono
        com.asistente.celular.util.MicCoordinator.acquireMicLock("MainActivityForeground")
    }

    override fun onPause() {
        super.onPause()
        com.asistente.celular.util.MicCoordinator.releaseMicLock("MainActivityForeground")
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getStringExtra("OPEN_TAB") == "TASKS") {
            currentScreenFlow.value = Screen.Tasks
        }
        if (intent.getBooleanExtra(AssistantVoiceService.EXTRA_TRIGGERED_BY_WAKE_WORD, false)) {
            ensurePermissionsAndListen()
        }
    }

    private fun ensurePermissionsAndListen() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.startListening()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun toggleBackgroundWakeWord(enable: Boolean) {
        viewModel.toggleWakeWord(enable)
        val serviceIntent = Intent(this, AssistantVoiceService::class.java)

        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            stopService(serviceIntent)
        }
    }

    private sealed interface Screen {
        data object Assistant : Screen
        data object Tasks : Screen
        data object Notes : Screen
        data object Settings : Screen
    }
}
