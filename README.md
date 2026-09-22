# Asistente Celular - Android Offline-First Voice Assistant

> Asistente de voz inteligente para Android con arquitectura **Offline-First**, motor local de PNL/gramáticas (inspirado en **Dicio**), procesamiento de voz y palabra de activación en el dispositivo (inspirado en **Sherpa-ONNX**), y enrutamiento inteligente hacia modelos de Inteligencia Artificial (**Gemini, Groq, OpenAI, Ollama o SLM local**) para tareas complejas.

Para ver las directrices fundamentales y la fuente única de verdad del proyecto, consulta [PROJECT_OBJECTIVE.md](PROJECT_OBJECTIVE.md).

---

## 🌟 Características Principales

- **⚡ Latencia Cero y 100% Offline para el 90% de tareas:**
  - Encender y apagar la linterna.
  - Temporizadores y cuentas regresivas ("temporizador de 5 minutos", "cuenta regresiva de media hora").
  - Alarmas del sistema ("alarma a las 7 de la mañana", "despiértame a las 8 y media").
  - Apertura de cualquier aplicación instalada ("abre WhatsApp", "inicia Spotify").
  - Control multimedia ("pausa la música", "siguiente canción").
  - Hora y fecha en español ("qué hora es", "qué día es hoy").
- **🧠 Enrutador Inteligente hacia IA (LLM/SLM):**
  - Si la consulta es abierta, explicativa o creativa (*"explícame cómo funciona un motor de cohete"*, *"resume este texto"*), el motor detecta la complejidad o la ausencia de coincidencia local y la deriva automáticamente a la IA seleccionada.
  - Compatible con: **Google Gemini**, **Groq (Llama 3 ultra-rápido)**, **OpenAI (ChatGPT)**, **Ollama (servidor propio en red local)** y **SLM Local On-Device**.
  - Si no hay conexión a internet y se pide una tarea compleja, el asistente ofrece una respuesta offline informando al usuario sin romperse.
- **🎙️ Arquitectura de Audio y Voz:**
  - **Wake Word (KWS):** Detección continua de *"Oye Hendrix"* offline mediante Sherpa-ONNX KeywordSpotter.
  - **Reconocimiento de voz (STT):** Transcripción offline con modelos cuantizados ONNX (Zipformer / Whisper-tiny).
  - **Síntesis de voz (TTS):** Motor nativo de Android TTS para cero huella de almacenamiento y respuesta inmediata.
- **📱 Interfaz Moderna con Jetpack Compose y Material 3:**
  - Historial conversacional interactivo.
  - Indicador visual animado (Orb/Ondas) de escucha, procesamiento y habla.
  - Chips distintivos que muestran si cada respuesta provino del **Motor Local (⚡ NLU)** o de la **Inteligencia Artificial (✨ IA)**.
  - Pantalla completa de ajustes para configurar claves de API, modelos y activación por voz en segundo plano.

---

## 📁 Estructura del Proyecto

```
AsistenteCelular/
├── PROJECT_OBJECTIVE.md       # Documento maestro de arquitectura y visión
├── settings.gradle.kts        # Configuración multi-módulo
├── build.gradle.kts           # Configuración raíz de plugins
├── gradle/libs.versions.toml  # Catálogo centralizado de versiones
│
├── core-nlu/                  # Motor de Lenguaje Natural local (tipo Dicio)
│   ├── construct/             # Combinadores sintácticos (Word, Or, Sequence, Capturing, Regex)
│   ├── model/                 # SkillScore, Specificity
│   ├── parser/                # Parsers de números y fechas/horas en español
│   ├── skill/                 # Contratos Skill, SkillContext, SkillOutput, InteractionPlan
│   └── evaluator/             # SkillRanker y SkillEvaluator
│
├── core-voice/                # Motor de audio, KWS, STT y TTS (tipo Sherpa-ONNX)
│   ├── audio/                 # Captura de PCM a 16kHz Mono con AudioRecord
│   ├── kws/                   # Detector de Wake Word ("Oye Asistente")
│   ├── stt/                   # Motor de reconocimiento de voz offline
│   └── tts/                   # Síntesis nativa de Android con suspensión por corrutinas
│
├── core-ai/                   # Capa de Enrutamiento a IA y Clientes LLM
│   ├── analyzer/              # ComplexityAnalyzer (detecta preguntas complejas)
│   ├── client/                # LlmClient (Gemini, Groq, OpenAI, Ollama)
│   ├── model/                 # LlmConfig, AiProvider
│   └── router/                # AiRouterSkill (Fallback a IA)
│
├── skills-builtin/            # Habilidades integradas para control del teléfono
│   ├── alarm/                 # AlarmSkill
│   ├── timer/                 # TimerSkill
│   ├── flashlight/            # FlashlightSkill
│   ├── applauncher/           # AppLauncherSkill
│   ├── time/                  # CurrentTimeSkill
│   └── media/                 # MediaControlSkill
│
└── app/                       # Aplicación Android Jetpack Compose
    ├── ui/                    # AssistantScreen, SettingsScreen, Theme
    ├── service/               # AssistantVoiceService (escucha en segundo plano)
    ├── viewmodel/             # AssistantViewModel
    └── MainActivity.kt        # Actividad principal
```

---

## 🚀 Cómo Empezar

### Requisitos
1. **Android Studio** (Hedgehog, Iguana, Jellyfish o posterior).
2. **JDK 17 o 21**.
3. Un dispositivo Android con **Android 8.0 (API 26)** o superior.

### Abrir y Ejecutar
1. Abre Android Studio.
2. Selecciona **File -> Open...** y elige la carpeta `AsistenteCelular`.
3. Deja que Gradle sincronice las dependencias.
4. Conecta tu dispositivo Android (o inicia un emulador con micrófono habilitado).
5. Pulsa **Run 'app'** (`Shift + F10`).

### Configuración de Claves de API (Variables de Entorno)
Las claves de API se gestionan de forma segura fuera del repositorio en `local.properties` (ignorado por Git):
```properties
GEMINI_API_KEY=tu_clave_de_gemini_aqui
GROQ_API_KEY=tu_clave_de_groq_aqui
OPENAI_API_KEY=tu_clave_de_openai_aqui
```
Gradle lee automáticamente `local.properties` (o la variable de entorno del sistema `GEMINI_API_KEY`) y la inyecta de forma segura a través de `BuildConfig.DEFAULT_GEMINI_API_KEY`.

---

## 🧩 Cómo Añadir una Nueva Habilidad Local

Para añadir una nueva acción sin tocar los motores existentes:

1. Crea una clase que herede de `StandardRecognizerSkill`:
```kotlin
class MiNuevaSkill : StandardRecognizerSkill(
    info = SkillInfo(id = "mi_skill", name = "Mi Habilidad", description = "Descripción"),
    specificity = Specificity.NORMAL
) {
    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("activa", "inicia"),
            WordConstruct("mi", "la"),
            WordConstruct("funcion")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        // Tu lógica de Android aquí
        return SkillOutput(speech = "Función ejecutada correctamente.")
    }
}
```
2. Regístrala en la lista de habilidades en `AssistantViewModel.kt`:
```kotlin
private val localSkills = listOf(
    FlashlightSkill(),
    TimerSkill(),
    AlarmSkill(),
    AppLauncherSkill(),
    CurrentTimeSkill(),
    MediaControlSkill(),
    MiNuevaSkill() // <- Aquí
)
```
¡El motor sintáctico y el enrutador de IA harán el resto automáticamente!

---

## 🖥️ Control de PC Studio, Módulos y Dropzone Cloud

Hendrix Assistant incluye un **Quick Command Deck** y suite de automatizaciones para controlar tu estación de trabajo remota con **cero consumo de video/ahorro de datos móvil**:
* **DAWs & Audio:** Ableton Live, FL Studio (Transporte, atajos, exportar audio a Drive).
* **3D & Videojuegos:** Blender 3D, Unreal Engine 5.
* **Diseño & Video:** Adobe Premiere Pro, Photoshop.
* **Navegadores Web:** Chrome, Brave, Opera, Edge con búsquedas por voz parametrizadas (Google, YouTube, Facebook, GitHub).
* **Buzón Dropzone & Cloud Sync:** Sincronización automática de renders y entregables con Google Drive y OneDrive, y vigilante de archivos en segundo plano (`DropzoneWatcher`).

👉 Para ver la guía completa de despliegue, arquitectura y comandos de voz, consulta la [Guía de Automatización de PC Studio](docs/PC_STUDIO_AUTOMATION_GUIDE.md).

