# Especificación de Arquitectura de Datos y Diagramas de Sistema

## 1. Diagrama Entidad-Relación Objetivo (Mermaid ER)

```mermaid
erDiagram
    CATEGORIES {
        string id PK
        string userId
        string name
        string nameNormalized UK "UNIQUE INDEX (nameNormalized, type)"
        string icon
        string colorHex
        string type "EXPENSE | INCOME"
        boolean isActive "1 = Active, 0 = Archived"
        int sortOrder
        long createdAt
        long updatedAt
        long archivedAt
        long version "Optimistic Locking"
    }

    ACCOUNTS {
        string id PK
        string userId FK
        string name
        string nameNormalized UK "UNIQUE INDEX (nameNormalized, userId)"
        double balance
        string currency
        string type "SAVINGS | CHECKING | CREDIT_CARD | CDT | INVESTMENT"
        string bankName
        boolean isFavorite
        boolean isActive
        long createdAt
        long updatedAt
        long archivedAt
        long version
    }

    TRANSACTIONS {
        string id PK
        string userId FK
        string accountId FK "ON DELETE RESTRICT"
        string accountNameSnapshot
        string categoryId FK "ON DELETE RESTRICT"
        string categoryNameSnapshot
        double amount
        string currency
        string type "EXPENSE | INCOME | TRANSFER"
        string date
        string description
        string receiptUrl
        string idempotencyKey UK
        long createdAt
        long updatedAt
    }

    MERCHANTS {
        string id PK
        string userId FK
        string canonicalName
        string nameNormalized UK "UNIQUE INDEX (nameNormalized, userId)"
        string alias
        string defaultCategoryId FK "ON DELETE SET NULL"
        int classificationConfidence
        boolean isActive
        long createdAt
        long updatedAt
    }

    BUDGETS {
        string id PK
        string userId FK
        string name
        string period "MONTHLY | WEEKLY | ANNUAL"
        double totalLimit
        long startDate
        long endDate
        boolean isActive
        long createdAt
        long updatedAt
    }

    BUDGET_ITEMS {
        string id PK
        string budgetId FK "ON DELETE CASCADE"
        string categoryId FK "ON DELETE RESTRICT"
        double allocatedLimit
        double spentAmount
        long createdAt
        long updatedAt
    }

    FINANCIAL_GOALS {
        string id PK
        string userId FK
        string name
        double targetAmount
        double currentAmount
        long targetDate
        string fundingAccountId FK "ON DELETE SET NULL"
        boolean isCompleted
        long createdAt
        long updatedAt
    }

    OUTBOX_OPERATIONS {
        string id PK
        string userId FK
        string entityType
        string entityId
        string operation "CREATE | UPDATE | ARCHIVE | DELETE"
        string payloadJson
        int retryCount
        long nextRetryAt
        string idempotencyKey UK
        string status "PENDING | DELIVERED | FAILED"
        string lastError
        long createdAt
    }

    CATEGORIES ||--o{ TRANSACTIONS : "categorizes"
    CATEGORIES ||--o{ BUDGET_ITEMS : "budget envelope"
    CATEGORIES ||--o{ MERCHANTS : "default category"
    ACCOUNTS ||--o{ TRANSACTIONS : "ledger origin/destination"
    ACCOUNTS ||--o{ FINANCIAL_GOALS : "funds"
    BUDGETS ||--o{ BUDGET_ITEMS : "contains"
```

## 2. Grafo de Dependencias entre Capas

```mermaid
graph TD
    subgraph Presentation ["Capa de Presentación (Compose & UI)"]
        UI["Composables (Screens & EntityPicker)"]
        VM["ViewModels (UiState, UiAction, UiEffect)"]
    end

    subgraph Domain ["Capa de Dominio (Kotlin Puro)"]
        UC["UseCases (Reglas de Negocio & Validación)"]
        Model["Modelos de Dominio & Value Classes (CategoryId, CategoryName)"]
        Outcome["Outcome<T, DomainError>"]
        RepoContract["Contratos de Repositorio (Interfaces)"]
    end

    subgraph Data ["Capa de Datos"]
        RepoImpl["Implementaciones de Repositorio (Singletons)"]
        Mappers["Mappers Bidireccionales (Entity <-> Domain)"]
        ErrorTranslator["PersistenceErrorTranslator"]
    end

    subgraph Database ["Capa de Persistencia Local (Room SSOT)"]
        RoomDB["SecurityDatabase (SQLCipher AES-256)"]
        DAOs["DAOs Reactivos (Flow<List<Entity>> & ACID @Transaction)"]
        Tables["Tablas con Índices Únicos y FKs"]
    end

    UI --> VM
    VM --> UC
    VM --> RepoContract
    UC --> RepoContract
    UC --> Model
    UC --> Outcome
    RepoContract --> Model
    RepoImpl --> RepoContract
    RepoImpl --> DAOs
    RepoImpl --> Mappers
    RepoImpl --> ErrorTranslator
    DAOs --> RoomDB
    RoomDB --> Tables
```

## 3. Máquina de Estados del Ciclo de Vida de Entidades

```mermaid
stateDiagram-v2
    [*] --> Active : Creación Atómica (UUIDv7 + Normalización)
    Active --> Active : Actualización (name, color, icon)
    Active --> Archived : Archivar (archivedAt = now, isActive = 0)
    
    state "Verificar Referencias Activas" as CheckRefs
    Archived --> CheckRefs : Solicitud de Purga
    CheckRefs --> Purged : Referencias = 0 y Periodo de Retención Cumplido (WorkManager)
    CheckRefs --> Archived : Referencias > 0 (Rechazado con DomainError.Integrity.InUse)
    
    Archived --> Active : Restaurar (isActive = 1, archivedAt = null)
    Purged --> [*]
```
