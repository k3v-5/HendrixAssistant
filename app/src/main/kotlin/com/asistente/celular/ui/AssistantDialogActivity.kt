package com.asistente.celular.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.ai.client.LlmClient
import com.asistente.celular.ai.harness.HarnessResult
import com.asistente.celular.ai.harness.ModelHarness
import com.asistente.celular.ai.router.AiRouterSkill
import com.asistente.celular.data.SettingsRepository
import com.asistente.celular.nlu.evaluator.SkillEvaluator
import com.asistente.celular.nlu.evaluator.SkillRanker
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.skills.alarm.AlarmSkill
import com.asistente.celular.skills.applauncher.AppLauncherSkill
import com.asistente.celular.skills.flashlight.FlashlightSkill
import com.asistente.celular.skills.media.MediaControlSkill
import com.asistente.celular.skills.time.CurrentTimeSkill
import com.asistente.celular.skills.timer.TimerSkill
import com.asistente.celular.ui.theme.AsistenteTheme
import com.asistente.celular.util.HapticFeedbackManager
import com.asistente.celular.voice.stt.AndroidSpeechRecognizerEngine
import com.asistente.celular.voice.tts.AndroidNativeTtsEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Actividad flotante tipo Bottom Sheet transparente.
 * Se despliega sobre cualquier aplicación abierta al activarse en segundo plano.
 */
class AssistantDialogActivity : ComponentActivity() {

    private lateinit var hapticManager: HapticFeedbackManager
    private lateinit var ttsEngine: AndroidNativeTtsEngine
    private lateinit var sttEngine: AndroidSpeechRecognizerEngine
    private lateinit var evaluator: SkillEvaluator
    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        hapticManager = HapticFeedbackManager(this)
        ttsEngine = AndroidNativeTtsEngine(this)
        sttEngine = AndroidSpeechRecognizerEngine(this)
        settingsRepo = SettingsRepository(this)

        // Pausar el motor de segundo plano para ceder el micrófono
        com.asistente.celular.util.MicCoordinator.acquireMicLock("AssistantDialogActivity")

        val activeLlmConfig = settingsRepo.loadLlmConfig()
        val llmClient = LlmClient { activeLlmConfig }
        val modelHarness = ModelHarness()
        val aiFallback = AiRouterSkill(llmClient, modelHarness) { activeLlmConfig }

        val localSkills = listOf(
            FlashlightSkill(),
            TimerSkill(),
            AlarmSkill(),
            AppLauncherSkill(),
            CurrentTimeSkill(),
            MediaControlSkill()
        )

        val ranker = SkillRanker(localSkills, aiFallback)

        val skillContext = object : SkillContext {
            override val androidContext: Context get() = this@AssistantDialogActivity
            override val isConnectedToInternet: Boolean get() = checkInternet()
            override val previousOutput: SkillOutput? = null
        }

        evaluator = SkillEvaluator(
            ranker = ranker,
            skillContext = skillContext,
            onSpeak = { text ->
                ttsEngine.speak(text)
            }
        )

        setContent {
            AsistenteTheme {
                FloatingAssistantBottomSheet(
                    onDismiss = { finish() },
                    onStartListening = { onStart, onPartial, onFinal, onError ->
                        hapticManager.vibrateStartListening()
                        sttEngine.startListening(onPartial, onFinal, onError)
                    },
                    onStopListening = { sttEngine.stopListening() },
                    onProcessCommand = { command, onDone ->
                        lifecycleScope.launch {
                            val output = evaluator.processInput(command)
                            if (output.success) {
                                hapticManager.vibrateSuccess()
                            } else {
                                hapticManager.vibrateError()
                            }
                            onDone(output)
                            // Esperar a que termine de hablar antes de cerrar
                            delay(1200)
                            finish()
                        }
                    },
                    onStopSpeech = {
                        ttsEngine.stop()
                        finish()
                    }
                )
            }
        }
    }

    private fun checkInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroy() {
        com.asistente.celular.util.MicCoordinator.releaseMicLock("AssistantDialogActivity")
        sttEngine.release()
        ttsEngine.release()
        super.onDestroy()
    }
}

@Composable
fun FloatingAssistantBottomSheet(
    onDismiss: () -> Unit,
    onStartListening: (onStart: () -> Unit, onPartial: (String) -> Unit, onFinal: (String) -> Unit, onError: (Throwable) -> Unit) -> Unit,
    onStopListening: () -> Unit,
    onProcessCommand: (String, (SkillOutput) -> Unit) -> Unit,
    onStopSpeech: () -> Unit
) {
    var isListening by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var resultOutput by remember { mutableStateOf<SkillOutput?>(null) }
    var statusText by remember { mutableStateOf("Escuchando…") }

    LaunchedEffect(Unit) {
        isListening = true
        onStartListening(
            { isListening = true },
            { partial -> recognizedText = partial },
            { final ->
                isListening = false
                recognizedText = final
                isProcessing = true
                statusText = "Analizando…"
                onProcessCommand(final) { output ->
                    isProcessing = false
                    resultOutput = output
                    statusText = if (output.handledByAi) "Hendrix (IA)" else "Hendrix (Local)"
                }
            },
            { error ->
                isListening = false
                statusText = "Error: ${error.message}"
            }
        )
    }

    // Fondo oscurecido semitransparente que cubre la app anterior
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Evitar que el clic en la tarjeta cierre el diálogo
                    ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Cabecera con botón de cierre
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (isListening) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Transcripción de voz en tiempo real
                    if (recognizedText.isNotBlank()) {
                        Text(
                            text = "\"$recognizedText\"",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Respuesta del Asistente
                    resultOutput?.let { output ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                val harnessData = output.payload as? HarnessResult
                                val badgeText = harnessData?.let { "✨ ${it.usedModel.displayName}" }
                                    ?: if (output.handledByAi) "✨ Inteligencia Artificial" else "⚡ Motor Local"

                                Text(
                                    text = badgeText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = output.displayText,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Indicador animado inferior
                    AssistantStatusOrb(
                        isListening = isListening,
                        isProcessing = isProcessing,
                        isSpeaking = resultOutput != null
                    )
                }
            }
        }
    }
}
