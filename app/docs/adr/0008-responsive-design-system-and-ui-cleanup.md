# ADR 0008: Sistema de Diseño Adaptativo Universal, Responsividad Multi-Ventana y Consolidación de UI

## Estado
Implementado y Verificado (100% Completado — TASK-001 a TASK-025 en Producción / Git master)

## Contexto
En KashApp (v9.3.3 / v9.3.5), el análisis estructural de los 8 módulos Gradle y 284 archivos de interfaz de usuario evidenció defectos visuales severos en producción (aplastamiento de importes en columnas verticales de 1 carácter, botones de acción truncados como `"Rebalanc"` o `"Barrer"`, desaparición de controles de cabecera fuera del viewport derecho y recorte de letras descendentes en buscadores).

Dichos defectos derivan de la ausencia de un módulo `:core-ui` compartido, la falta de contratos de diseño en filas con `Arrangement.SpaceBetween`, el uso de alturas fijas rígidas (`.height(30..48.dp)`), la dispersión de 281 llamadas a formateo monetario sin `softWrap = false`, la pérdida de estado en cambios de configuración por falta de `rememberSaveable`, y la coexistencia de 42 archivos de componentes en inglés huérfanos/duplicados frente a los componentes activos en español.

Conforme al requerimiento normativo del producto, las directrices de responsividad deben aplicar a **todas y cada una de las pantallas de toda la aplicación**, así como a **cualquier pantalla o módulo que se desarrolle de aquí en adelante**.

## Opciones Evaluadas
1. **Parches Puntuales de Estilos por Pantalla**:
   - *Pros*: Rápido a muy corto plazo.
   - *Contras*: No previene regresiones; mantiene el código duplicado; rompe en tablets, modo horizontal o fuentes grandes (`fontScale 1.5x`); no ofrece garantías arquitectónicas para pantallas futuras.
2. **Sistema de Diseño Adaptativo Universal Basado en Módulo Compartido (`:core-ui`) y Contratos Estrictos**:
   - *Pros*: Elimina las 7 causas raíz identificadas; garantiza compatibilidad desde `320.dp` hasta `1280.dp`; proporciona componentes unificados (`CurrencyText`, `KashAdaptiveTopBar`, `KashCompactButton`, `KashSearchBar`, `KashKeyValueRow`, `KashResponsiveDialog`, `KashNumericKeyboard`); blinda permanentemente el repositorio contra código huérfano.
   - *Contras*: Requiere desacoplar dependencias y estructurar la suite de pruebas multi-resolución.

## Decisión
Adoptar el **Sistema de Diseño Adaptativo Universal de KashApp**, de obligatorio cumplimiento para todas las pantallas actuales y futuras:

### 1. Las Cinco Reglas de Oro de Composición
1. **Contrato de Filas Flexibles (`CR-01`)**: En cualquier `Row` con `Arrangement.SpaceBetween`, el contenedor informativo de la izquierda (`Row` o `Column`) debe llevar obligatoriamente `Modifier.weight(1f).padding(end = X.dp)` y sus textos deben incorporar `maxLines = 1, overflow = TextOverflow.Ellipsis` o envolverse en `FlowRow`. El bloque derecho de acciones/montos se mide con tamaño intrínseco garantizando su visibilidad íntegra.
2. **Elasticidad en Botones Compactos (`CR-02`)**: Queda terminantemente prohibido el uso de `.height(X.dp)` rígido inferior a `40.dp` en `Button` o `OutlinedButton`. Se debe utilizar `Modifier.heightIn(min = 40.dp)` con relleno compacto explícito (`PaddingValues(horizontal = 8..10.dp, vertical = 6.dp)`) y `Text(maxLines = 1, softWrap = false)`.
3. **Piso Mínimo en Campos de Texto (`CR-03`)**: Ningún `OutlinedTextField` o campo de búsqueda podrá tener una altura inferior a `56.dp` (`OutlinedTextFieldDefaults.MinHeight`) o `52.dp` en buscadores compactos dedicados (`KashSearchBar`), preservando sin recortes los trazos ascendentes y descendentes (`p`, `g`, `q`, `j`, `y`).
4. **Formateo Monetario Inquebrantable (`CR-04`)**: Toda cifra monetaria, porcentaje, tasa o indicador numérico debe renderizarse mediante el componente centralizado `CurrencyText` (o `softWrap = false, maxLines = 1`), garantizando que jamás se divida en renglones verticales dentro de una cifra (`lineCount == 1`).
5. **Preservación Universal del Estado (`CR-06`)**: Toda bandera de navegación de nivel superior, índice de pestaña, modo de privacidad o borrador editable debe implementarse mediante `rememberSaveable { mutableStateOf(...) }` almacenando tipos primitivos (`Boolean`, `Int`, `String?` para IDs de entidad), garantizando resiliencia ante rotaciones de pantalla, multiventana y redimensionamiento.

### 2. Catálogo Canónico de Pantallas Activas
Permanecen como componentes oficiales y fueron adaptados en la Fase 3:
- **Core / Finanzas (`:app`)**: `LoginScreen`, `DashboardScreen` (12 tarjetas activas en español), `FinancialRubroDetailScreen`, `BudgetsScreen`, `CreateBudgetScreen`, `BankAccountsManagementScreen`, `CreateAccountScreen`, `CreateExpenseScreen`, `CreateIncomeScreen`, `CreateTransferScreen`, `AddCreditCardScreen`, `CreateCardPurchaseScreen`, `CreateGoalScreen`, `FixedAssetsScreen` (4 pestañas), `UpcomingPaymentsScreen`, `CurrencyTrmScreen`, `AuditModuleScreen`, `SettingsScreen`, `ThirdPartiesHubScreen`.
- **Módulos de Features**: `ReceiptScannerScreen` (`:feature-ocr`), `InformesScreen` (6 pestañas, `:feature-informes`), `ObjetivosScreen` (5 pestañas, `:feature-objetivos`), `SharedExpensesHubScreen` (`:feature-gastos-compartidos`), `FixedAssetsAnalyticsBanner` (`:feature-activos`).

### 3. Consolidación de Utilidades y Limpieza de Huérfanos
- **Unificación Inmediata (Fase 1 y 2)**:
  - `MoneyFormatter`: Consolidado en `core-model/src/main/kotlin/com/example/util/MoneyFormatter.kt`; la versión en `:app` delega como alias.
  - `IPhoneAdaptiveDatePickerField`: Movido a `:core-ui`; eliminada la copia duplicada en `fixedassets/create/components/`.
  - `CurrencyText`: Promovido a `:core-ui` para acceso transversal de todos los módulos.
- **Limpieza de Huérfanos (Fase 4 - TASK-023)**: Se eliminaron 35 componentes huérfanos sin uso (29 del dashboard en inglés y 6 widgets de tarjetas no referenciados) y se resolvió el conflicto de nombre de `FirebaseAiCloudProviderBridge.kt`. Se preservaron intactas las 14 pantallas secundarias activas de `SettingsScreen`.

## Verificación y Garantía Continua (Fase 5)
1. **Suite de Aceptación Responsiva (`TASK-024`)**:
   Implementada en `app/src/test/java/com/example/ui/responsiveness/ResponsivenessAcceptanceTestSuite.kt` cubriendo los 12 escenarios de diseño (`TST-01` a `TST-12`):
   - `TST-01`: Teléfono ultra-compacto 320dp (cero desbordamiento horizontal y cifras single-line).
   - `TST-02`: Teléfono estándar 360-411dp (resolución de capturas reportadas de producción).
   - `TST-03`: Plegable / Tablet media 600dp (rejillas de 2 columnas y espaciados medianos).
   - `TST-04`: Tablet grande / Escritorio 1024dp (centrado elástico a 960dp y 3 columnas).
   - `TST-05`: Rotación de pantalla y preservación de estado con `StateRestorationTester`.
   - `TST-06`: Multiventana y redimensionamiento dinámico en tiempo de ejecución.
   - `TST-07`: Textos extremos, cifras multimillonarias ($1,234,567,890.50) y descripciones largas.
   - `TST-08`: Formularios de creación con errores activos de validación en campos obligatorios.
   - `TST-09`: Teclado numérico virtual compacto y comportamiento ante IME en apaisado.
   - `TST-10`: Modales, diálogos y bottom sheets con `heightIn(max = maxHeight * 0.9f)` y scroll vertical.
   - `TST-11`: Accesibilidad con `fontScale = 1.5x` y objetivos táctiles WCAG $\ge 48\,\text{dp}$ / $\ge 40\,\text{dp}$.
   - `TST-12`: Regresión de navegación con `BackHandler` y preservación de bloqueo de sesión.
2. **Guarda Arquitectónica Estática (`TASK-025`)**:
   Implementada en `app/src/test/java/com/example/ArchitectureResponsivenessGuardTest.kt` validando en cada compilación:
   - Cero `OutlinedTextField` con altura rígida menor a 56dp.
   - Cero `Button`/`OutlinedButton` con altura rígida menor a 40dp.
   - Navegación principal del dashboard respaldada por `rememberSaveable`.
   - Uso obligatorio de `CurrencyText` en módulos financieros.

## Consecuencias
- **Positivas**: Cero textos mutilados o botones truncados; soporte impecable en teléfonos compactos (`320dp`), estándar (`360–411dp`), plegables (`600dp`) y tablets/escritorio (`840–1280dp`); cero pérdida de datos al rotar el dispositivo.
- **Gobernanza Visual**: Toda pantalla futura creada en el proyecto está sujeta a la guarda estática arquitectónica (`ArchitectureResponsivenessGuardTest.kt`).

## Plan de Reversa
Si un contenedor de terceros o visualización gráfica no estándar requiere dimensiones fijas por especificación de biblioteca externa (ej. gráficos o vistas de cámara), deberá encapsularse obligatoriamente dentro de un contenedor elástico con `BoxWithConstraints` o `Modifier.aspectRatio()`, respetando los límites de `maxWidth` y `maxHeight`.
