# Directrices de Desarrollo y Reglas de Arquitectura

## Principio Fundamental: Escalabilidad Preventiva y Diseño a Futuro

Cada vez que el usuario solicite una funcionalidad, módulo, cambio o ajuste:

1. **Visión Prospectiva (Diseño para Crecimiento):**
   - No diseñar únicamente para el caso mínimo actual. Evaluar inmediatamente: *"Si en el futuro esta funcionalidad se vuelve 10x más grande, compleja o distribuida, ¿cómo debe estar diseñada hoy para soportarlo sin necesidad de refactorizar el núcleo?"*

2. **Desacoplamiento por Contratos e Interfaces:**
   - Todo componente debe interactuar a través de abstracciones (`interface`, contratos de datos puros y patrones de estrategia).
   - Ninguna capa superior debe depender de implementaciones concretas de bajo nivel.

3. **Arquitectura Abierta a la Extensión, Cerrada a la Modificación (OCP):**
   - Las nuevas características deben poder añadirse creando nuevos componentes o subclases (ej. nuevas `Skill`, nuevos motores `SttEngine` / `TtsEngine`, nuevos proveedores `LlmClient`), sin tener que modificar o reescribir las clases base existentes.

4. **Modelos de Datos Flexibles y Evolutivos:**
   - Diseñar las estructuras de datos (cargas útiles, estados de UI, configuraciones) con capacidad de crecimiento (slots opcionales, metadatos extensibles, versionado si aplica) para evitar migraciones destructivas.

5. **Cero Atajos Técnicos:**
   - Evitar soluciones "parche" o código acoplado que funcione en el momento pero obligue a una reescritura cuando la funcionalidad madure.

---

## Principios SOLID, Seguridad y Optimización Obligatoria

Todo desarrollo, refactorización o adición de código en el proyecto debe regirse estrictamente por:

1. **Principios SOLID:**
   - **S (Single Responsibility):** Cada clase, función o componente Compose debe tener una única responsabilidad bien delimitada. Separar lógica de negocio, persistencia, red y presentación.
   - **O (Open/Closed):** El código debe estar abierto a la extensión pero cerrado a la modificación. Nuevas capacidades se añaden mediante nuevos adaptadores, coordinadores o subclases.
   - **L (Liskov Substitution):** Cualquier implementación de una interfaz (`Skill`, `SttEngine`, `SmartDeviceDriver`, `PcWorkspaceBridge`, etc.) debe poder intercambiarse sin romper el comportamiento esperado del sistema.
   - **I (Interface Segregation):** Diseñar interfaces pequeñas, cohesivas y orientadas al cliente en lugar de contratos monolíticos inflados.
   - **D (Dependency Inversion):** Los módulos de alto nivel nunca deben depender de módulos de bajo nivel; ambos dependen de abstracciones. La inyección de dependencias (`AssistantSkillFactory`) debe ser limpia y predecible.

2. **Seguridad por Diseño (Security-First):**
   - **Cero Exposición de Secretos:** Claves de API, tokens o credenciales nunca deben estar en código duro (*hardcoded*); residen en `local.properties`, variables de entorno o almacén cifrado.
   - **Validación y Sanitización de Entradas:** Toda entrada proveniente de voz (ASR), texto, comandos remotos RPC o WebSockets debe ser validada sintácticamente antes de su procesamiento.
   - **Manejo Seguro de Permisos y Servicios Android:** Control estricto de excepciones (`SecurityException`), verificación en tiempo de ejecución de permisos de Android y límites de aislamiento en servicios en segundo plano (`AssistantVoiceService`, accesibilidad).
   - **Aislamiento en Canales de Red:** Protocolos locales (UDP Discovery, WebSockets) con autenticación previa (PIN/tokens) y mitigación de llamadas malformadas o paquetes corruptos.

3. **Optimización de Rendimiento y Recursos (High Efficiency):**
   - **Concurrencia Estructurada:** Uso meticuloso de Kotlin Coroutines con el despachador adecuado (`Dispatchers.IO` para disco/red, `Dispatchers.Default` para cómputo de PNL, `Dispatchers.Main` solo para UI). Evitar hilos bloqueantes o fugas de corrutinas (`viewModelScope`, `serviceScope` supervisados).
   - **Memoria y Ciclos de Vida:** Cero retención de referencias a `Context` o vistas en componentes de larga duración; optimización en Jetpack Compose evitando recomposiciones innecesarias (`derivedStateOf`, `remember`, keys estables).
   - **Economía de Batería y Red:** Operaciones de red ultraeficientes (< 1 KB por comando RPC/telemetría) y reposo de módulos de audio/IA cuando no están en escucha activa.

4. **Diseño Atómico y Componentes Altamente Reutilizables:**
   - En UI (Jetpack Compose): Dividir las vistas en componentes atómicos e independientes (botones, chips, tarjetas, estados visuales) que puedan ser reutilizados en diálogos flotantes, pantallas completas o widgets sin duplicación.
   - En lógica de negocio: Extraer utilidades, parsers y coordinadores como piezas atómicas reutilizables entre el `ViewModel`, el `AssistantVoiceService`, los receivers y las actividades.

5. **Código Limpio, Tipado y Autodocumentado:**
   - Escribir código fuertemente tipado en Kotlin, con estados inmutables exhaustivos (`StateFlow`, estados sellados `sealed interface`) y manejo robusto de excepciones.

6. **Validación Exhaustiva de Flujo y Pruebas Obligatorias (100% Pass Rate):**
   - **Pruebas Completas por Defecto:** Todo módulo, habilidad (`Skill`), coordinador, adaptador de red o componente de lógica debe implementarse junto con su suite de pruebas unitarias/integración correspondientes.
   - **Verificación de Flujo de Extremo a Extremo:** Las pruebas no deben limitarse a aserciones triviales; deben simular y validar el **flujo de interacción completo**:
     1. Entrada sintáctica / datos recibidos.
     2. Evaluación, coincidencia de gramáticas y puntuación (`score`).
     3. Ejecución de la acción y llamadas a contratos (`execute` con puentes mockeados o reales).
     4. Salida esperada (`SkillOutput`: habla `speech`, planes de interacción `InteractionPlan` y cargas útiles visuales `AssistantUiPayload`).
     5. Casos límite, cancelaciones y degradación elegante ante errores.
   - **Criterio de Aceptación Inmutable:** Ninguna tarea o funcionalidad se considerará concluida hasta que la suite completa de pruebas haya sido ejecutada y validada con **100% de éxito**.

---

## Documentación Continua de Avances y Pendientes

1. **Registro Sincrónico de Hitos:**
   - Cada nueva característica, refactorización estructural o módulo incorporado debe ser documentado de inmediato en los archivos maestros del proyecto ([PROJECT_INDEX.md](PROJECT_INDEX.md) y [README.md](README.md) si aplica).
2. **Control de Pendientes y Deuda Técnica (Roadmap Vivo):**
   - Mantener siempre visible y actualizada la sección de tareas pendientes, mejoras identificadas y siguientes pasos en la documentación del proyecto, permitiendo priorizar tareas de manera transparente.

---

## Protocolo de Indexación y Persistencia de Contexto para Agentes y LLMs

Para asegurar que cualquier agente o modelo de lenguaje (actual o futuro) retome el trabajo sin pérdida de contexto ni desalineación técnica, se establece el siguiente protocolo obligatorio:

1. **Indexación Integral Continua:**
   - Todo archivo, clase central, habilidad, contrato o servicio nuevo debe quedar indexado y catalogado en [PROJECT_INDEX.md](PROJECT_INDEX.md) con su propósito, ubicación en el árbol de directorios y relaciones de dependencia.
2. **Fase de Onboarding al Iniciar una Tarea:**
   - Al recibir una solicitud o iniciar una sesión, el agente/LLM debe inspeccionar de inmediato los documentos clave del proyecto:
     - `GEMINI.md`: Reglas y directrices de arquitectura.
     - `PROJECT_OBJECTIVE.md`: Visión, alcance y reglas inmutables de diseño.
     - `PROJECT_INDEX.md`: Estructura actual, catálogo de habilidades, módulos, estado de pruebas y backlog de tareas.
3. **Fase de Cierre y Transferencia de Contexto (Context Handoff):**
   - Al culminar cualquier tarea o bloque de trabajo, el agente/LLM debe:
     - Actualizar [PROJECT_INDEX.md](PROJECT_INDEX.md) reflejando los avances completados.
     - Documentar los pendientes inmediatos y el estado operativo del proyecto.
     - Dejar el entorno en un estado limpio, coherente y con las pruebas pasando al 100%, de forma que el siguiente agente pueda continuar sin fricción.
