# ADR 0005: Normalización Canónica de Texto, Diacríticos y Unicidad en Base de Datos

## Estado
Aprobado (Propuesta de Arquitectura Fase 1)

## Contexto
En SQLite, la cláusula `COLLATE NOCASE` solo cubre caracteres ASCII (A-Z) y no reconoce equivalencias en español como "Café" vs "cafe" vs "CAFÉ" o "Año" vs "AÑO". Además, espacios múltiples o caracteres invisibles ("  Alimentación  ") introducen duplicados lógicos indeseados en catálogos.

## Opciones Evaluadas
1. **Delegar unicidad a `COLLATE NOCASE` en SQLite**:
   - *Falla*: Permite duplicados como "Alimentación" y "alimentacion" o "Préstamo" y "Prestamo".
2. **Validación únicamente en memoria en ViewModel**:
   - *Falla*: Expuesta a condiciones de carrera ante inserciones simultáneas concurrentes.
3. **Columna `nameNormalized` calculada en Dominio + Índice Único en Room**:
   - *Pros*: Garantía matemática a nivel de base de datos; insensible a mayúsculas, acentos, diacríticos y espacios en blanco duplicados.

## Decisión
Implementar un `TextNormalizer` canónico y almacenar una columna `nameNormalized` en todas las entidades de catálogo (`categories`, `banks`, `merchants`, `currencies`, `income_sources`):
```kotlin
object TextNormalizer {
    fun normalize(input: String): String {
        val trimmed = input.trim().replace(Regex("\\s+"), " ")
        val nfkd = java.text.Normalizer.normalize(trimmed, java.text.Normalizer.Form.NFKD)
        val withoutDiacritics = nfkd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return withoutDiacritics.lowercase(java.util.Locale.ROOT)
    }
}
```
Regla de unicidad en Room:
```kotlin
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["nameNormalized", "type"], unique = true)
    ]
)
```
Política de negocio: "Café" y "Cafe" representan la misma entidad canónica para prevenir duplicidad accidental por tipeo en dispositivos móviles.

## Consecuencias
- **Positivas**: Cero duplicados lógicos; búsquedas rápidas mediante prefijo en `nameNormalized`; seguridad transaccional total ante condiciones de carrera en inserciones concurrentes.
