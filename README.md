# Generative Layers — Framework

[![Maven Central](https://img.shields.io/maven-central/v/com.generativelayers/generative-layers-core?color=blue&label=Maven%20Central)](https://central.sonatype.com/artifact/com.generativelayers/generative-layers-core)
[![Build](https://img.shields.io/badge/Tests-65%20passed-brightgreen)]()
[![Java](https://img.shields.io/badge/Java-17%2B-orange)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A governance middleware for BDI agents that use Large Language Models. Every LLM call passes through a **policy → validate → candidate → decide** pipeline before the agent can adopt it.

## Install

```xml
<dependency>
    <groupId>com.generativelayers</groupId>
    <artifactId>generative-layers-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Supported Platforms

| Platform | Adapter | Syntax |
|---|---|---|
| **ASTRA** | `gl.GLModule` | `gl.ask(...)` |
| **Jason** | Internal Actions | `gl.ask(...)` |
| **JaCaMo** | CArtAgO Artifact | `ask(...)` |

## Supported Providers

| Provider | Key | Models |
|---|---|---|
| **Gemini** | `GEMINI_API_KEY` | gemini-2.0-flash, gemini-1.5-pro, etc. |
| **OpenAI** | `OPENAI_API_KEY` | gpt-4o, gpt-4o-mini, etc. |

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

## Documentation

📖 [Full docs & API reference](https://www.generativelayers.com/framework.html)  
🔬 [Research](https://www.generativelayers.com/research.html)  
💡 [Example projects](https://github.com/generativelayers/examples)

## Building from Source

```bash
cd core
mvn clean test        # Run 65 tests
mvn clean package     # Build JARs
```

## License

Apache License 2.0
