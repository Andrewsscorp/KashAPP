# ADR 0006: Componente Reutilizable Universal EntityPicker<T> y Formulario de Creación Inline

## Estado
Aprobado (Propuesta de Arquitectura Fase 1)

## Contexto
Las pantallas de gastos, ingresos, transferencias, metas, presupuestos y ajustes implementaban sus propios selectores, diálogos emergentes y hojas inferiores de creación (`CreateCustomCategoryDialog`, `CreateCustomInstitutionDialog`, `SelectAccountTypeBottomSheet`, diálogos locales en `CreateExpenseScreen`). Esto provocaba duplicación de lógica de validación, discrepancias visuales de diseño y comportamientos dispares al autoseleccionar entidades recién creadas.

## Opciones Evaluadas
1. **Componentes ad-hoc por pantalla**:
   - *Falla*: Viola el principio DRY, duplica código de validación y genera inconsistencias en accesibilidad y diseño.
2. **Componente Genérico Universal `EntityPicker<T>` con `EntityCreateBottomSheet<T>`**:
   - *Pros*: Una sola implementación auditada, accesible (TalkBack, teclado, 48dp+), con búsqueda con debounce, validación reactiva en vivo y autoselección atómica post-creación.

## Decisión
Desarrollar en `:core:ui` el componente Compose genérico `EntityPicker<T>`:
- **Parámetros**:
  - `selectedId: String?`
  - `entities: ImmutableList<T>`
  - `label: String`
  - `placeholder: String`
  - `onSelect: (T) -> Unit`
  - `onCreateNew: ((name: String, extraArgs: Any?) -> Unit)?`
- **Características**:
  - Búsqueda en tiempo real mediante `TextNormalizer.normalize(query)`.
  - Estado vacío interactivo con botón directo "+ Crear [Entidad]".
  - Apertura de formulario unificado en `ModalBottomSheet` con validación en vivo conectada al use case de dominio.
  - Al completar la creación, la nueva entidad se inserta en Room, el repositorio emite la lista actualizada, y el `EntityPicker` la selecciona automáticamente sin interacción adicional.

## Consecuencias
- **Positivas**: Reducción masiva de código duplicado en pantallas; experiencia de usuario 100% homogénea en toda la app; cumplimiento de WCAG 2.2 AA.
