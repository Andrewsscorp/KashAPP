# ADR 0002: Estrategia de Identidad con UUIDv7 Ordenable por Tiempo

## Estado
Aprobado (Propuesta de Arquitectura Fase 1)

## Contexto
Las entidades maestras y transaccionales requieren identificadores únicos. Históricamente se usaron combinaciones de prefijos de texto arbitrarios (`cat_...`, `acc_...`) y `UUID.randomUUID()` (v4). El uso de UUIDv4 introduce fragmentación de índices B-Tree en SQLite debido a su aleatoriedad, y los IDs autoincrementales de SQLite impiden la generación de IDs en el cliente para operaciones offline e idempotentes.

## Opciones Evaluadas
1. **SQLite AUTOINCREMENT (Entero de 64 bits)**:
   - *Pros*: Claves compactas, inserción secuencial óptima.
   - *Contras*: Requiere insertar en base de datos antes de conocer el ID; colisiones en arquitecturas distribuidas / sync offline; no permite idempotencia cliente.
2. **UUIDv4 (Aleatorio)**:
   - *Pros*: Cero colisiones sin coordinación central.
   - *Contras*: Dispersión no secuencial de índices B-Tree; degrada el rendimiento de inserción en lotes grandes (100k+ registros).
3. **UUIDv7 (Secuencial con Timestamp de Unix de 48 bits + Entropía)**:
   - *Pros*: Ordenable naturalmente por tiempo de creación; inserción contigua óptima en B-Trees de SQLite; generación en cliente antes de persistir; apto para sincronización offline e importación/exportación.

## Decisión
Adoptar **UUIDv7** (o generación con timestamp ordenable de 64 bits + entropía) administrada por una interfaz inyectable `IdGenerator`:
```kotlin
interface IdGenerator {
    fun generateId(prefix: String = ""): String
}
```
Para pruebas unitarias deterministas, se provee `DeterministicIdGenerator` con secuencias reproducibles.

## Consecuencias
- **Positivas**: Inserción de alto rendimiento en Room; orden cronológico intrínseco; soporte completo de operaciones idempotentes (doble tap descartado por PK duplicada en outbox).
- **Negativas / Mitigaciones**: Requiere que las entidades de dominio y Room acepten `String` o value classes tipadas (`CategoryId`, `AccountId`).
