# Generative Layers — Framework

[![Maven Central](https://img.shields.io/maven-central/v/com.generativelayers/generative-layers-core?color=blue&label=Maven%20Central)](https://central.sonatype.com/artifact/com.generativelayers/generative-layers-core)
[![Build](https://img.shields.io/badge/Tests-67%20passed-brightgreen)]()
[![Java](https://img.shields.io/badge/Java-17%2B-orange)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A governance middleware for BDI agents that use Large Language Models. Every LLM call passes through a **policy → validate → candidate → decide** pipeline before the agent can adopt it.

## Install

```xml
<dependency>
    <groupId>com.generativelayers</groupId>
    <artifactId>generative-layers-core</artifactId>
    <version>0.1.1</version>
</dependency>
```

## Supported Platforms

| Platform | Adapter | Syntax |
|---|---|---|
| **ASTRA** | `gl.GLModule` | `gl.ask(...)` |
| **Jason** | Internal Actions | `gl.ask(...)` |
| **JaCaMo** | CArtAgO Artifact | `ask(...)` |

## Supported Providers

| Provider | Env Variable | Free Tier | Status |
|---|---|---|---|
| **Cerebras** | `CEREBRAS_API_KEY` | Free (no credit card) | ✓ Tested |
| **Groq** | `GROQ_API_KEY` | 30 req/min, 14,400/day | ✓ Tested |
| **Gemini** | `GEMINI_API_KEY` | 15 req/min, 1,500/day | ✓ Tested |
| **OpenAI** | `OPENAI_API_KEY` | Paid only | Compatible |
| **DeepSeek** | `DEEPSEEK_API_KEY` | Small initial credit | Compatible |

> Any OpenAI-compatible endpoint works out of the box via the unified `ChatCompletionsProvider`.

## Quick Start

```bash
export GEMINI_API_KEY="your-key-here"    # or CEREBRAS_API_KEY, GROQ_API_KEY, etc.
git clone https://github.com/generativelayers/examples.git
cd examples/astra/single-agent-candidate
mvn compile astra:deploy
```

## Minimal Example (Jason)

```
+!main <-
    gl.configure("model", "gemini-2.0-flash");
    gl.use_provider("gemini");
    gl.ask("agent1", "classify", "Classify: apple", Rid);
    !decide(Rid).

+!decide(Rid) : gl.valid(Rid, true) <-
    gl.field(Rid, "label", Label);
    gl.candidate(Rid, Cid);
    gl.accept(Cid);
    .print("Adopted: ", Label).
```

## What's New in v0.1.1

- **Structured JSON outputs** — dynamic schema-driven generation for Gemini and ChatCompletions providers
- **Stateful conversation memory** — multi-turn dialogue via optional `conversationId` in `ask`
- **5 built-in providers** — Cerebras, Groq, Gemini, OpenAI, DeepSeek

## Documentation

📖 [Full docs & API reference](https://www.generativelayers.com/framework.html)
🚀 [Getting started](https://www.generativelayers.com/getting-started.html)
🔬 [Research](https://www.generativelayers.com/research.html)
💡 [Example projects](https://github.com/generativelayers/examples)

## Building from Source

```bash
cd core
mvn clean test        # Run 67 tests
mvn clean package     # Build JARs
```

## License

Apache License 2.0
