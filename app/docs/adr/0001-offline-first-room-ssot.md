# ADR 0001: Arquitectura Offline-First con Single Source of Truth (SSOT) en Room

## Estado
Aprobado (Propuesta de Arquitectura Fase 1)

## Contexto
En KashApp, las entidades financieras compartidas (Categorías, Cuentas, Divisas, Bancos, Comercios, Metas, Presupuestos) eran creadas o consumidas en múltiples pantallas (`CreateExpenseScreen`, `CreateIncomeScreen`, `CreateBudgetScreen`, `CreateAccountScreen`, `DashboardScreen`, `SettingsScreen`). Varios ViewModels y pantallas mantenían listas locales mutables en memoria, fallbacks hardcodeados o modelos duplicados (`ExpenseCategory` vs `CategoryEntity`), provocando que una entidad creada en una pantalla no se reflejara reactivamente en las demás sin recargar o reiniciar la aplicación.

## Opciones Evaluadas
1. **Estado en Memoria Global / EventBus (Bus de Eventos)**:
   - *Pros*: Fácil de implementar a corto plazo.
   - *Contras*: Propenso a fugas de memoria, se destruye en *process death*, carece de garantías transaccionales ACID y no resuelve la persistencia offline.
2. **Room como Single Source of Truth (SSOT) reactivo con Kotlin Coroutines Flow**:
   - *Pros*: Persistencia transaccional ACID, invalidación reactiva por tabla (`InvalidationTracker`), resiliencia ante rotación y *process death*, consistencia garantizada en todas las pantallas observadoras.
   - *Contras*: Requiere diseño cuidadoso de consultas, normalización y mappers inmutables entre capas.

## Decisión
Adoptar **Room como la única Fuente de Verdad (SSOT)** para todos los datos del dominio:
- Ninguna pantalla ni ViewModel mantendrá copias maestras o cachés mutables paralelas de catálogos compartidos.
- Toda mutación (`create`, `update`, `archive`, `delete`) se ejecuta transaccionalmente en Room.
- Todas las pantallas observan el estado mediante `Flow<List<DomainModel>>` expuesto por Repositorios singleton, transformado en los ViewModels a `StateFlow<UiState>` mediante `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)`.
- La UI almacena únicamente el `id` de la entidad seleccionada; los datos descriptivos se resuelven reactivamente desde el catálogo observado.

## Consecuencias
- **Positivas**: Propagación automática instantánea (<100ms) entre todas las pantallas activas; cero inconsistencias de catálogo; resiliencia ante recreación de actividades y ciclo de vida de Compose.
- **Negativas / Mitigaciones**: El `InvalidationTracker` invalida por tabla; se mitiga aplicando `distinctUntilChanged()` en repositorios y proyecciones mínimas en DAOs.

## Plan de Reversa
Si una entidad específica requiere aislamiento temporal no persistente (ej. borradores de formulario descartables), dicho estado residirá exclusivamente en el `SavedStateHandle` del ViewModel correspondiente, sin alterar el SSOT de Room.
