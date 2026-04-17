## Why

The `model/` package currently holds processor-shaped classes (`model/engine/*EngineType` + `PacketInstanceReader*`) and mixes engine-specific state (`model/state/S1*` + `model/state/S2*`) in a single directory. Schema types (SendProp, SendTable, Serializer, Field, DTClass concretes) live under `io/s{1,2}/` even though their abstract parent `DTClass` lives in `model/`, so the inheritance chain spans top-level packages. S1 and S2 code is scattered across seven locations with no consistent layout per horizontal concern.

Goal: make every horizontal concern that has engine-specific parts follow the same `(root, s1, s2)` shape, move processor-shaped classes out of `model/`, and colocate schema parent/child pairs.

## What Changes

Pure internal reorganization. No behavior changes, no capability changes, no deltas to specs. Public event-handler signatures (the `@On*` parameter types) are unaffected — all the referenced types stay at their current package path. Import path changes hit:

- Code that references `EngineType` directly (moves `model/ → engine/`).
- Code that imports from `model/engine/`, `model/state/`, or schema types from `io/s{1,2}/`.

`processor/`, `source/`, `event/`, `io/bitstream/`, `io/decoder/` (including its factory subtree) are untouched.

## Target layout

```
skadistats.clarity
├── source/         unchanged
├── event/          unchanged
├── processor/      UNTOUCHED (including processor/reader, processor/packet,
│                   processor/sendtables/{S1,S2}DTClassEmitter, processor/entities, ...)
├── io/
│   ├── (root)      FieldReader, FieldChanges, MutationListener, Util
│   ├── bitstream/  unchanged
│   ├── decoder/    unchanged (including factory/{s1,s2}/)
│   ├── s1/         S1FieldReader, DotaS1FieldReader, CsGoFieldReader, S1DecoderFactory
│   └── s2/         S2FieldReader, S2DecoderFactory
├── model/
│   ├── (root)      Entity, DTClass, FieldPath, StringTable, GameEvent,
│   │               GameEventDescriptor, CombatLogEntry, Vector, EngineId,
│   │               EngineMagic, GameVersionRange
│   ├── s1/         existing (PropFlag, PropType, S1FieldPath, S1CombatLogEntry,
│   │               S1CombatLogIndices, GameRulesStateType, ParticleAttachmentType)
│   │               + incoming: S1DTClass, ReceiveProp, SendProp, SendTable,
│   │                 SendTableExclusion, SendTableFlattener
│   └── s2/         existing (S2FieldPath, S2LongFieldPath*, S2CombatLogEntry,
│                   S2ModifiableFieldPath, S2LongModifiableFieldPath)
│                   + incoming: S2DTClass, Serializer, SerializerId, SerializerProperties,
│                     Field, FieldType, Pointer, field/*, FieldOp, FieldOpHuffmanGraph,
│                     FieldOpHuffmanTree
├── state/          NEW top-level (promoted from model/state/)
│   ├── (root)      EntityState, BaselineRegistry, EntityRegistry, EntityStateFactory,
│   │               ClientFrame, FieldLayout, FieldLayoutBuilder, PrimitiveType,
│   │               StateMutation
│   ├── s1/         S1EntityState, S1EntityStateType, S1FlatEntityState,
│   │               S1FlatLayout, S1ObjectArrayEntityState
│   └── s2/         S2EntityState, S2EntityStateType, S2FlatEntityState,
│                   S2NestedArrayEntityState, S2NestedArrayEntityStateIterator,
│                   S2NestedEntityState, S2TreeMapEntityState
└── engine/         NEW top-level (promoted from model/engine/)
    ├── (root)      EngineType, AbstractEngineType, AbstractProtobufDemoEngineType,
    │               ContextData, PacketInstanceReader, PacketInstanceReaderProtobufDemo
    ├── s1/         DotaS1EngineType, CsGoS1EngineType, PacketInstanceReaderCsGoS1
    └── s2/         DotaS2EngineType, CsgoS2EngineType, DeadlockEngineType
```

## Move table

Every row: old FQCN → new FQCN. Driven by a generated `git mv` + `sed -i` script across the three repos (`clarity/`, `clarity-analyzer/`, `clarity-examples/`). `clarity-protobuf/` is unaffected (no imports from `skadistats.clarity.*`).

### model/engine/ → engine/

| From | To |
|------|----|
| `skadistats.clarity.model.engine.EngineType` | `skadistats.clarity.engine.EngineType` *(note: currently in `model/`, not `model/engine/`; see separate row below)* |
| `skadistats.clarity.model.engine.AbstractEngineType` | `skadistats.clarity.engine.AbstractEngineType` |
| `skadistats.clarity.model.engine.AbstractProtobufDemoEngineType` | `skadistats.clarity.engine.AbstractProtobufDemoEngineType` |
| `skadistats.clarity.model.engine.ContextData` | `skadistats.clarity.engine.ContextData` |
| `skadistats.clarity.model.engine.PacketInstanceReader` | `skadistats.clarity.engine.PacketInstanceReader` |
| `skadistats.clarity.model.engine.PacketInstanceReaderProtobufDemo` | `skadistats.clarity.engine.PacketInstanceReaderProtobufDemo` |
| `skadistats.clarity.model.engine.DotaS1EngineType` | `skadistats.clarity.engine.s1.DotaS1EngineType` |
| `skadistats.clarity.model.engine.CsGoS1EngineType` | `skadistats.clarity.engine.s1.CsGoS1EngineType` |
| `skadistats.clarity.model.engine.PacketInstanceReaderCsGoS1` | `skadistats.clarity.engine.s1.PacketInstanceReaderCsGoS1` |
| `skadistats.clarity.model.engine.DotaS2EngineType` | `skadistats.clarity.engine.s2.DotaS2EngineType` |
| `skadistats.clarity.model.engine.CsgoS2EngineType` | `skadistats.clarity.engine.s2.CsgoS2EngineType` |
| `skadistats.clarity.model.engine.DeadlockEngineType` | `skadistats.clarity.engine.s2.DeadlockEngineType` |

### model/EngineType → engine/EngineType (the interface)

| From | To |
|------|----|
| `skadistats.clarity.model.EngineType` | `skadistats.clarity.engine.EngineType` |

### model/state/ → state/

| From | To |
|------|----|
| `skadistats.clarity.model.state.EntityState` | `skadistats.clarity.state.EntityState` |
| `skadistats.clarity.model.state.BaselineRegistry` | `skadistats.clarity.state.BaselineRegistry` |
| `skadistats.clarity.model.state.EntityRegistry` | `skadistats.clarity.state.EntityRegistry` |
| `skadistats.clarity.model.state.EntityStateFactory` | `skadistats.clarity.state.EntityStateFactory` |
| `skadistats.clarity.model.state.ClientFrame` | `skadistats.clarity.state.ClientFrame` |
| `skadistats.clarity.model.state.FieldLayout` | `skadistats.clarity.state.FieldLayout` |
| `skadistats.clarity.model.state.FieldLayoutBuilder` | `skadistats.clarity.state.FieldLayoutBuilder` |
| `skadistats.clarity.model.state.PrimitiveType` | `skadistats.clarity.state.PrimitiveType` |
| `skadistats.clarity.model.state.StateMutation` | `skadistats.clarity.state.StateMutation` |
| `skadistats.clarity.model.state.S1EntityState` | `skadistats.clarity.state.s1.S1EntityState` |
| `skadistats.clarity.model.state.S1EntityStateType` | `skadistats.clarity.state.s1.S1EntityStateType` |
| `skadistats.clarity.model.state.S1FlatEntityState` | `skadistats.clarity.state.s1.S1FlatEntityState` |
| `skadistats.clarity.model.state.S1FlatLayout` | `skadistats.clarity.state.s1.S1FlatLayout` |
| `skadistats.clarity.model.state.S1ObjectArrayEntityState` | `skadistats.clarity.state.s1.S1ObjectArrayEntityState` |
| `skadistats.clarity.model.state.S2EntityState` | `skadistats.clarity.state.s2.S2EntityState` |
| `skadistats.clarity.model.state.S2EntityStateType` | `skadistats.clarity.state.s2.S2EntityStateType` |
| `skadistats.clarity.model.state.S2FlatEntityState` | `skadistats.clarity.state.s2.S2FlatEntityState` |
| `skadistats.clarity.model.state.S2NestedArrayEntityState` | `skadistats.clarity.state.s2.S2NestedArrayEntityState` |
| `skadistats.clarity.model.state.S2NestedArrayEntityStateIterator` | `skadistats.clarity.state.s2.S2NestedArrayEntityStateIterator` |
| `skadistats.clarity.model.state.S2NestedEntityState` | `skadistats.clarity.state.s2.S2NestedEntityState` |
| `skadistats.clarity.model.state.S2TreeMapEntityState` | `skadistats.clarity.state.s2.S2TreeMapEntityState` |

### io/s1/ schema → model/s1/

| From | To |
|------|----|
| `skadistats.clarity.io.s1.S1DTClass` | `skadistats.clarity.model.s1.S1DTClass` |
| `skadistats.clarity.io.s1.ReceiveProp` | `skadistats.clarity.model.s1.ReceiveProp` |
| `skadistats.clarity.io.s1.SendProp` | `skadistats.clarity.model.s1.SendProp` |
| `skadistats.clarity.io.s1.SendTable` | `skadistats.clarity.model.s1.SendTable` |
| `skadistats.clarity.io.s1.SendTableExclusion` | `skadistats.clarity.model.s1.SendTableExclusion` |
| `skadistats.clarity.io.s1.SendTableFlattener` | `skadistats.clarity.model.s1.SendTableFlattener` |

**Stays in `io/s1/`:** `S1FieldReader`, `DotaS1FieldReader`, `CsGoFieldReader`, `S1DecoderFactory`.

### io/s2/ schema → model/s2/

| From | To |
|------|----|
| `skadistats.clarity.io.s2.S2DTClass` | `skadistats.clarity.model.s2.S2DTClass` |
| `skadistats.clarity.io.s2.Serializer` | `skadistats.clarity.model.s2.Serializer` |
| `skadistats.clarity.io.s2.SerializerId` | `skadistats.clarity.model.s2.SerializerId` |
| `skadistats.clarity.io.s2.SerializerProperties` | `skadistats.clarity.model.s2.SerializerProperties` |
| `skadistats.clarity.io.s2.Field` | `skadistats.clarity.model.s2.Field` |
| `skadistats.clarity.io.s2.FieldType` | `skadistats.clarity.model.s2.FieldType` |
| `skadistats.clarity.io.s2.Pointer` | `skadistats.clarity.model.s2.Pointer` |
| `skadistats.clarity.io.s2.FieldOp` | `skadistats.clarity.model.s2.FieldOp` |
| `skadistats.clarity.io.s2.FieldOpHuffmanGraph` | `skadistats.clarity.model.s2.FieldOpHuffmanGraph` |
| `skadistats.clarity.io.s2.FieldOpHuffmanTree` | `skadistats.clarity.model.s2.FieldOpHuffmanTree` |
| `skadistats.clarity.io.s2.field.ArrayField` | `skadistats.clarity.model.s2.field.ArrayField` |
| `skadistats.clarity.io.s2.field.PointerField` | `skadistats.clarity.model.s2.field.PointerField` |
| `skadistats.clarity.io.s2.field.SerializerField` | `skadistats.clarity.model.s2.field.SerializerField` |
| `skadistats.clarity.io.s2.field.ValueField` | `skadistats.clarity.model.s2.field.ValueField` |
| `skadistats.clarity.io.s2.field.VectorField` | `skadistats.clarity.model.s2.field.VectorField` |

**Stays in `io/s2/`:** `S2FieldReader`, `S2DecoderFactory`.

## Pre-move audit (verified 2026-04-17)

- **META-INF/services:** only file is `javax.annotation.processing.Processor`, listing four classes under `skadistats.clarity.processor.*`. The `processor/` tree is untouched by this refactor → no edit needed.
- **Wildcard imports (`import skadistats.clarity.*.*;`):** four occurrences, all `import skadistats.clarity.io.decoder.*;` (in `io/decoder/factory/s1/{Int,Long,Float}DecoderFactory.java` and `io/s2/S2DecoderFactory.java`). `io/decoder/` does not move → no expansion needed.
- Re-run both audits immediately before executing the script, in case new code lands in the meantime.

## Impact

- **clarity**: ~55 file moves, all internal; build must stay green, JMH unchanged.
- **clarity-analyzer**: import-path updates only (likely references to `EngineType`, state classes).
- **clarity-examples**: import-path updates only (`dev/` tools likely reference schema internals).
- **clarity-protobuf**: no change.
- **Public API**: every `@On*` parameter type stays at its current package path. Downstream user code that only writes event handlers needs no changes. Downstream code that imports `EngineType` or pokes at schema/state classes needs import updates only.
