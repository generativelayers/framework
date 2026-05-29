# Generative Layers (GL) — Core Framework

A governance middleware for BDI agents that use Large Language Models.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     BDI Agent (ASTRA / Jason)                   │
│                                                                 │
│  Goals   ──► BodyAffordance     ──► generation request          │
│  Beliefs ──► beliefContext      ──► prompt augmentation (RAG)   │
│                                                                 │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │              gl.adapter (Platform Binding)                 │  │
│  │  AstraAdapter / JasonAdapter ──► DirectAdapter (shared)    │  │
│  │                                      │                     │  │
│  │                              ResourceActions (16 commands) │  │
│  └────────────────────────────────┬───────────────────────────┘  │
│                                   │                              │
│  ┌────────────────────────────────▼───────────────────────────┐  │
│  │                gl (Governance Engine)                       │  │
│  │                                                            │  │
│  │  GenerativeBody ──► GovernanceKernel                       │  │
│  │                         │                                  │  │
│  │    1. PolicyDecision    │  GovernancePolicy.evaluate()     │  │
│  │    2. ConversationCtx   │  multi-turn history prepend      │  │
│  │    3. ProviderOutput    │  GenerativeProvider.generate()   │  │
│  │    4. RetryPolicy       │  retry on failure/invalid        │  │
│  │    5. ValidationResult  │  OutputValidator.validate()      │  │
│  │    6. Candidate         │  PROPOSED → VALIDATED            │  │
│  │    7. Assessment        │  (optional peer review)          │  │
│  │    8. Admissibility     │  AdmissibilityChecker.check()    │  │
│  │    9. Accept / Reject   │  Agent deliberation              │  │
│  │                                                            │  │
│  │  KernelListener ──► event hooks at every step              │  │
│  │  TraceRecord    ──► every step is auditable                │  │
│  │  Blob           ──► every prompt/output is stored          │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │              gl.provider (LLM Backend)                     │  │
│  │  GeminiProvider / OpenAiProvider / ProviderRegistry         │  │
│  └────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Package Structure

```
gl/                          9 files  — Governance engine (the core)
├── model/                  17 files  — Domain records & enums
├── body/                   10 files  — Affordance model & belief-RAG
├── provider/                5 files  — Pluggable LLM backends (Gemini, OpenAI)
└── adapter/                21 files  — Platform bindings (ASTRA, Jason)
```

### gl — Governance Engine

| Type | Purpose |
|---|---|
| `GovernanceKernel` | Central pipeline: policy → generate → validate → retry → candidate → assess → accept/reject |
| `GovernanceKernelFactory` | Factory + builder with configurable SPI ports, retry policy, and listeners |
| `KernelPorts` | SPI interfaces: GovernancePolicy, OutputValidator, AdmissibilityChecker, GenerativeProvider, BlobStore |
| `KernelDefaults` | Default implementations (permit-all policy, key=value validator, fake provider) |
| `KernelListener` | Event hooks: onPolicyDenied, onProviderFailed, onValidationFailed, onCandidateCreated/Accepted/Rejected, onRetry |
| `InMemoryKernelStores` | Thread-safe in-memory stores |
| `Outcomes` | Enum container: ResultOutcome, PolicyOutcome, AssessmentVerdict, AdmissibilityOutcome |
| `Ids` | UUID-based ID generation with type prefixes |

### gl.model — Domain Types

| Type | Purpose |
|---|---|
| `Candidate`, `CandidateStatus`, `CandidateType` | Governed generative output and its lifecycle |
| `Assessment`, `AdmissibilityDecision` | Peer review and gate check results |
| `ResourceRequest`, `ResourceResult` | Generation request/response (includes conversationId) |
| `ResponseSchema`, `ValidationResult` | Expected schema and validation outcome |
| `PolicyDecision`, `GovernanceContext` | Pre-generation policy gate |
| `ProviderOutput` | Raw LLM response metadata |
| `Blob`, `BlobType` | Content-addressed storage |
| `TraceRecord` | Full audit trail |
| `RetryPolicy` | Configurable retry on failure/invalid output |
| `ConversationContext` | Multi-turn conversation history with prompt prepend |

### gl.body — Affordance Model

| Type | Purpose |
|---|---|
| `BodyAffordance` | 13 affordance types: ANSWER, CLASSIFY, SUMMARISE, REFLECT, CRITIQUE, etc. |
| `GenerativeBody`, `DefaultGenerativeBody` | Interface and default impl: maps affordance → candidate type, belief-RAG prepend |
| `GenerativeBodyRegistry`, `GenerativeBodyRuntime` | Body factory and registry |
| `BodyDescriptor`, `BodyInvocation`, `BodyKind` | Body metadata, invocation request (with beliefContext), kind enum |
| `InvocationResult`, `InvocationStatus` | Result wrapper and status enum |

## Candidate Lifecycle

```
                    LLM output
                        │
                        ▼
    ┌─────────┐   ┌──────────┐   ┌──────────┐   ┌──────────────┐
    │ PROPOSED │──►│VALIDATED │──►│ ASSESSED │──►│  ACCEPTED    │
    └─────────┘   └──────────┘   └──────────┘   │  by agent    │
         │              │              │         └──────────────┘
         │              │              │                │
         ▼              ▼              ▼                ▼
      [policy       [schema       [peer           [enters
       denied]       invalid]      rejected]       beliefs]
              ▲              ▲
              │              │
         [retry with    [retry with
          error ctx]     error ctx]
```

## Quick Start

### ASTRA
```astra
agent MyAgent {
    module gl.adapter.AstraAdapter gl;

    rule +!main(list args) {
        gl.use_provider("gemini");
        string result = gl.ask("me", "goal", "Classify: apple");
        if (gl.valid(result)) {
            gl.accept(gl.candidate(result));
        }
    }
}
```

### Jason
```jason
+!start <-
    gl.adapter.jason.actions.use_provider("gemini");
    gl.adapter.jason.actions.ask("me", "goal", "Classify: apple", R);
    gl.adapter.jason.actions.valid(R, Valid);
    if (Valid) {
        gl.adapter.jason.actions.candidate(R, C);
        gl.adapter.jason.actions.accept(C);
    }.
```

### Java (Direct API)
```java
// With retry and event hooks
GovernanceKernel kernel = GovernanceKernelFactory.builder(myProvider)
    .withRetryPolicy(RetryPolicy.withRetries(3))
    .withListener(new KernelListener() {
        @Override public void onCandidateAccepted(Candidate c) {
            logger.info("Accepted: " + c.candidateId());
        }
    })
    .build();

ResourceResult result = kernel.invoke(new ResourceRequest(
    null, "agent", "goal", "llm.answer", "answer",
    CandidateType.CANDIDATE_ANSWER, "Classify: apple",
    ResponseSchema.required("schema", List.of("label")),
    GovernanceContext.empty(), Map.of(), "conversation-1"));
```

## Maven Dependency

```xml
<dependency>
    <groupId>io.github.generativelayers</groupId>
    <artifactId>generative-layers-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Building

```bash
# Build and test the framework
cd framework/core
mvn test        # 65 tests

# Package (JAR + sources + javadoc)
mvn package -Dgpg.skip=true

# Install to local Maven repository
mvn install -Dgpg.skip=true

# Build an ASTRA example
cd examples/astra/single-agent-candidate
export GEMINI_API_KEY="your-key"
mvn compile astra:deploy
```

## Extending

### Custom Provider
```java
ProviderRegistry.register("my-llm", config -> new MyLlmProvider(config));
```

### Custom Governance Policy
```java
GovernanceKernelFactory.builder(provider)
    .withPolicy(request -> PolicyDecision.allow())
    .build();
```

### Custom Listener
```java
GovernanceKernelFactory.builder(provider)
    .withListener(new KernelListener() {
        @Override public void onPolicyDenied(ResourceRequest r, PolicyDecision d) {
            alert("Blocked: " + d.reason());
        }
    })
    .build();
```

## Test Coverage

| Test Suite | Tests | What It Proves |
|---|---|---|
| KernelTest | 15 | Full governance lifecycle |
| AffordanceCoverageTest | 14 | All 13 affordances map correctly |
| ContractNetGLTest | 6 | Multi-agent CNP bidding |
| CrossPlatformParityTest | 5 | ASTRA = Jason governance outcomes |
| GoalDecompositionTest | 5 | DECOMPOSE_GOAL produces governed plans |
| RetryRecoveryTest | 5 | Retry on failure, invalid output, max attempts |
| ConversationContextTest | 6 | Multi-turn history, isolation, kernel integration |
| KernelListenerTest | 5 | Event hooks fire at correct lifecycle points |
| ReflectionLoopTest | 4 | Generate → Critique → Accept/Reject |
| **Total** | **65** | |
