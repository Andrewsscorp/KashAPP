# ADR 0007: Seguridad de Datos, Cifrado en Reposo con SQLCipher y Android Keystore

## Estado
Aprobado (Propuesta de Arquitectura Fase 1)

## Contexto
KashApp gestiona saldos, cuentas bancarias, transacciones, presupuestos e inversiones financieras. Estos datos clasifican como información financiera confidencial sujeta a estándares bancarios y de privacidad.

## Opciones Evaluadas
1. **Base de Datos SQLite en texto plano sin cifrar**:
   - *Riesgo*: Acceso indebido a los archivos de base de datos en dispositivos rooteados o volcados de memoria.
2. **Cifrado en reposo con SQLCipher (AES-256) integrado con Android Keystore**:
   - *Pros*: Cifrado transparente de todas las páginas de base de datos; clave criptográfica derivada y custodiada por el enclave de seguridad por hardware (TEE/StrongBox); cumplimiento de normas bancarias.

## Decisión
1. **Cifrado en Reposo**:
   - Uso de `net.zetetic:sqlcipher-android:4.6.0` con `SupportOpenHelperFactory` de Room.
   - Generación de clave simétrica de 256 bits resguardada en `AndroidKeyStore`.
2. **Enmascaramiento de PII**:
   - Números de cuenta y tokens se enmascaran (`•••• 1234`) para logs y vistas; nunca se registran números completos ni saldos en texto plano en `Log` o telemetría.
3. **Protección de Pantalla y Portapapeles**:
   - Activación de `FLAG_SECURE` en pantallas que muestran credenciales, saldos o configuración de seguridad.
4. **Respaldo Seguro**:
   - Configuración explícita de `dataExtractionRules` y `fullBackupContent` para excluir llaves criptográficas y datos efímeros de sesión.

## Consecuencias
- **Positivas**: Máxima protección de datos financieros; compatibilidad con dispositivos modernos de 16 KB page size; cero exposición de datos sensibles.
