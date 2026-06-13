# Generative Layers — Framework

[![Maven Central](https://img.shields.io/maven-central/v/com.generativelayers/generative-layers-core?color=blue&label=Maven%20Central)](https://central.sonatype.com/artifact/com.generativelayers/generative-layers-core)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A governance middleware for BDI agents that use Large Language Models. Every LLM call passes through a **policy → validate → candidate → assess → decide** pipeline before the agent can adopt it. Generated content never becomes belief automatically — adoption is always an explicit agent decision.

## Install

Requires **Java 17+**.

```xml
<dependency>
    <groupId>com.generativelayers</groupId>
    <artifactId>generative-layers-core</artifactId>
    <version>0.2.1</version>
</dependency>
```

## The 13-Command Lifecycle

```
see → bind → call → result → candidate → check → get
                                   ↓
                             judge → decide → accept/reject → knowledge → explain
```

| # | Command | Purpose |
|---|---------|---------|
| 1 | `see` | Discover available providers |
| 2 | `bind` | Bind agent to a provider/model/config |
| 3 | `call` | Perform one governed LLM invocation |
| 4 | `result` | Inspect invocation outcome |
| 5 | `candidate` | Get the candidate ID from a result |
| 6 | `check` | Check validation or candidate status |
| 7 | `get` | Extract a field from candidate material |
| 8 | `judge` | Record evaluative evidence about a candidate |
| 9 | `decide` | Compute admissibility (read-only preview) |
| 10 | `accept` | Record positive decision (requires admissibility) |
| 11 | `reject` | Record negative decision |
| 12 | `knowledge` | Retrieve accepted GL-side knowledge for an agent |
| 13 | `explain` | Audit/trace any lifecycle object |

## Cross-Platform Parity

All platforms use the same 13 commands with the same semantics:

| Platform | Adapter | Syntax | Example |
|---|---|---|---|
| **ASTRA** | Module | `gl.command(...)` | `gl.bind("a1", "gemini", "", "")` |
| **Jason** | Internal Actions | `gl.command(...)` | `gl.bind("a1", "gemini", "", "", Bid)` |
| **JaCaMo** | CArtAgO Artifact | `command(...)` | `bind("a1", "gemini", "", "", Bid)` |

## Supported Providers

Any OpenAI-compatible endpoint works via the unified `ChatCompletionsProvider`.

| Provider | Env Variable | Status |
|---|---|---|
| **Gemini** | `GEMINI_API_KEY` | ✓ Tested |
| **Cerebras** | `CEREBRAS_API_KEY` | ✓ Tested |
| **Groq** | `GROQ_API_KEY` | ✓ Tested |
| **OpenAI** | `OPENAI_API_KEY` | Compatible |
| **DeepSeek** | `DEEPSEEK_API_KEY` | Compatible |

## Quick Start (Jason)

```prolog
+!start <-
    gl.bind("agent1", "gemini", "gemini-2.5-flash", "", Bid);
    gl.call(Bid, "classify", "llm.answer", "ANSWER", "Classify: apple", "label,confidence", "", Rid);
    gl.candidate(Rid, Cid);
    gl.get(Cid, "label", Label);
    gl.accept(Cid, "valid classification", Did);
    .print("Accepted: ", Label).
```

## Quick Start (ASTRA)

```java
module gl.adapter.astra.AstraAdapter gl;

rule +!main(list args) {
    string bid = gl.bind("agent1", "gemini", "gemini-2.5-flash", "");
    string rid = gl.call(bid, "classify", "llm.answer", "ANSWER", "Classify: apple", "label,confidence", "");
    string cid = gl.candidate(rid);
    string label = gl.get(cid, "label");
    gl.accept(cid, "valid classification");
    system.out("Accepted: " + label);
}
```

## Quick Start (JaCaMo)

```prolog
+!start <-
    makeArtifact("gl", "gl.adapter.jacamo.JaCaMoAdapter", [], Id);
    focus(Id);
    bind("agent1", "gemini", "gemini-2.5-flash", "", Bid);
    call(Bid, "classify", "llm.answer", "ANSWER", "Classify: apple", "label,confidence", "", Rid);
    candidate(Rid, Cid);
    get(Cid, "label", Label);
    accept(Cid, "valid classification", Did);
    .print("Accepted: ", Label).
```

## Semantic Constraints

The framework enforces governance rules at both the adapter and kernel levels:

- **Finality** — No `judge`, `accept`, or `reject` after a decision is recorded
- **Invalid isolation** — `INVALID` candidates cannot be rehabilitated by judgement
- **Affordance safety** — Bodies only accept affordances they declare
- **Admissibility gating** — `accept` requires the candidate to be admissible
- **Decision immutability** — Once accepted or rejected, the decision is permanent

## Architecture

```
┌─────────────────────────────────────────────┐
│              Agent Program (BDI)            │
│         ASTRA / Jason / JaCaMo             │
├─────────────────────────────────────────────┤
│            ResourceActions (13 commands)    │
│              DirectAdapter                 │
├─────────────────────────────────────────────┤
│         GovernanceKernel                   │
│   policy → provider → validate → candidate │
│   assess → admissibility → decision        │
├─────────────────────────────────────────────┤
│     Provider Registry                      │
│   Gemini | ChatCompletions (OpenAI/etc.)   │
│   Fake (deterministic, offline testing)    │
└─────────────────────────────────────────────┘
```

## Building from Source

```bash
cd core
mvn clean test        # Run all tests
mvn clean package     # Build JARs
```

## Documentation

- 📖 [Full docs & API reference](https://www.generativelayers.com/framework.html)
- 🚀 [Getting started](https://www.generativelayers.com/getting-started.html)
- 🔬 [Research](https://www.generativelayers.com/research.html)
- 💡 [Example projects](https://github.com/generativelayers/examples)

## License

Apache License 2.0
