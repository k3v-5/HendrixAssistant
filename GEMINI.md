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
