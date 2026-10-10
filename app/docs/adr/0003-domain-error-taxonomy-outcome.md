# ADR 0003: Taxonomía de Errores de Dominio y Tipo de Retorno Outcome<T, E>

## Estado
Aprobado (Propuesta de Arquitectura Fase 1)

## Contexto
El código previo utilizaba `kotlin.Result<T>` o lanzaba excepciones crudas (`ReferentialIntegrityException`, `SQLiteConstraintException`). `kotlin.Result` limita el tipo de error a `Throwable`, obligando a la UI o a los Use Cases a parsear mensajes de texto o realizar `is SQLiteConstraintException`, acoplando la UI a la tecnología de base de datos.

## Opciones Evaluadas
1. **Excepciones Tradicionales (Checked/Unchecked)**:
   - *Contras*: Flujo de control invisible; riesgo de tragar `CancellationException` de Coroutines en bloques `try-catch` genéricos.
2. **`kotlin.Result<T>` estándar**:
   - *Contras*: Solo permite `Throwable`; pierde tipado estricto del dominio.
3. **Tipo Funcional `Outcome<Value, Error>` con Jerarquía Sellada `DomainError`**:
   - *Pros*: Errores fuertemente tipados; modelado exhaustivo con `when`; relanza siempre `CancellationException`; desacopla la capa de persistencia de la capa de presentación.

## Decisión
Implementar el tipo de retorno funcional `Outcome<out T, out E>` y la jerarquía sellada `DomainError`:
```kotlin
sealed interface Outcome<out T, out E> {
    data class Success<out T>(val value: T) : Outcome<T, Nothing>
    data class Failure<out E>(val error: E) : Outcome<Nothing, E>
}

sealed interface DomainError {
    sealed interface Validation : DomainError {
        data object Blank : Validation
        data class TooLong(val max: Int, val current: Int) : Validation
        data class InvalidCharacters(val details: String) : Validation
        data class NegativeAmount(val amount: Long) : Validation
    }
    sealed interface Conflict : DomainError {
        data class DuplicateName(val name: String) : Conflict
        data class ConcurrentModification(val currentVersion: Long) : Conflict
    }
    sealed interface NotFound : DomainError {
        data class EntityNotFound(val entityType: String, val id: String) : NotFound
    }
    sealed interface Integrity : DomainError {
        data class InUse(val entityType: String, val id: String, val referenceCount: Int) : Integrity
    }
    sealed interface Storage : DomainError {
        data class DiskFull(val cause: Throwable?) : Storage
        data class DatabaseCorrupted(val cause: Throwable?) : Storage
        data class DatabaseLocked(val cause: Throwable?) : Storage
    }
    data class Unexpected(val cause: Throwable) : DomainError
}
```
Un componente centralizado `PersistenceErrorTranslator` traduce excepciones de SQLite/Room a `DomainError`.

## Consecuencias
- **Positivas**: La UI y ViewModels manejan estados de error exhaustivos con mensajes localizados sin lidiar con excepciones SQL; cero fugas de `CancellationException`.
- **Negativas / Mitigaciones**: Requiere transformar resultados en los UseCases/Repositorios mediante funciones de orden superior (`map`, `flatMap`, `onFailure`).
