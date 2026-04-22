## Why

Several classes exist only to shuttle configuration and late-initialized state around the processor graph: `EntityStateFactory` is run configuration masquerading as a DI processor; `ContextData` is a shared mutable object populated after parsing starts; `source` sits in the processor list with no injectors. `Context` is the natural single point of access for everything describing a run — it should own construction, configuration, and late-initialized parse state directly, with clear invariants on each.

## What Changes

- **`EntityStateFactory` is eliminated** — its type enum fields (`s1EntityStateType`, `s2EntityStateType`, `s2FieldPathType`) and `FieldLayoutBuilder` become private fields on `Context`; `newEntityState(DTClass)` dispatches via sealed switch on S1DTClass/S2DTClass directly
- **`ContextData` is eliminated** — `buildNumber`, `millisPerTick`, `gameVersion` become set-once fields on `Context` with `IllegalStateException` guards; engine types call setters on the already-injected `ctx`
- **`Context` gains a unified construction API**: `newEntityState(DTClass)`, `newFieldReader()`, and set-once setters `setBuildNumber`, `setMillisPerTick`, `setGameVersion`, `setPointerCount`
- **`DTClass.getEmptyState()` is removed** — Context owns entity state dispatch; `S1DTClass` and `S2DTClass` no longer hold or receive an `EntityStateFactory`
- **`DTClasses`** drops `@Insert EntityStateFactory` and `getEntityStateFactory()`
- **`S2DTClassEmitter`** replaces `dtClasses.entityStateFactory.setPointerCount(n)` with `ctx.setPointerCount(n)`
- **`Entities` and `TempEntities`** use `context.newEntityState(cls)` and `context.newFieldReader()`
- **`AbstractRunner` gains `infraProcessors()` hook** — separates infrastructure processors from user processors; removes recursive `Object[]` flattening from `addProcessorsToModel`; `AbstractFileRunner` overrides to contribute `[this, engineType, packetReader, source]`
- **`AbstractRunner` gains `createContext()` hook** — `AbstractFileRunner` overrides to construct `Context` with its configured type enums and `FieldLayoutBuilder`

## Capabilities

### New Capabilities

- `context-construction-api`: Context as the single point of access for entity state construction, field reader construction, run configuration types, and parse-time initialized state — all with clear structural vs set-once invariants

### Modified Capabilities

_(none — runner configuration API and observable parse behaviour are unchanged)_

## Impact

- `skadistats.clarity.processor.runner.Context` — new fields, constructor, construction methods, set-once setters; **ContextData reference removed**
- `skadistats.clarity.processor.runner.AbstractRunner` / `AbstractFileRunner` — `infraProcessors()` and `createContext()` hooks; processor list simplified
- `skadistats.clarity.engine.ContextData` — **deleted**
- `skadistats.clarity.engine.EngineType` — `getContextData()` removed; engine subtypes call `ctx` setters directly
- `skadistats.clarity.state.EntityStateFactory` — **deleted**
- `skadistats.clarity.model.DTClass` — **BREAKING**: `getEmptyState()` and `setEntityStateFactory()` removed from interface
- `skadistats.clarity.model.s1.S1DTClass`, `skadistats.clarity.model.s2.S2DTClass` — `EntityStateFactory` field and `getEmptyState()` removed
- `skadistats.clarity.processor.sendtables.DTClasses` — remove `@Insert EntityStateFactory` and `getEntityStateFactory()`
- `skadistats.clarity.processor.sendtables.S2DTClassEmitter` — use `ctx.setPointerCount(n)`
- `skadistats.clarity.processor.entities.Entities` — use `context.newEntityState()` / `context.newFieldReader()`
- `skadistats.clarity.processor.tempentities.TempEntities` — use `context.newEntityState()` / `context.newFieldReader()`
