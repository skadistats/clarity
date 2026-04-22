## 1. Refactor AbstractRunner initialization hooks

- [x] 1.1 Add protected `infraProcessors()` method to `AbstractRunner` returning `List.of(this)`
- [x] 1.2 Add protected abstract `createContext(ExecutionModel em)` to `AbstractRunner`
- [x] 1.3 Rewrite `initWithProcessors` to use `infraProcessors()` and `createContext()`; retain recursive `Object[]` flattening in `addProcessorsToModel` for user-provided processors
- [x] 1.4 Override `infraProcessors()` in `AbstractFileRunner` to return `[this, engineType, engineType.getPacketReader(), source]`
- [x] 1.5 Remove `getRegisteredProcessors()` from `EngineType` interface and all engine type implementations once `infraProcessors()` covers them

## 2. Rebuild Context

- [x] 2.1 Add `s1EntityStateType`, `s2EntityStateType`, `s2FieldPathType`, and `FieldLayoutBuilder` as constructor parameters / private fields on `Context`
- [x] 2.2 Add set-once fields and setters for `buildNumber`, `millisPerTick`, `gameVersion`, `pointerCount` with `IllegalStateException` guards
- [x] 2.3 Add `newEntityState(DTClass cls)` with sealed switch dispatching to `s2EntityStateType.createState` / `s1EntityStateType.createState`
- [x] 2.4 Add `newFieldReader()` delegating to `engineType.getNewFieldReader(s2FieldPathType)`
- [x] 2.5 Implement `createContext(ExecutionModel em)` in `AbstractFileRunner` constructing `Context` with the configured type enums
- [x] 2.6 Remove `ContextData` parameter from `Context` constructor; remove `contextData` field; remove `getContextData()` delegation methods (replace with direct field access)

## 3. Delete EntityStateFactory and ContextData

- [x] 3.1 Delete `EntityStateFactory.java`
- [x] 3.2 Delete `ContextData.java`
- [x] 3.3 Remove `EngineType.getContextData()` from interface and all implementations
- [x] 3.4 Remove `EntityStateFactory` creation and processor-list entry from `AbstractFileRunner.initAndRunWith()`


## 4. Update engine types to use Context setters

- [x] 4.1 Replace `contextData.setBuildNumber(n)` with `ctx.setBuildNumber(n)` in `AbstractProtobufDemoEngineType`
- [x] 4.2 Replace `contextData.setMillisPerTick(n)` with `ctx.setMillisPerTick(n)` in `AbstractProtobufDemoEngineType` and `CsGoS1EngineType`
- [x] 4.3 Replace `contextData.setGameVersion(n)` with `ctx.setGameVersion(n)` in `AbstractProtobufDemoEngineType`

## 5. Remove EntityStateFactory from DTClass hierarchy

- [x] 5.1 Remove `setEntityStateFactory(EntityStateFactory)` from `DTClass` interface, `S1DTClass`, and `S2DTClass`
- [x] 5.2 Remove `getEmptyState()` from `DTClass` interface, `S1DTClass`, and `S2DTClass`
- [x] 5.3 Remove stored `EntityStateFactory` field from `S1DTClass` and `S2DTClass`

## 6. Update DTClasses and S2DTClassEmitter

- [x] 6.1 Remove `@Insert EntityStateFactory entityStateFactory` from `DTClasses`
- [x] 6.2 Remove `getEntityStateFactory()` from `DTClasses`
- [x] 6.3 Remove `dtClass.setEntityStateFactory(entityStateFactory)` from `DTClasses.onDTClass()`
- [x] 6.4 Replace `dtClasses.entityStateFactory.setPointerCount(n)` with `ctx.setPointerCount(n)` in both handlers in `S2DTClassEmitter`
- [x] 6.5 Remove `dtClasses.pointerCount` field and `getPointerCount()` if no remaining callers

## 7. Update Entities and TempEntities

- [x] 7.1 Replace `engineType.getNewFieldReader(dtClasses.getEntityStateFactory().getS2FieldPathType())` with `context.newFieldReader()` in `Entities`
- [x] 7.2 Replace `cls.getEmptyState()` with `context.newEntityState(cls)` in `Entities` baseline construction
- [x] 7.3 Replace `engineType.getNewFieldReader()` with `context.newFieldReader()` in `TempEntities`
- [x] 7.4 Replace `cls.getEmptyState()` with `context.newEntityState(cls)` in `TempEntities`

## 8. Verify and clean up

- [x] 8.1 Confirm no remaining references to `EntityStateFactory` or `ContextData` anywhere in the codebase
- [x] 8.2 Confirm no remaining calls to `getEmptyState()` or `setEntityStateFactory()` on any DTClass
- [x] 8.3 Confirm `source` does not appear in any processor list
- [x] 8.4 Build clarity, clarity-examples, and clarity-analyzer; confirm no compilation errors
- [x] 8.5 Run a full parse on a Dota S2 replay and confirm output is identical to pre-change
- [x] 8.6 Run a full parse on an S1 replay and confirm output is identical to pre-change
