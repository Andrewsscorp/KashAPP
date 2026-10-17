# 🏦 KashApp Secure Auth - Versión 9.0.0 (Producción)

[![Android](https://img.shields.io/badge/Platform-Android_14%2B-3DDC84.svg?style=flat&logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Room](https://img.shields.io/badge/Room_DB-v31_Encrypted-4285F4.svg?style=flat&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![SQLCipher](https://img.shields.io/badge/Cipher-AES--256--GCM-green.svg)](https://www.zetetic.net/sqlcipher/)
[![Version](https://img.shields.io/badge/Version-9.0.0-blue.svg)](https://github.com/Andrewsscorp/KashAPP)

**KashApp** es una plataforma financiera de grado corporativo y bancario para Android, diseñada bajo una arquitectura moderna modular, offline-first y reactiva con Jetpack Compose. Implementa cifrado simétrico y asimétrico de nivel militar, validación biométrica con respaldo en Hardware Security Module (Keystore), sincronización en segundo plano con WorkManager y cero dependencia de datos ficticios en producción.

---

## 🚀 Declaración de Versión: 9.0.0
- **Version Code:** `900`
- **Version Name:** `9.0.0`
- **Estado de Base de Datos:** **100% Limpia (0 datos de prueba)**. Todos los registros de activos fijos mock (`AF-00*`), comprobantes y transacciones de prueba han sido purgados. El sistema inicia en blanco para el usuario final con integridad referencial garantizada.

---

## 🏛️ Arquitectura Modular del Sistema

El proyecto está estructurado en módulos Gradle desacoplados de alta cohesión:

```
├── app/                  # Orquestador principal de UI, navegación, DI (AppContainer) y Auth
├── core-database/        # Capa de persistencia cifrada Room v31 (SQLCipher AES-256), DAOs y Entidades
├── core-model/           # Modelos de dominio y contratos de datos transversales
├── feature-activos/      # Motor de depreciación de activos fijos NIC 16 y proyección patrimonial
├── feature-informes/     # Reportes financieros, desglose analítico y gestor de soportes documentales
├── feature-objetivos/    # Metas financieras de ahorro e inversión con tracking de hitos
└── feature-ocr/          # Escaneo y procesamiento de facturas y recibos con ML Kit
```

---

## 🔐 Capa de Seguridad y Criptografía

1. **Bóveda SQLCipher AES-256 (Room v31):**
   - Base de datos local SQLite cifrada integralmente a nivel de página (4096 bytes).
   - Derivación de clave mediante PBKDF2 / Argon2id combinada con semilla de hardware generada en el Android Keystore.
   - Rotación y restablecimiento seguro de clave maestra sin fuga de memoria.

2. **Autenticación Biométrica y Google Sign-In:**
   - Compatibilidad completa con `androidx.biometric.BiometricPrompt` con validación de clave criptográfica `CryptoObject`.
   - Google Sign-In mediante Google Identity Credential Manager con soporte multi-cliente OAuth 2.0 (Android Client & Web Server Client ID).

3. **Cero Datos de Prueba (Zero Mock Baseline):**
   - Catálogos de bancos centrales (ACH / SWIFT), monedas globales y categorías estándar inicializadas sin saldos ni transacciones falsas.
   - Presupuestos, cuentas, tarjetas de crédito y activos fijos inician en cero absoluto, listos para la operación real del usuario.

---

## 📦 Ejecución y Compilación Local

### Requisitos Previos:
- **Android Studio Ladybug | 2024.2+** o superior.
- **JDK 17** o **JDK 21** configurado en Gradle.
- Android SDK 36 (targetSdk 36, minSdk 24).

### Pasos:
1. Clonar o abrir la carpeta `kashapp-secure-auth-v9.0` en Android Studio.
2. Asegurar que `app/google-services.json` esté ubicado correctamente en la raíz del módulo `app/`.
3. Sincronizar Gradle (`File -> Sync Project with Gradle Files`).
4. Compilar y ejecutar en emulador o dispositivo físico:
   ```bash
   ./gradlew assembleDebug
   ```
5. Para ejecutar el conjunto completo de pruebas unitarias:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 📝 Licencia y Derechos
© 2026 KashApp Financial Technologies. Todos los derechos reservados.
