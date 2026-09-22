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

## Principios SOLID y Buenas Prácticas Obligatorias

Todo desarrollo, refactorización o adición de código en el proyecto debe regirse estrictamente por:

1. **Principios SOLID:**
   - **S (Single Responsibility):** Cada clase, función o componente Compose debe tener una única responsabilidad bien delimitada. Separar lógica de negocio, persistencia, red y presentación.
   - **O (Open/Closed):** El código debe estar abierto a la extensión pero cerrado a la modificación. Nuevas capacidades se añaden mediante nuevos adaptadores, coordinadores o subclases.
   - **L (Liskov Substitution):** Cualquier implementación de una interfaz (`Skill`, `SttEngine`, `SmartDeviceDriver`, `PcWorkspaceBridge`, etc.) debe poder intercambiarse sin romper el comportamiento esperado del sistema.
   - **I (Interface Segregation):** Diseñar interfaces pequeñas, cohesivas y orientadas al cliente en lugar de contratos monolíticos inflados.
   - **D (Dependency Inversion):** Los módulos de alto nivel nunca deben depender de módulos de bajo nivel; ambos dependen de abstracciones. La inyección de dependencias (`AssistantSkillFactory`) debe ser limpia y predecible.

2. **Diseño Atómico y Componentes Altamente Reutilizables:**
   - En UI (Jetpack Compose): Dividir las vistas en componentes atómicos e independientes (botones, chips, tarjetas, estados visuales) que puedan ser reutilizados en diálogos flotantes, pantallas completas o widgets sin duplicación.
   - En lógica de negocio: Extraer utilidades, parsers y coordinadores como piezas atómicas reutilizables entre el `ViewModel`, el `AssistantVoiceService`, los receivers y las actividades.

3. **Código Limpio, Tipado y Escalabilidad Preventiva:**
   - Escribir código autodocumentado, fuertemente tipado en Kotlin, con manejo exhaustivo de estados (`StateFlow`, estados sellados `sealed interface`) y manejo robusto de excepciones y corrutinas.

4. **Validación Exhaustiva de Flujo y Pruebas Obligatorias:**
   - **Pruebas Completas por Defecto:** Todo módulo, habilidad (`Skill`), coordinador, adaptador de red o componente de lógica debe implementarse junto con su suite de pruebas unitarias/integración correspondientes.
   - **Verificación de Flujo de Extremo a Extremo:** Las pruebas no deben limitarse a aserciones triviales; deben simular y validar el **flujo de interacción completo**:
     1. Entrada sintáctica / datos recibidos.
     2. Evaluación, coincidencia de gramáticas y puntuación (`score`).
     3. Ejecución de la acción y llamadas a contratos (`execute` con puentes mockeados o reales).
     4. Salida esperada (`SkillOutput`: habla `speech`, planes de interacción `InteractionPlan` y cargas útiles visuales `AssistantUiPayload`).
     5. Casos límite, cancelaciones y degradación elegante ante errores.
   - **Criterio de Aceptación Inmutable:** Ninguna tarea o funcionalidad se considerará concluida hasta que la suite completa de pruebas haya sido ejecutada y validada con **100% de éxito**.


