# Owlsburg OPS – Vollständige Architektur-Dokumentation

## Inhaltsverzeichnis

1. [Systemübersicht](#1-systemübersicht)
2. [Tech Stack & Infrastruktur](#2-tech-stack--infrastruktur)
3. [Multi-Tenancy & RLS](#3-multi-tenancy--rls)
4. [Domänen-Module](#4-domänen-module)
5. [Agent-System – Übersicht](#5-agent-system--übersicht)
6. [Agent-System – OOP-Architektur im Detail](#6-agent-system--oop-architektur-im-detail)
7. [Agent-Lifecycle & Execution](#7-agent-lifecycle--execution)
8. [Tool-System](#8-tool-system)
9. [LLM-Integration](#9-llm-integration)
10. [Memory-System](#10-memory-system)
11. [Agent-Messaging](#11-agent-messaging)
12. [Chat & SSE-Streaming](#12-chat--sse-streaming)
13. [Scheduling & Cleanup](#13-scheduling--cleanup)
14. [Frontend-Architektur](#14-frontend-architektur)
15. [Datenbank-Schema](#15-datenbank-schema)
16. [Sicherheit & Isolation](#16-sicherheit--isolation)
17. [Datenfluss-Beispiele](#17-datenfluss-beispiele)

---

## 1. Systemübersicht

Owlsburg OPS ist eine agentenbasierte Operations-Plattform für eine Auto-Sitz-Manufaktur. Das System kombiniert klassische Business-Domänen (Produktion, Maschinen, Lager, Personal, Kunden, Support) mit einem hierarchischen KI-Agentensystem, das als echte Java-Objekte existiert und über eine Tool-Registry auf die Domänen-Services zugreift.

**Kernprinzipien:**
- Alle Geschäftslogik ist deterministisch – kein LLM in der Domänenlogik
- Agenten greifen nur über die Tool-Registry auf Services zu
- Single-Schema mit PostgreSQL Row Level Security (RLS) für Multi-Tenancy
- Modularer Monolith (Package-by-Domain)
- Token-Verbrauch und Kosten werden von Anfang an erfasst

**Monorepo-Struktur:**
```
sindojan_ops_remastered/
├── CLAUDE.md              # Projekt-Dokumentation
├── ARCHITECTURE.md        # Diese Datei
├── frontend/              # Next.js 16 + React 19 + shadcn/ui
└── backend/               # Spring Boot 3.5.0 + Java 21
    └── src/main/java/com/owlsburg/ops/
        ├── common/        # BaseEntity, TenantContext, Exceptions
        ├── config/        # Security, JPA, Flyway, CORS
        ├── auth/          # JWT, Login, User-CRUD
        ├── tenant/        # Tenant-Management, SystemCompanyController
        ├── customers/     # Kunden, Kontakte, Adressen
        ├── production/    # Jobs, Stationen, Schichten, QA
        ├── machines/      # Maschinen, Wartung, Störungen
        ├── people/        # Mitarbeiter, Zeiterfassung
        ├── inventory/     # Lager, Artikel, Bestand
        ├── bom/           # Stücklisten, Arbeitspläne
        ├── documents/     # Dokumente (MinIO)
        ├── knowledge/     # Wissensdatenbank
        ├── inbox/         # Conversations, Nachrichten
        ├── events/        # Domain Events, Triggers
        └── agentinfra/    # ← Das Agent-System
            ├── dto/
            ├── llm/       # LLM Provider Abstraktion
            ├── runtime/   # Agent Interface, Factory, Records
            ├── memory/    # Persistent Memory System
            ├── messaging/ # Agent-zu-Agent Kommunikation
            ├── tools/     # Tool Registry + Interface
            │   └── impl/  # 40 Tool-Implementierungen
            ├── execution/ # Orchestrator, SystemPromptBuilder
            └── events/    # Event Subscriptions
```

---

## 2. Tech Stack & Infrastruktur

| Layer | Technologie |
|-------|-------------|
| Frontend | Next.js 16, React 19, shadcn/ui, Tailwind CSS v4, TypeScript |
| Backend | Spring Boot 3.5.0, Java 21, Maven |
| Datenbank | PostgreSQL (Single-Schema + RLS) |
| Migrations | Flyway (V1–V21, flache Struktur) |
| Auth | JWT (JJWT 0.12.6, stateless), Spring Security |
| Dateispeicher | MinIO |
| Rate Limiting | Bucket4j (Login: 10 req/min pro IP) |

**Ports:**
- Backend: 8080
- Frontend Dev: 4201
- PostgreSQL: 5432 (Docker)
- MinIO: 9000 (API), 9002 (Console)

---

## 3. Multi-Tenancy & RLS

**Architektur:** Single-Schema, alle Tabellen in `public` mit `tenant_id UUID NOT NULL`.

**Enforcement-Kette:**
1. `TenantAwareDataSource` (DelegatingDataSource) setzt `set_config('app.current_tenant', ?, false)` auf jeder JDBC Connection
2. PostgreSQL `current_tenant_id()` Funktion liest diesen Config-Wert
3. RLS Policies auf jeder Tabelle filtern automatisch: `tenant_id = current_tenant_id()`
4. `BaseEntity` hat `tenant_id` Feld mit `@PrePersist` das aus `TenantContext` (ThreadLocal) setzt
5. Controller verwenden `findByIdSecure()` für Defense-in-Depth (403 statt 404)

**Ausnahmen (kein RLS):** TenantEntity, UserEntity (eigene Logik), RefreshTokenBlacklistEntity

**SYSTEM_ADMIN:** Hat keinen Tenant, bypassed RLS. Verwendet `withTenantContext(UUID, Supplier)` Wrapper für Tenant-spezifische Operationen.

**WICHTIG:** `TenantContext` ist ein ThreadLocal und wird NICHT automatisch in Virtual Threads propagiert. Bei Virtual Threads (CeoAgent-Delegationen) muss der Tenant manuell captured, gesetzt und cleared werden.

---

## 4. Domänen-Module

9 Module, pro Tenant togglebar (außer `core`):

| Modul | Package | Lead-Agent | Deaktivierbar |
|-------|---------|------------|---------------|
| `core` | auth, tenant, agentinfra, events, documents | CEO | Nein |
| `production` | production | Production Lead | Ja |
| `machines` | machines | Machine Lead | Ja |
| `inventory` | inventory | Supply Lead | Ja |
| `people` | people | People Lead | Ja |
| `customers` | customers | — | Ja |
| `inbox` | inbox | Support Lead | Ja |
| `bom` | bom | — | Ja |
| `knowledge` | knowledge | — | Ja |

**Deaktivierung bewirkt:**
- Kein Sidebar-Eintrag im Frontend
- Kein API-Zugriff (403 via `ModuleAccessInterceptor`)
- Keine Agent-Tools (gefiltert in `AgentToolRegistry`)
- Daten bleiben intakt

---

## 5. Agent-System – Übersicht

Das Agent-System folgt einer strikten 3-Ebenen-Hierarchie:

```
         ┌─────────┐
         │   CEO   │  (1 Instanz pro Tenant)
         │  Opus   │  Streaming, Delegation
         └────┬────┘
              │ delegate_to_lead
    ┌─────────┼─────────┬─────────┬─────────┐
    ▼         ▼         ▼         ▼         ▼
┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐
│Prod.   ││Machine ││Supply  ││People  ││Support │
│Lead    ││Lead    ││Lead    ││Lead    ││Lead    │
│Sonnet  ││Sonnet  ││Sonnet  ││Sonnet  ││Sonnet  │
└───┬────┘└────────┘└────────┘└────────┘└────────┘
    │ spawn_sub_agent
    ▼
┌────────┐
│Sub-    │  (Ephemeral, TTL 1h)
│Agent   │  Tool-Subset des Parents
│Sonnet  │
└────────┘
```

**Schlüsselkonzepte:**
- Agenten sind **echte Java-Objekte** (nicht nur DB-Einträge)
- Sie werden **pro Request** aus der DB gebaut und nach Verwendung verworfen
- Die DB speichert Templates, Instanzen, Runs und Steps
- Die Java-Objekte (Agent sealed interface) führen die ReAct-Loops aus
- Jeder Agent hat eine Identität, Capabilities und Tools

---

## 6. Agent-System – OOP-Architektur im Detail

### 6.1 Sealed Interface: `Agent`

```
backend/src/main/java/com/owlsburg/ops/agentinfra/runtime/Agent.java
```

```java
public sealed interface Agent permits CeoAgent, LeadAgent, SubAgent {
    AgentIdentity identity();
    AgentCapabilities capabilities();
    AgentResult execute(AgentContext context, String task);
    default void executeStreaming(AgentContext context, String task, SseEmitter emitter) {
        throw new UnsupportedOperationException("Streaming not supported");
    }
}
```

Das `sealed` Keyword beschränkt die Implementierungen auf genau 3 Klassen. Compile-Time Sicherheit: Kein anderer Code kann eine vierte Agent-Klasse erstellen.

### 6.2 Records (Immutable Value Objects)

Alle Daten, die ein Agent zur Laufzeit braucht, sind in **Records** gekapselt:

#### AgentIdentity – "Wer bin ich?"

```java
public record AgentIdentity(
    UUID instanceId,              // DB-Instanz-ID
    UUID templateId,              // Template-Vorlage
    UUID tenantId,                // Tenant-Zugehörigkeit
    String name,                  // "CEO", "Production Lead"
    String role,                  // "ceo", "production_lead"
    AgentInstanceType type,       // PERSISTENT oder EPHEMERAL
    UUID parentInstanceId,        // Parent (null für CEO)
    String systemPrompt,          // Vollständiger System-Prompt (mit Memory)
    String model,                 // "claude-opus-4-6"
    List<String> allowedToolNames // Immutable Tool-Whitelist
)
```

#### AgentCapabilities – "Was kann ich?"

```java
public record AgentCapabilities(
    List<AgentTool> tools,                    // Instanziierte Tool-Objekte
    List<LlmToolDefinition> toolDefinitions,  // JSON-Schema für LLM
    boolean canDelegate,                      // Nur CEO (true)
    boolean canSpawnSubAgents,                // Leads (true)
    boolean canCommunicatePeers,              // Leads (true)
    int maxIterations,                        // ReAct-Loop-Limit (CEO=10, Lead=5)
    int maxTokensPerRun,                      // Token-Budget pro Ausführung
    Double temperature                        // LLM Sampling (nullable)
)
```

#### AgentContext – "In welchem Kontext arbeite ich?"

```java
public record AgentContext(
    UUID invocationId,           // Unique pro Ausführung
    String tenantId,             // Für RLS
    UUID userId,                 // Wer hat den Agent aufgerufen
    UUID chatSessionId,          // Chat-Session (null bei Scheduler)
    int delegationDepth,         // Verschachtelungstiefe (CEO=0, Lead=1)
    Instant startedAt,           // Start-Zeitpunkt
    AgentActivityBus activityBus,// SSE Event Publisher (nullable)
    RunMemory runMemory          // Ephemerer Notizblock für diesen Run
) {
    public static AgentContext forChat(String tenantId, UUID userId,
                                       UUID sessionId, AgentActivityBus bus) { ... }
    public AgentContext withDelegationDepth(int depth) { ... }
}
```

#### AgentResult – "Was habe ich erreicht?"

```java
public record AgentResult(
    String output,              // Finale Antwort
    int inputTokens,            // Verbrauchte Input-Tokens
    int outputTokens,           // Generierte Output-Tokens
    List<String> toolsUsed,     // Ausgeführte Tools
    String status,              // "completed", "max_iterations", "error"
    List<LeadStep> steps        // Schritt-für-Schritt-Reasoning (Lead/Sub)
) {
    static AgentResult completed(...) { ... }
    static AgentResult maxIterations(...) { ... }
    static AgentResult error(String message) { ... }
}
```

### 6.3 Die drei Agent-Implementierungen

#### CeoAgent – Streaming-Spezialist (567 Zeilen)

```
backend/src/main/java/com/owlsburg/ops/agentinfra/runtime/CeoAgent.java
```

**Konstruktor-Parameter:**
```java
public CeoAgent(
    AgentIdentity identity,
    AgentCapabilities capabilities,
    AgentToolRegistry toolRegistry,
    String apiKey,              // Tenant-spezifischer Anthropic API-Key
    ObjectMapper objectMapper
)
```

**Besonderheiten:**
- Verwendet die **Anthropic Streaming API direkt** (raw HTTP, kein SDK)
- Statischer `HttpClient` (shared, connection pooling)
- Parallelisiert Delegationen über **Virtual Threads**
- Parst Tokens inline aus dem SSE-Stream (`message_start`, `message_delta`)
- Erkennt Client-Disconnects und bricht den Loop ab (`trySend()`)
- Retry-Logik mit Exponential Backoff (1s, 3s, 8s) bei 429/529/5xx

**Records im CeoAgent:**
```java
record ToolCallLog(String toolName, String input, String result, boolean success)
record ChatResult(String response, int inputTokens, int outputTokens, List<ToolCallLog> toolCallLogs)
```

**Kern-Methode: `executeStreamingInternal()`** – Der ReAct-Loop:

```
FÜR iteration 0 bis maxIterations:
  1. RunMemory-Summary in System-Prompt injizieren (wenn nicht leer)
  2. THINKING-Event publizieren

  3. Anthropic Streaming API Call:
     - Token-Counts parsen (message_start.usage, message_delta.usage)
     - Text-Tokens puffern (content_block_delta.text_delta)
     - Tool-Use-Blöcke akkumulieren (content_block_start/delta/stop)
     - stop_reason tracken (tool_use, end_turn, client_disconnected)

  4. Wenn Client disconnected (trySend() == false):
     - IDLE publizieren, Loop abbrechen

  5. Wenn stop_reason != tool_use ODER keine Tools:
     - IDLE publizieren, Loop abbrechen (Agent fertig)

  6. Tools partitionieren:
     - delegate_to_lead → parallel (Virtual Threads)
     - reguläre Tools → sequentiell

  7. PHASE 1: Sequentielle Tool-Ausführung
     - toolCall SSE-Event senden
     - TOOL_CALL Activity publizieren
     - Tool ausführen über Registry
     - toolResult SSE-Event senden
     - TOOL_RESULT Activity publizieren

  8. PHASE 2: Parallele Delegationen
     - delegation SSE-Events senden (status: "running")
     - Virtual Thread pro Delegation spawnen
     - TenantContext manuell capturen + setzen + clearen
     - LeadAgent ausführen
     - leadStep SSE-Events emittieren (pro Schritt)
     - delegationResult SSE-Event emittieren
     - Max 90s Timeout pro Thread, dann Interrupt

  9. tool_result Message bauen (role=user) mit allen Ergebnissen
  10. Loop fortsetzen
```

**TenantContext-Propagation in Virtual Threads:**
```java
String capturedTenantId = context.tenantId();  // Capturen VOR Thread-Start
Thread t = Thread.startVirtualThread(() -> {
    TenantContext.setCurrentTenant(capturedTenantId);  // Manuell setzen
    try {
        // ... LeadAgent ausführen ...
    } finally {
        TenantContext.clear();  // Aufräumen!
    }
});
```

#### LeadAgent – Synchroner Spezialist (139 Zeilen)

```
backend/src/main/java/com/owlsburg/ops/agentinfra/runtime/LeadAgent.java
```

**Konstruktor-Parameter:**
```java
public LeadAgent(
    AgentIdentity identity,
    AgentCapabilities capabilities,
    LlmProvider llmProvider,     // Abstraktion (nicht raw HTTP)
    String apiKey,
    AgentToolRegistry toolRegistry,
    ObjectMapper objectMapper
)
```

**Besonderheiten:**
- Kein Streaming – verwendet `LlmProvider.chat()` (synchron, blocking)
- Kein Virtual Threading – alles im selben Thread
- Captures Steps (reasoning, tool_call, tool_result) für Transparenz
- Kann Sub-Agents spawnen, aber NICHT weiter delegieren

**ReAct-Loop:**
```
FÜR i = 0 bis maxIterations:
  1. RunMemory-Summary in System-Prompt injizieren
  2. LlmProvider.chat() aufrufen (blocking)
  3. Wenn Text-Content → LeadStep("reasoning", null, content, i) capturen
  4. Wenn toolUse vorhanden:
     - LeadStep("tool_call", toolName, input, i)
     - TOOL_CALL Activity publizieren
     - Tool synchron ausführen
     - LeadStep("tool_result", toolName, result, i)
     - TOOL_RESULT Activity publizieren
     - Loop fortsetzen
  5. Wenn kein toolUse → AgentResult.completed(..., steps) zurückgeben
```

#### SubAgent – Ephemerer Spezialist (108 Zeilen)

```
backend/src/main/java/com/owlsburg/ops/agentinfra/runtime/SubAgent.java
```

**Identisch mit LeadAgent, aber:**
- Kein ActivityBus (keine Events publiziert)
- Kein RunMemory im Konstruktor
- Kürzere maxIterations (3 statt 5)
- Tool-Subset des Parent-Agents
- Ephemeral: Wird nach Verwendung terminiert
- Memory-Promotion: Wichtige Erinnerungen (importance ≥ 7) werden zum Parent übertragen

### 6.4 AgentFactory – Der Builder

```
backend/src/main/java/com/owlsburg/ops/agentinfra/runtime/AgentFactory.java (307 Zeilen)
```

Die Factory ist der einzige Ort, an dem Agent-Objekte erstellt werden.

**`createAgent(UUID instanceId, String tenantId)` → Agent**

```
1. AgentInstanceEntity aus DB laden (mit Tenant-Check)
2. AgentTemplateEntity laden
3. buildAgent() aufrufen
```

**`buildAgent(instance, template, tenantId)` – Kern-Logik:**

```
1. System-Prompt auflösen:
   - instance.customSystemPrompt (Override) ODER template.basePrompt (Fallback)
   - {{TENANT_NAME}} Placeholder ersetzen
   - Memory-Sektion injizieren (via AgentMemoryService.buildMemorySection)

2. Model auflösen:
   - instance.config JSONB → "model" Feld
   - Fallback: LlmConfigService.defaultModel
   - Letzter Fallback: "claude-sonnet-4-6"

3. maxTokensPerRun auflösen:
   - instance.config JSONB ODER template.maxTokensPerRun

4. Erlaubte Tools auflösen:
   - instance.allowedToolsOverride (JSONB, nullable) ODER
   - template.allowedTools (JSONB)
   - Filtern nach aktivierten Modulen (moduleService)

5. Tool-Definitionen bauen (LlmToolDefinition Liste für Anthropic API)

6. Temperature aus LlmConfigService auflösen

7. Agent erstellen basierend auf Rolle:
   - "ceo" → new CeoAgent(identity, capabilities, toolRegistry, apiKey, objectMapper)
   - "*_lead" → new LeadAgent(identity, capabilities, llmProvider, apiKey, toolRegistry, objectMapper)
   - Default → LeadAgent
```

**`spawnSubAgent(parent, task, toolSubset, name, tenantId)` → SubAgent:**

```
1. Ephemere AgentInstanceEntity erstellen (type=EPHEMERAL, status=ACTIVE)
2. Tool-Subset validieren (muss Subset der Parent-Tools sein)
3. Minimalen System-Prompt bauen
4. SubAgent-Objekt erstellen (keine Ausführung, nur Objekt)
```

### 6.5 Zusammenspiel: DB-Entitäten vs. Java-Objekte

```
┌────────────────────────────┐     ┌──────────────────────────┐
│     DATENBANK (Persistent) │     │    JAVA-OBJEKTE (Runtime) │
│                            │     │                          │
│  AgentTemplateEntity       │     │                          │
│  ├── name, role            │     │                          │
│  ├── basePrompt            │────▶│  AgentIdentity (Record)  │
│  ├── allowedTools [JSON]   │     │  ├── name, role          │
│  ├── maxTokensPerRun       │     │  ├── systemPrompt        │
│  └── dailyTokenBudget      │     │  ├── model               │
│                            │     │  └── allowedToolNames    │
│  AgentInstanceEntity       │     │                          │
│  ├── templateId            │     │  AgentCapabilities (Rec) │
│  ├── config {model}        │────▶│  ├── tools [Objekte]     │
│  ├── customSystemPrompt    │     │  ├── toolDefinitions     │
│  ├── allowedToolsOverride  │     │  ├── canDelegate         │
│  ├── activityStatus        │     │  ├── maxIterations       │
│  └── lastRunId             │     │  └── temperature         │
│                            │     │                          │
│  AgentRunEntity            │     │  CeoAgent / LeadAgent /  │
│  ├── status                │◀────│  SubAgent                │
│  ├── tokensUsed            │     │  (sealed interface impl) │
│  ├── costUsd               │     │  ├── identity()          │
│  └── output                │     │  ├── capabilities()      │
│                            │     │  └── execute(ctx, task)  │
│  AgentRunStepEntity        │     │                          │
│  ├── type (LLM/TOOL)       │     │  AgentContext (Record)   │
│  ├── toolName              │     │  ├── tenantId            │
│  ├── input/output          │     │  ├── userId              │
│  └── tokensUsed            │     │  ├── chatSessionId       │
│                            │     │  └── runMemory           │
│  AgentMemoryEntity         │     │                          │
│  ├── type, category, key   │────▶│  → injiziert in Prompt   │
│  ├── value, importance     │     │                          │
│  └── lastAccessedAt        │     │  AgentResult (Record)    │
│                            │     │  ├── output              │
│  AgentMessageEntity        │     │  ├── tokens              │
│  ├── sender/target         │     │  ├── toolsUsed           │
│  ├── messageType           │     │  └── steps               │
│  └── body                  │     │                          │
└────────────────────────────┘     └──────────────────────────┘
```

**Lebenszyklus eines Agent-Objekts:**
1. HTTP-Request kommt rein (Chat oder Scheduler)
2. `AgentFactory.createAgent()` baut aus DB-Daten ein Agent-Objekt
3. Agent-Objekt führt ReAct-Loop aus (1–10 Iterationen)
4. Ergebnis wird in DB geschrieben (AgentRun, Steps, Messages)
5. Agent-Objekt wird verworfen (Garbage Collected)
6. Nächster Request → neues Agent-Objekt (frischer State aus DB)

---

## 7. Agent-Lifecycle & Execution

### 7.1 Persistenter Lifecycle (DB)

```
AgentInstanceEntity Status-Maschine:

    INACTIVE ──────▶ ACTIVE
       ▲                │
       │                ├──▶ INACTIVE
       │                ├──▶ QUARANTINE ──▶ ACTIVE
       │                │                  │
       │                └──▶ TERMINATED    └──▶ TERMINATED
       │                     (terminal)         (terminal)
```

```
AgentActivityStatus (Runtime):

    IDLE ──▶ BUSY ──▶ IDLE     (Erfolgreicher Run)
                  └──▶ ERROR   (Fehler im Run)
```

```
AgentRunEntity Status-Maschine:

    PENDING ──▶ RUNNING ──▶ SUCCESS
                        └──▶ FAILED
                        └──▶ CANCELLED
```

### 7.2 Zwei Execution-Pfade

**Pfad A: Chat (SimpleChatService)** – Streaming, User-getrieben

```
HTTP POST /api/chat/message → SseEmitter
  │
  ├─ Session auflösen/erstellen
  ├─ AgentInstance laden (Tenant-Check)
  ├─ User-Message in DB speichern
  ├─ sessionId SSE-Event senden
  ├─ AgentFactory.createAgent() → Agent-Objekt
  ├─ Chat-History aus DB laden
  ├─ AgentRun erstellen (TriggerType.CHAT)
  ├─ Activity → BUSY
  │
  ├─ CeoAgent.getLastResponse(context, messages, emitter)
  │   └─ ReAct-Loop mit Streaming (siehe §6.3)
  │
  ├─ AgentRun abschließen (Tokens, Kosten)
  ├─ Activity → IDLE, Last-Run verlinken
  ├─ Assistant-Message in DB speichern
  ├─ Usage SSE-Event senden
  ├─ Session-Titel generieren (erste Nachricht)
  └─ Done SSE-Event senden
```

**Pfad B: Scheduler/Event (AgentExecutionService)** – Nicht-Streaming, System-getrieben

```
@Scheduled oder DomainEvent → AgentRunOrchestrator.triggerRun()
  │
  ├─ AgentRun erstellen (TriggerType.SCHEDULE/EVENT)
  ├─ Activity → BUSY
  ├─ Budget prüfen
  ├─ Tools aus Registry laden
  ├─ System-Prompt bauen (SystemPromptBuilder)
  │
  ├─ ReAct-Loop (bis 15 Iterationen):
  │   ├─ LLM-Call → Step loggen
  │   ├─ Tool-Call → Step loggen
  │   └─ Delegation → Untergeordneten Agent ausführen
  │
  ├─ Kosten berechnen
  ├─ AgentRun abschließen
  ├─ Activity → IDLE
  └─ Last-Run verlinken
```

### 7.3 Fehlerbehandlung & Guarantees

`SimpleChatService` hat ein robustes Fehlerbehandlungs-System:

```java
try {
    // ... Haupt-Execution ...
} catch (LlmProviderException e) {
    // LLM-Fehler: Run als FAILED markieren, Incident melden
    failRunSafe(agentRunId, e.getMessage());
    safeUpdateActivity(instanceId, AgentActivityStatus.ERROR);
    reportIncidentSafe(instanceId, "LLM_ERROR", e.getMessage());
    sendErrorAndComplete(emitter, e.getMessage());
} catch (Exception e) {
    // Generischer Fehler: Incident melden
    failRunSafe(agentRunId, e.getMessage());
    safeUpdateActivity(instanceId, AgentActivityStatus.ERROR);
    reportIncidentSafe(instanceId, "RUNTIME_ERROR", e.getMessage());
    sendErrorAndComplete(emitter, "Interner Fehler");
} finally {
    // GARANTIE 1: Verwaiste Runs aufräumen
    finalizeRunSafe(agentRunId);  // PENDING/RUNNING → FAILED
    // GARANTIE 2: BUSY-Status zurücksetzen
    resetBusyIfNeeded(instanceId);  // BUSY → IDLE (z.B. bei SSE-Timeout)
}
```

Alle `safe*` Methoden sind in try-catch gewrapped und loggen nur auf debug-Level.

---

## 8. Tool-System

### 8.1 Interface & Contracts

```java
public interface AgentTool {
    String getName();                    // z.B. "get_jobs"
    String getDescription();             // Beschreibung für LLM (deutsch)
    String getInputSchema();             // JSON Schema für Parameter
    ToolPermission getPermission();      // READ_ONLY, WRITE_WITH_APPROVAL, CRITICAL_WRITE
    ToolResult execute(ToolExecutionContext context, String input);
    default String getModuleId() { return "core"; }  // Modul-Zugehörigkeit
}
```

```java
public record ToolExecutionContext(
    String tenantId,
    UUID instanceId,        // Ausführender Agent
    UUID runId,             // Aktueller Run
    AgentActivityBus activityBus,
    RunMemory runMemory     // Ephemerer Notizblock
)
```

```java
public record ToolResult(
    boolean success,
    String data,            // Ergebnis (JSON oder Text)
    String errorMessage     // Nur bei Fehler
) {
    static ToolResult success(String data) { ... }
    static ToolResult error(String message) { ... }
}
```

### 8.2 Tool Registry

```java
@Component
public class AgentToolRegistry {
    private final Map<String, AgentTool> toolMap;  // Name → Tool

    // Alle Tools für eine Instanz (Template-Filter + Modul-Filter)
    List<AgentTool> getToolsForInstance(AgentTemplateEntity template, UUID tenantId)

    // Tools nach Namen (Instance-Override + Modul-Filter)
    List<AgentTool> getToolsByNames(List<String> names, UUID tenantId)

    // Lookup
    Optional<AgentTool> getTool(String name)
    Collection<AgentTool> getAllTools()
    List<String> getAllToolNames()
}
```

Modul-Filterung: Wenn ein Tool `getModuleId() = "production"` hat und das Production-Modul für den Tenant deaktiviert ist, wird das Tool aus der Liste entfernt.

### 8.3 Vollständige Tool-Liste (40 Tools)

| Domäne | Tool | Permission | Beschreibung |
|--------|------|-----------|--------------|
| **CEO/Orchestration** | | | |
| core | `delegate_to_lead` | READ_ONLY | Aufgabe an Lead-Agent delegieren |
| core | `get_kpi_summary` | READ_ONLY | Zusammenfassung aller KPIs |
| **Production** | | | |
| production | `get_jobs` | READ_ONLY | Jobs filtern (Status, überfällig) |
| production | `get_job_detail` | READ_ONLY | Job-Details mit Status-Historie |
| production | `update_job_status` | WRITE | Job-Status ändern |
| production | `list_jobs` | READ_ONLY | Alle Jobs auflisten |
| production | `get_production_kpis` | READ_ONLY | Produktions-KPIs |
| production | `list_stations` | READ_ONLY | Stationen mit Kapazität |
| **Machines** | | | |
| machines | `get_machines` | READ_ONLY | Maschinenübersicht |
| machines | `get_machine_detail` | READ_ONLY | Maschine + Incidents + Wartung |
| machines | `get_maintenance_due` | READ_ONLY | Fällige Wartungen |
| machines | `schedule_maintenance` | WRITE | Wartung planen |
| machines | `report_incident` | WRITE | Störung melden |
| machines | `get_machine_overview` | READ_ONLY | Maschinenübersicht (Alias) |
| machines | `get_capacity` | READ_ONLY | Kapazitäts-Metriken |
| **Inventory** | | | |
| inventory | `get_critical_stock` | READ_ONLY | Kritischer Bestand (unter Minimum) |
| inventory | `get_stock_level` | READ_ONLY | Aktueller Bestand eines Artikels |
| inventory | `get_stock_movements` | READ_ONLY | Lagerbewegungen |
| inventory | `get_article_detail` | READ_ONLY | Artikel-Details |
| inventory | `list_critical_stock` | READ_ONLY | Kritischer Bestand (Liste) |
| inventory | `create_reorder` | WRITE | Nachbestellung erstellen |
| **People** | | | |
| people | `get_attendance_today` | READ_ONLY | Heutige Anwesenheit |
| people | `get_absences` | READ_ONLY | Abwesenheitsanträge |
| people | `get_employee_detail` | READ_ONLY | Mitarbeiter-Details |
| people | `list_employees` | READ_ONLY | Alle Mitarbeiter |
| people | `approve_absence` | WRITE | Abwesenheit genehmigen/ablehnen |
| **Support/Inbox** | | | |
| inbox | `list_conversations` | READ_ONLY | Offene Konversationen |
| inbox | `get_conversation_detail` | READ_ONLY | Konversations-Thread |
| inbox | `get_inbox_messages` | READ_ONLY | Posteingangs-Nachrichten |
| inbox | `create_inbox_reply` | WRITE | Antwort erstellen |
| **Customers** | | | |
| customers | `get_customer_orders` | READ_ONLY | Kundenaufträge |
| **Agent-Infrastruktur** | | | |
| core | `spawn_sub_agent` | WRITE | Sub-Agent erstellen (nur Leads) |
| core | `save_memory` | WRITE | Erinnerung speichern |
| core | `recall_memory` | READ_ONLY | Erinnerungen abrufen |
| core | `read_agent_memory` | READ_ONLY | Erinnerungen anderer Agents lesen |
| core | `send_message` | WRITE | Nachricht an anderen Agent |
| core | `report_to_ceo` | WRITE | Bericht an CEO eskalieren |
| core | `check_messages` | READ_ONLY | Ungelesene Nachrichten prüfen |
| core | `get_my_day` | READ_ONLY | Persönliches Dashboard |

### 8.4 Tool-Zuweisungen pro Agent

| Agent | Tools |
|-------|-------|
| **CEO** | delegate_to_lead, get_kpi_summary, check_messages, save_memory, recall_memory, read_agent_memory |
| **Production Lead** | get_jobs, get_job_detail, update_job_status, list_stations, get_production_kpis, send_message, report_to_ceo, check_messages, save_memory, recall_memory, spawn_sub_agent |
| **Machine Lead** | get_machines, get_machine_detail, get_maintenance_due, report_incident, schedule_maintenance, send_message, report_to_ceo, check_messages, save_memory, recall_memory |
| **Supply Lead** | get_critical_stock, get_stock_level, get_stock_movements, create_reorder, send_message, report_to_ceo, check_messages, save_memory, recall_memory |
| **People Lead** | get_attendance_today, get_absences, get_employee_detail, approve_absence, send_message, report_to_ceo, check_messages, save_memory, recall_memory |
| **Support Lead** | get_customer_orders, list_conversations, get_conversation_detail, get_inbox_messages, create_inbox_reply, send_message, report_to_ceo, check_messages, save_memory, recall_memory |

---

## 9. LLM-Integration

### 9.1 Provider-Abstraktion

```java
public interface LlmProvider {
    LlmResponse chat(LlmRequest request, String apiKey);
    List<String> listModels(String apiKey);
    String getProviderName();
}
```

```java
public record LlmRequest(
    String systemPrompt,
    List<LlmMessage> messages,
    List<LlmToolDefinition> tools,
    String model,           // z.B. "claude-opus-4-6"
    int maxTokens,
    Double temperature      // nullable
)

public record LlmResponse(
    String content,         // Text-Output
    LlmToolUse toolUse,    // Tool-Call (wenn vorhanden)
    String stopReason,      // "end_turn", "tool_use", "max_tokens"
    int inputTokens,
    int outputTokens,
    String model
)
```

### 9.2 Anthropic-Provider

```
backend/src/main/java/com/owlsburg/ops/agentinfra/llm/AnthropicLlmProvider.java
```

- API-Endpoint: `https://api.anthropic.com/v1/messages` (v2023-06-01)
- Retry: 3 Versuche bei 429/5xx mit Exponential Backoff (1s, 3s, 8s)
- Model-Caching: 1h TTL Cache der verfügbaren Modelle pro API-Key
- HTTP-Client: Standard Java `HttpClient` (Connection Pooling)

### 9.3 Zwei Kommunikationswege zum LLM

1. **CeoAgent → Anthropic Streaming API (direkt, raw HTTP)**
   - Parsed SSE-Events in Echtzeit
   - Token-Tracking inline
   - Verbindung bleibt offen für chunked Responses

2. **LeadAgent/SubAgent → LlmProvider.chat() (abstrakt, blocking)**
   - Verwendet die Provider-Abstraktion
   - Wartet auf vollständige Antwort
   - Einfacher, aber kein Streaming

### 9.4 Tenant-spezifische Konfiguration

Jeder Tenant hat eine `TenantLlmConfigEntity`:
- `provider`: "anthropic" (aktuell einziger)
- `apiKey`: AES-256-GCM verschlüsselter API-Key
- `defaultModel`: Standard-Modell für den Tenant
- `settings` (JSONB): `{ "temperature": 0.7, "maxTokensDefault": 4096 }`

### 9.5 Kosten-Berechnung

```java
private BigDecimal calculateCost(String model, int inputTokens, int outputTokens) {
    if (model.contains("opus")) {
        return inputTokens * 15.0/1M + outputTokens * 75.0/1M;
    } else if (model.contains("haiku")) {
        return inputTokens * 0.80/1M + outputTokens * 4.0/1M;
    }
    // Sonnet (default)
    return inputTokens * 3.0/1M + outputTokens * 15.0/1M;
}
```

---

## 10. Memory-System

### 10.1 Zwei Ebenen

**RunMemory (ephemer, in-memory):**
```java
public final class RunMemory {
    private final ConcurrentHashMap<String, String> entries;
    void put(String key, String value);
    String get(String key);
    String buildSummary();  // → "## Arbeitsnotizen\n- key: value\n..."
}
```
- Thread-safe (ConcurrentHashMap)
- Lebt nur während eines `AgentContext`
- Wird in System-Prompt injiziert wenn nicht leer
- Für temporäre Notizen während eines Runs

**AgentMemoryEntity (persistent, DB):**
- Tabelle: `agent_memories` (mit RLS)
- Pro Agent bis zu 200 Einträge (LRU-Eviction)
- 9 Memory-Typen mit unterschiedlichen Expiry-Regeln

### 10.2 Memory-Typen

| Kategorie | Typen | Expiry | Beispiel |
|-----------|-------|--------|----------|
| Semantisch | FACT, PREFERENCE, RULE | 180 Tage (wenn importance < 8), sonst nie | "CNC-3 hat 2mm Toleranz" |
| Prozedural | LEARNING, STRATEGY, WORKFLOW | 180 Tage (wenn importance < 8) | "Bei Lager-Engpass: Lieferant B bevorzugen" |
| Episodisch | DECISION, EVENT | 90 Tage | "Job 1234 wurde wegen Materialengpass pausiert" |
| Allgemein | NOTE | Nie | Notizen |

### 10.3 Memory-Injection in System-Prompt

`AgentMemoryService.buildMemorySection(instanceId)`:
```
## Dein Gedächtnis

### Wissen (Fakten & Regeln)
- [FACT/production] cnc_tolerance: CNC-3 hat 2mm Toleranz (Wichtigkeit: 9)
- [RULE/general] escalation_policy: Bei > 3 Ausfällen CEO informieren (Wichtigkeit: 8)
...

### Erfahrungen (Gelerntes)
- [STRATEGY/inventory] supplier_fallback: Bei Lieferant A Verzögerung → Lieferant B (Wichtigkeit: 7)
...

### Letzte Ereignisse
- [EVENT/production] shift_change_issue: Schichtwechsel am 10.03. hatte Kommunikationsproblem (Wichtigkeit: 6)
...
```

Maximal 10 semantische + 5 prozedurale + 5 episodische Erinnerungen (nach Wichtigkeit sortiert).

### 10.4 Episodic Memory Extraction

`EpisodicMemoryExtractor` – **deterministisch, kein LLM**:
- Wird nach CeoAgent-Runs aufgerufen
- Extrahiert aus ToolCallLogs:
  - Status-Änderungen (update_job_status, approve_absence, etc.)
  - Delegationen (delegate_to_lead → Lead-Name + Task-Summary)
  - Bestellungen (create_reorder)
- Speichert als Episodic Memory (source="system")

### 10.5 Tool-Outcome-Tracking

`AgentMemoryService.recordToolOutcome(instanceId, toolName, success)`:
- Erstellt/aktualisiert STRATEGY-Memory: `tool_stats:{toolName}`
- Metadata JSONB: `{ "total_calls": N, "successful": M, "success_rate": M/N }`
- Agenten lernen über Zeit, welche Tools zuverlässig funktionieren

### 10.6 Memory-Promotion (Sub-Agent → Parent)

Wenn ein Sub-Agent terminiert wird:
1. Memories mit importance ≥ 7 werden zum Parent-Agent kopiert
2. Restliche Memories werden gelöscht
3. Sub-Agent Instance wird TERMINATED
4. Wissen geht nicht verloren, auch wenn der Sub-Agent nur temporär war

---

## 11. Agent-Messaging

### 11.1 Message Bus

```java
@Service
public class AgentMessageBus {
    void send(UUID senderId, UUID targetId, String type, String subject, String body, String priority);
    List<AgentMessageEntity> getUnreadMessages(UUID targetId);
    void markAsRead(UUID messageId);
}
```

### 11.2 Hierarchie-Enforcement

```
CEO ↔ Jeder         (immer erlaubt)
Lead ↔ Lead          (Peer-Kommunikation erlaubt)
Lead → eigene Subs   (erlaubt)
Sub → eigener Parent  (erlaubt)
Sub ↔ Geschwister    (gleicher Parent, erlaubt)
```

Der Bus validiert die Hierarchie über `resolveLevel()`:
- Kein `parentInstanceId` → CEO
- Parent ist CEO → LEAD
- Anderer Parent → SUB_AGENT

### 11.3 Message-Tools

- `send_message`: Nachricht an Agent per Name senden
- `report_to_ceo`: Eskalation/Report an CEO
- `check_messages`: Ungelesene Nachrichten abrufen

---

## 12. Chat & SSE-Streaming

### 12.1 SSE-Event-Typen (Backend → Frontend)

```json
// 1. Session-ID (erstes Event)
{ "sessionId": "uuid" }

// 2. Text-Token (gestreamt)
{ "token": "Hallo, " }

// 3. Tool-Call Start
{ "toolCall": { "name": "get_jobs", "input": "{\"status\":\"IN_PRODUCTION\"}" } }

// 4. Tool-Call Ergebnis
{ "toolResult": { "name": "get_jobs", "result": "[{...}]" } }

// 5. Delegation Start
{ "delegation": { "lead": "produktions_lead", "task": "...", "status": "running", "id": "uuid" } }

// 6. Lead-Agent Schritt
{ "leadStep": { "lead": "produktions_lead", "type": "reasoning", "content": "...", "iteration": 0, "id": "uuid" } }

// 7. Delegation Ergebnis
{ "delegationResult": { "lead": "produktions_lead", "result": "...", "id": "uuid" } }

// 8. Token-Verbrauch
{ "usage": { "inputTokens": 1234, "outputTokens": 567 } }

// 9. Fertig
{ "done": true }

// 10. Fehler
{ "error": "Fehlermeldung" }
```

### 12.2 Chat-Persistenz

**Tabellen:**
- `chat_sessions`: id, userId, agentInstanceId, title (auto-generiert aus erster Nachricht)
- `chat_messages`: id, sessionId, role ("user"/"assistant"), content, createdAt

**Ablauf:**
1. Neue Session → Greeting speichern ("Guten Tag! Ich bin Ihr CEO Agent...")
2. User-Message → speichern vor LLM-Call
3. Assistant-Response → speichern nach LLM-Call
4. Vollständige History wird bei jedem Request aus DB geladen
5. Session-Titel = erste 50 Zeichen der ersten User-Message

---

## 13. Scheduling & Cleanup

### 13.1 SubAgentCleanupScheduler

- **Intervall:** Alle 30 Minuten
- **Logik:** EPHEMERAL Agents > 1h alt → Memory-Promotion + TERMINATED

### 13.2 MemoryPruningScheduler

- **Intervall:** Täglich 3:00 Uhr
- **Logik:** Abgelaufene Memories löschen (basierend auf `expiresAt`)

### 13.3 Scheduled Triggers

- CEO: Täglich 6:00 Uhr
- Production Lead: Täglich 22:00 Uhr
- Supply Lead: Täglich 7:00 Uhr
- Ausführung über `AgentRunOrchestrator.triggerRun()` (async, ThreadPoolExecutor)

---

## 14. Frontend-Architektur

### 14.1 Agent-Panel (Chat-UI)

```
frontend/components/layout/agent-panel.tsx (380px Sidebar)
```

**Features:**
- SSE-Streaming mit Token-Tracking (pro Nachricht + Session-Total)
- Session-Management (Liste, Wechsel, Erstellen, Löschen)
- Last-Run-Anzeige (Steps mit Tokens/Dauer)
- Tool-Call-Visualisierung (Inline Info-Cards)
- Delegation-Tracking (ID-Matching für Lead-Agent-Steps)
- Lead-Step-Expansion (Reasoning, Tool-Call, Tool-Result)
- Markdown-Rendering (react-markdown + remark-gfm + rehype-highlight)

### 14.2 Agent-Hierarchie-View

```
frontend/components/agents/hierarchy-view.tsx
frontend/components/agents/hierarchy-canvas.tsx
frontend/components/agents/agent-node-card.tsx
frontend/components/agents/agent-monitor-panel.tsx
```

**Visualisierung:**
- 3 Ebenen: CEO (oben) → Leads (Mitte) → Sub-Agents (unten)
- SVG-Verbindungslinien (Parent ↔ Child)
- Aktive Delegationen: Primary Color, 4px, gestrichelt, animiert
- Inaktive Verbindungen: Gedämpft, 2.5px, 25% Opacity
- Klick auf Card → AgentMonitorPanel (420px rechts)
- Real-time Activity via SSE (`/api/agent-activity/stream`)

### 14.3 Agent-Monitor Panel

**Sektionen:**
1. Header: Name, Status-Badge, Model
2. KPI-Streifen: Token-Budget-Nutzung, letzte Aktivität
3. Info: Parent, Rolle, Typ, Live-Activity
4. Thought-Log: Live-Events + DB-Steps (Live hat Priorität)

### 14.4 Agent-Settings

**Normale Settings (`/settings`):**
- Rollen → Agent-Zuweisungen (ADMIN→CEO, WORKER→People Lead, etc.)
- Erweiterter Agents-Tab: Model-Dropdown, System-Prompt-Editor, Tool-Whitelist

**System-Admin Settings (`/system/companies/[id]`):**
- Agents-Tab: Status-Toggle (An/Aus), Model, Prompt-Dialog, Tools, Budget
- LLM-Tab: Provider, API-Key, Temperature-Slider, MaxTokens

### 14.5 TypeScript-Interfaces

```typescript
type AgentActivityStatus = 'IDLE' | 'BUSY' | 'ERROR';

interface AgentInstance {
    id: string;
    templateId: string;
    name: string;
    parentInstanceId?: string;
    type: 'PERSISTENT' | 'EPHEMERAL';
    status: 'INACTIVE' | 'ACTIVE' | 'QUARANTINE' | 'TERMINATED';
    activityStatus: AgentActivityStatus;
    lastRunId?: string;
    config: string;  // JSONB
    customSystemPrompt?: string;
}

interface AgentRunDetail {
    id: string;
    instanceId: string;
    triggerType: string;
    status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';
    tokensUsed: number;
    costUsd: number;
    steps: AgentRunStep[];
}
```

---

## 15. Datenbank-Schema

### 15.1 Agent-Tabellen

```sql
-- Templates: Vorlagen für Agent-Typen
agent_templates (
    id UUID PK,
    tenant_id UUID FK NOT NULL,
    name VARCHAR NOT NULL,
    role VARCHAR NOT NULL,         -- "ceo", "production_lead", etc.
    description TEXT,
    base_prompt TEXT,              -- System-Prompt-Vorlage
    allowed_tools JSONB,          -- ["delegate_to_lead", "get_kpi_summary"]
    trigger_types JSONB,
    max_tokens_per_run INT DEFAULT 4096,
    daily_token_budget INT DEFAULT 100000,
    status VARCHAR DEFAULT 'ACTIVE',
    version INT DEFAULT 1,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
)

-- Instanzen: Laufende Agent-Objekte (persistent in DB)
agent_instances (
    id UUID PK,
    tenant_id UUID FK NOT NULL,
    template_id UUID FK NOT NULL,
    name VARCHAR NOT NULL,
    parent_instance_id UUID FK,   -- NULL für CEO, CEO-ID für Leads
    type VARCHAR NOT NULL,        -- PERSISTENT, EPHEMERAL
    status VARCHAR NOT NULL,      -- INACTIVE, ACTIVE, QUARANTINE, TERMINATED
    config JSONB,                 -- {"model": "claude-opus-4-6", "maxTokensPerRun": 4096}
    custom_system_prompt TEXT,    -- Override für Template-Prompt (nullable)
    activity_status VARCHAR(10) DEFAULT 'IDLE',  -- IDLE, BUSY, ERROR
    last_run_id UUID FK,          -- Letzter AgentRun
    activity_status_changed_at TIMESTAMPTZ,
    allowed_tools_override JSONB, -- Instance-Level Tool-Override (nullable)
    spawned_by_run_id UUID FK,    -- Für Sub-Agents: welcher Run hat sie erstellt
    terminated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
)

-- Runs: Einzelne Agent-Ausführungen
agent_runs (
    id UUID PK,
    tenant_id UUID FK NOT NULL,
    instance_id UUID FK NOT NULL,
    trigger_type VARCHAR NOT NULL,  -- CHAT, BUTTON, EVENT, SCHEDULE
    trigger_source VARCHAR,
    input_context JSONB,
    output JSONB,
    status VARCHAR NOT NULL,        -- PENDING, RUNNING, SUCCESS, FAILED, CANCELLED
    tokens_used INT DEFAULT 0,
    cost_usd DECIMAL(10,6) DEFAULT 0,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ
)

-- Steps: Einzelne Schritte innerhalb eines Runs
agent_run_steps (
    id UUID PK,
    tenant_id UUID FK NOT NULL,
    run_id UUID FK NOT NULL,
    step_number INT NOT NULL,
    type VARCHAR NOT NULL,          -- LLM_CALL, TOOL_CALL
    tool_name VARCHAR,
    input JSONB,
    output JSONB,
    tokens_used INT DEFAULT 0,
    duration_ms INT,
    created_at TIMESTAMPTZ
)

-- Memories: Persistentes Agent-Gedächtnis
agent_memories (
    id UUID PK,
    tenant_id UUID FK NOT NULL,
    instance_id UUID FK NOT NULL,
    type VARCHAR(30),               -- FACT, PREFERENCE, RULE, LEARNING, etc.
    category VARCHAR(100),
    key VARCHAR(255),               -- UNIQUE per (instance_id, key)
    value TEXT,
    importance INT,                 -- 1-10
    source VARCHAR(50) DEFAULT 'agent',
    metadata JSONB DEFAULT '{}',
    last_accessed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
)

-- Messages: Agent-zu-Agent Kommunikation
agent_messages (
    id UUID PK,
    tenant_id UUID FK NOT NULL,
    sender_instance_id UUID FK,
    target_instance_id UUID FK,
    message_type VARCHAR(30),       -- INFO, ALERT, REQUEST, REPORT
    subject VARCHAR(255),
    body TEXT,
    priority VARCHAR(10),           -- LOW, NORMAL, HIGH, CRITICAL
    status VARCHAR(20),             -- PENDING, DELIVERED, READ, ARCHIVED
    parent_message_id UUID FK,      -- Threading
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ
)

-- Incidents: Störungsmeldungen
agent_incidents (
    id UUID PK,
    tenant_id UUID FK NOT NULL,
    instance_id UUID FK NOT NULL,
    type VARCHAR,                   -- LLM_ERROR, RUNTIME_ERROR, TIMEOUT
    description TEXT,
    created_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ
)

-- Chat: Persistente Chat-Sessions
chat_sessions (
    id UUID PK,
    tenant_id UUID FK NOT NULL,
    user_id UUID FK NOT NULL,
    agent_instance_id UUID FK NOT NULL,
    title VARCHAR,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
)

chat_messages (
    id UUID PK,
    tenant_id UUID FK NOT NULL,
    session_id UUID FK NOT NULL,
    role VARCHAR(20),               -- user, assistant, system
    content TEXT,
    created_at TIMESTAMPTZ
)

-- LLM-Konfiguration pro Tenant
tenant_llm_config (
    id UUID PK,
    tenant_id UUID FK NOT NULL,
    provider VARCHAR,
    api_key VARCHAR,                -- AES-256-GCM verschlüsselt
    default_model VARCHAR,
    settings JSONB DEFAULT '{}',    -- temperature, maxTokensDefault
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
)
```

Alle Tabellen mit `tenant_id` haben RLS Policies: `USING (tenant_id = current_tenant_id())`.

### 15.2 Seed-Daten (Default Tenant)

**Templates (7):**
CEO, Production Lead, Machine Lead, Supply Lead, People Lead, Support Lead, Knowledge Lead

**Instances (8):**
CEO (parent=null), 5 Leads (parent=CEO), Knowledge Lead, Finance Lead

**Scheduled Triggers:**
CEO (6:00), Production Lead (22:00), Supply Lead (7:00)

**Role Defaults:**
ADMIN/MANAGER → CEO, TEAM_LEAD → Production Lead, WORKER → People Lead

---

## 16. Sicherheit & Isolation

### 16.1 Tenant-Isolation

1. **RLS (DB-Level):** Automatische Filterung aller Queries
2. **TenantContext (Application-Level):** ThreadLocal mit Tenant-ID
3. **findByIdSecure() (Service-Level):** Returns 403 statt 404 (kein Information Leakage)
4. **findByIdAndTenantId (Repository-Level):** Defense-in-Depth

### 16.2 Agent-Sicherheit

- Agenten greifen nur über Tools auf Daten zu (nie direkt auf DB)
- Tool-Whitelist pro Agent (Template + Instance-Override)
- Modul-Filterung (deaktivierte Module = keine Tools)
- Token-Budget pro Agent (daily + per-run)
- Hierarchie-Enforcement bei Messaging
- Sub-Agent-Limit (max 3 aktive pro Lead)
- Timeout bei Delegationen (90s)

### 16.3 API-Sicherheit

- JWT Access Token (24h) + Refresh Token (7d)
- Bucket4j Rate Limiting auf Login
- SYSTEM_ADMIN: Separater Endpunkt-Bereich
- @PreAuthorize Annotations auf sensiblen Endpoints
- AES-256-GCM Verschlüsselung für API-Keys

---

## 17. Datenfluss-Beispiele

### 17.1 User chattet mit CEO

```
User: "Wie ist der Produktionsstatus?"

1. POST /api/chat/message (SSE)
   ├─ SimpleChatService.streamChat()
   ├─ Session erstellen/laden
   ├─ User-Message in DB speichern
   ├─ AgentFactory.createAgent(ceoId) → CeoAgent-Objekt
   │   ├─ Template laden (base_prompt + allowed_tools)
   │   ├─ Memory-Sektion injizieren (Top 20 Memories)
   │   ├─ {{TENANT_NAME}} ersetzen
   │   └─ CeoAgent mit Identity + Capabilities erstellen
   │
   ├─ CeoAgent.getLastResponse(context, history, emitter)
   │   │
   │   ├─ Iteration 0: Anthropic API Stream
   │   │   ├─ LLM entscheidet: delegate_to_lead verwenden
   │   │   ├─ Tool-Use: { name: "delegate_to_lead", input: { lead: "produktions_lead", task: "Produktionsstatus abrufen" } }
   │   │   │
   │   │   ├─ Virtual Thread gestartet:
   │   │   │   ├─ TenantContext manuell gesetzt
   │   │   │   ├─ DelegateToLeadTool.execute()
   │   │   │   │   ├─ Production Lead Template + Instance laden
   │   │   │   │   ├─ AgentFactory.createAgent() → LeadAgent-Objekt
   │   │   │   │   │
   │   │   │   │   └─ LeadAgent.execute(context, task)
   │   │   │   │       ├─ Iteration 0: LlmProvider.chat()
   │   │   │   │       │   └─ Tool-Use: get_jobs { status: "IN_PRODUCTION" }
   │   │   │   │       │
   │   │   │   │       ├─ GetJobsTool.execute() → { jobs: [...] }
   │   │   │   │       │
   │   │   │   │       ├─ Iteration 1: LlmProvider.chat() (mit Tool-Result)
   │   │   │   │       │   └─ stop_reason: "end_turn"
   │   │   │   │       │
   │   │   │   │       └─ AgentResult { output: "5 Jobs in Produktion...", steps: [...] }
   │   │   │   │
   │   │   │   └─ TenantContext cleared
   │   │   │
   │   │   └─ tool_result Message gebaut
   │   │
   │   └─ Iteration 1: Anthropic API Stream
   │       ├─ LLM generiert finale Antwort (mit Delegation-Ergebnis)
   │       ├─ Text gestreamt als SSE tokens
   │       └─ stop_reason: "end_turn"
   │
   ├─ AgentRun abschließen (Tokens: 1500, Kosten: $0.045)
   ├─ Activity → IDLE
   ├─ Response in DB speichern
   ├─ Usage SSE-Event senden
   └─ Done SSE-Event senden
```

### 17.2 Lead spawnt Sub-Agent

```
Production Lead erhält Aufgabe: "Analysiere CNC-3 Wartungshistorie"

LeadAgent.execute():
  ├─ Iteration 0: LLM entscheidet spawn_sub_agent
  │   └─ Tool-Use: { name: "spawn_sub_agent", input: {
  │       task: "CNC-3 Wartungshistorie der letzten 30 Tage",
  │       tools: ["get_machine_detail", "get_maintenance_due"],
  │       name: "CNC-Analyst"
  │   }}
  │
  ├─ SpawnSubAgentTool.execute():
  │   ├─ Max Sub-Agents prüfen (< 3)
  │   ├─ Tool-Subset validieren
  │   ├─ EPHEMERAL Instance erstellen (parent=Production Lead)
  │   ├─ AgentFactory.spawnSubAgent() → SubAgent-Objekt
  │   │
  │   ├─ SubAgent.execute(context, task):
  │   │   ├─ Iteration 0: get_machine_detail → CNC-3 Daten
  │   │   ├─ Iteration 1: get_maintenance_due → Fällige Wartungen
  │   │   └─ Iteration 2: Finale Analyse generieren
  │   │
  │   ├─ Memories promoten (importance ≥ 7 → Production Lead)
  │   ├─ Restliche Memories löschen
  │   └─ Instance TERMINATED setzen
  │
  ├─ Iteration 1: LLM verarbeitet Sub-Agent-Ergebnis
  └─ AgentResult mit finaler Zusammenfassung

--- Später (SubAgentCleanupScheduler, alle 30 Min) ---
  ├─ EPHEMERAL Agents > 1h alt finden
  └─ Verwaiste terminieren (Sicherheitsnetz)
```

### 17.3 Scheduled Run (ohne Chat)

```
Cron: "0 0 6 * * *" (täglich 6:00)

1. ScheduledRunExecutor → AgentRunOrchestrator.triggerRun(ceoId, SCHEDULE)
2. AgentRunService.startRunWithBudgetCheck() → AgentRunEntity (PENDING)
3. AgentExecutionService.executeRun():
   ├─ Activity → BUSY
   ├─ Budget prüfen (tokensRemaining > 0)
   ├─ SystemPromptBuilder.build() (ohne Memory-Injection)
   │
   ├─ ReAct-Loop (bis 15 Iterationen):
   │   ├─ LLM-Call → Step loggen
   │   ├─ Tool-Call → Step loggen
   │   └─ Delegation → LeadAgent synchron ausführen
   │
   ├─ Kosten berechnen
   ├─ AgentRun → SUCCESS
   ├─ Activity → IDLE
   └─ Last-Run verlinken
```

---

*Erstellt: 2026-03-10 | Backend: ~570 Java-Dateien | V21 Migrationen | 40 Agent-Tools*
