# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Clarity is an open-source replay parser for Dota 2, CSGO, CS2, and Deadlock, written in Java. It parses Source 1 and Source 2 engine replay files to extract game data like entities, events, combat logs, and more.

## Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "skadistats.clarity.TestClassName"

# Clean build artifacts
./gradlew clean

# Generate Javadoc
./gradlew javadoc

# Create source and javadoc JARs
./gradlew sourcesJar javadocJar

# Publish to local Maven repository (for testing)
./gradlew publishToMavenLocal
```

## Project Requirements

- Java 17 or higher (configured in build.gradle.kts)
- Gradle (use included wrapper: ./gradlew)
- Dependencies managed via Maven Central

## Core Architecture

### Event-Driven Processor System

Clarity uses an annotation-based event system where processors declare what events they provide and consume:

- **@Provides**: Mark processors as event providers (e.g., `@Provides({OnEntityCreated.class})`)
- **@OnMessage**: Subscribe to specific protobuf message types
- **@Insert**: Inject processors or context into fields
- **@InsertEvent**: Inject event emitters
- **@Initializer**: Custom initialization for event listeners
- **@Order**: Control event listener execution order

The **ExecutionModel** automatically resolves dependencies, instantiates required processors, and injects dependencies at startup.

### Main Components

**Source** (`skadistats.clarity.source`): Abstraction for reading replay files
- `MappedFileSource`: Memory-mapped files (preferred for local files, supports seeking)
- `InputStreamSource`: Stream-based (limited seeking)
- `LiveSource`: For live replay streaming

**Runner** (`skadistats.clarity.processor.runner`): Orchestrates parsing pipeline
- `SimpleRunner`: Single-pass sequential parsing
- `ControllableRunner`: Allows seeking to specific ticks
- `Context`: Provides access to engine type, tick, build number

**EngineType** (`skadistats.clarity.model.engine`): Abstracts differences between game engines
- `DOTA_S1`, `DOTA_S2`: Dota 2 on Source 1/2
- `CSGO_S1`, `CSGO_S2`: CSGO/CS2 on Source 1/2
- `DEADLOCK`: Deadlock on Source 2

**Entities** (`skadistats.clarity.processor.entities`): Tracks all in-game entities and state changes
- Subscribes to CSVCMsg_PacketEntities messages (transmitted via @OnMessage)
- Entities encoded using proprietary protocol in Bitstreams (not standard protobuf)
- Handles entity lifecycle: CREATE, UPDATE, LEAVE, DELETE
- Manages baselines (default entity states)
- Emits events: `OnEntityCreated`, `OnEntityUpdated`, `OnEntityDeleted`

**DTClasses** (`skadistats.clarity.processor.sendtables`): Registry of entity class definitions
- Parses entity schemas from replay files
- Maps field paths to property names and decoders

**InputSourceProcessor** (`skadistats.clarity.processor.reader`): Root processor that drives the parsing pipeline
- Reads protobuf-encoded messages from replay files via `processSource()`
- Two-tier message routing:
  - Message containers (CDemoPacket, CDemoSendTables, etc.) emitted via @OnMessageContainer
  - `processEmbedded()` unpacks containers and emits individual messages via @OnMessage
- Other processors subscribe to @OnMessage for specific message types (CSVCMsg_PacketEntities, etc.)

### Source 1 vs Source 2 (s1 vs s2)

The codebase extensively uses `s1` and `s2` package naming to separate engine-specific implementations:

**Source 1**: Simpler field encoding with property indices
- Field readers: `DotaS1FieldReader`, `CsGoS1FieldReader`
- Decoder factories in `io.decoder.factory.s1`
- Send tables in `CDemoSendTables` message
- Entity data in CSVCMsg_PacketEntities encoded using property indices in bitstreams

**Source 2**: Hierarchical field paths with compact encoding
- Field reader: `S2FieldReader` (uses field path operations)
- Decoder factories in `io.decoder.factory.s2`
- Flattened serializer in `CSVCMsg_FlattenedSerializer`
- Supports deeply nested entity structures
- Entity data in CSVCMsg_PacketEntities encoded using field path operations in bitstreams

### Parsing Pipeline

Replay files contain protobuf-encoded messages that are read and routed by InputSourceProcessor:

```
Source → EngineType Detection → Runner.runWith(processors) →
ExecutionModel (dependency resolution & injection) → OnInit event →
InputSourceProcessor.processSource() reads PacketInstance objects →

For message containers (CDemoPacket, CDemoSendTables):
  → Emit via @OnMessageContainer with ByteString data →
  → processEmbedded() unpacks the ByteString →
  → Individual embedded messages emitted via @OnMessage

For direct messages:
  → Emit directly via @OnMessage

Processors subscribe to specific @OnMessage types (Entities, DTClasses, etc.) →
OnTickStart/OnTickEnd events → User processors receive parsed data
```

### Key Processors

Located in `skadistats.clarity.processor.*`:

- **reader**: Contains InputSourceProcessor, the root processor that reads replay files and routes protobuf messages via @OnMessage
- **entities**: Entity lifecycle and state management
- **sendtables**: Parse entity class definitions (DTClasses)
- **stringtables**: Named lookup tables for game data
- **gameevents**: Game events like player_death, item_purchase
- **modifiers**: Buffs/debuffs on entities (Dota 2)
- **tempentities**: Temporary visual effects
- **runner**: Execution orchestration

## Common Patterns

### Message Containers and Two-Tier Routing

Some protobuf messages in replays are **containers** that hold multiple embedded messages:

1. **Container messages** (CDemoPacket, CDemoSendTables, etc.) contain a ByteString with packed embedded messages
2. InputSourceProcessor emits containers via **@OnMessageContainer**
3. The `processEmbedded()` method subscribes to @OnMessageContainer and unpacks the ByteString
4. Individual embedded messages are then emitted via **@OnMessage** for other processors to consume

This two-tier architecture allows efficient packing of multiple messages while maintaining clean message routing.

### Entity Lifecycle

1. **Existent but not active**: Created but outside PVS (Potentially Visible Set)
2. **Active**: Visible to player
3. **Leave**: Entity goes out of PVS (but still exists)
4. **Delete**: Entity destroyed

### Baseline System

When a new entity is created, it starts with a baseline that represents its initial state. From that point on, all changes to the entity are made via updates. Baselines are stored per entity class in the "instancebaseline" string table.

### Tick-Based Processing

Everything is anchored to game ticks (not wall clock time). The parser processes tick-by-tick with TickStart → Process Messages → TickEnd cycles.

### Decoder Factory Pattern

Property decoders are created dynamically based on metadata using the Factory pattern. Each property type has a corresponding `DecoderFactory` that creates `Decoder<?>` instances.

## Code Organization

```
src/main/java/skadistats/clarity/
├── event/          Event system core (annotations, execution model)
├── io/             Low-level I/O (bitstream, decoders)
│   ├── bitstream/  Bit-oriented reading of compressed data
│   ├── decoder/    Decoder implementations
│   ├── s1/         Source 1 specific I/O
│   └── s2/         Source 2 specific I/O (field path operations)
├── model/          Data models (entities, DTClasses, engine types)
│   ├── csgo/       CSGO-specific models
│   ├── engine/     Engine type abstraction
│   ├── s1/         Source 1 models
│   └── s2/         Source 2 models
├── processor/      Core processors (entities, events, etc.)
├── source/         Replay file input abstraction
└── util/           Utilities

src/main/test/      TestNG tests
```

## Testing

- Uses TestNG framework (configured in build.gradle.kts)
- Tests located in `src/main/test/skadistats/`
- Requires replay files (symlinked to `/home/spheenik/projects/replays`)
- Run tests with `./gradlew test`

## Example Usage Pattern

```java
Source source = new MappedFileSource("replay.dem");
SimpleRunner runner = new SimpleRunner(source);

runner.runWith(new Object() {
    @OnEntityCreated
    public void onCreated(Entity entity) {
        // Handle entity creation
    }

    @OnMessage(CSVCMsg_ServerInfo.class)
    public void onServerInfo(CSVCMsg_ServerInfo msg) {
        // Handle server info message
    }
});
```

For more examples, see the separate [clarity-examples](https://github.com/skadistats/clarity-examples) repository.

## Dependencies

- `clarity-protobuf`: Wire protocol definitions (separate artifact)
- `snappy-java`: Compression support
- `slf4j-api`: Logging facade
- `classindex`: Annotation indexing for reflection

## Publishing

The project publishes to Maven Central via OSSRH:
- Signing configured via GPG
- Nexus publishing plugin configured
- Source and Javadoc JARs included
- Current version: 3.1.3
