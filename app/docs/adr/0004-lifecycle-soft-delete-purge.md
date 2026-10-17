# ADR 0004: Ciclo de Vida de Entidades (Active → Archived → Purged) y Política de Integridad Referencial

## Estado
Aprobado (Propuesta de Arquitectura Fase 1)

## Contexto
En un sistema financiero, eliminar físicamente un registro maestro (ej. Categoría o Cuenta) que posee transacciones históricas causa inconsistencias contables o fallos de `FOREIGN KEY` (RESTRICT) o registros huérfanos (`ON DELETE CASCADE` destructivo no deseado).

## Opciones Evaluadas
1. **Borrado Físico Directo (`DELETE FROM ...`)**:
   - *Riesgo*: Destruye el historial de auditoría o deja transacciones sin categoría/cuenta asociada.
2. **Borrado Lógico con Estado Explícito (`Active` → `Archived` → `Purged`)**:
   - *Pros*: Preserva la integridad referencial y el ledger contable; oculta entidades inactivas en selectores de creación; permite reactivar o reasignar; soporta purga programada vía `WorkManager`.

## Decisión
1. **Ciclo de Vida de 3 Estados**:
   - `ACTIVE`: Disponible para nuevas transacciones y visible en todos los selectores.
   - `ARCHIVED`: Oculto para nuevas transacciones en pickers; visible en históricos/reportes; preserva referencias FK.
   - `PURGED`: Eliminación física permitida **únicamente** si el conteo de referencias históricas es `0` y ha transcurrido el periodo de retención (ej. 30 días de archivado).
2. **Política de Archivado con Entidad en Uso**:
   - Si una entidad tiene referencias activas (`usageCount > 0`), el flujo exige confirmación explícita del usuario o reasignación obligatoria a otra entidad activa del mismo tipo.
3. **Columnas de Control en Room**:
   - `isActive: Boolean` (por defecto `true`)
   - `archivedAt: Long?` (timestamp epoch millis)
   - `updatedAt: Long`
   - `version: Long` (control de concurrencia optimista)

## Consecuencias
- **Positivas**: Cero huérfanos; cero pérdidas de transacciones históricas; consultas optimizadas con índices parciales (`WHERE isActive = 1`).
- **Negativas / Mitigaciones**: Los DAOs deben filtrar por `isActive = 1` en las consultas orientadas a formularios de creación.
