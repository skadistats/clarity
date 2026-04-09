# Reconnaissance Findings

## Annotations with @UsagePointMarker(EVENT_LISTENER)

### clarity core (28)

| Annotation | parameterClasses |
|-----------|-----------------|
| OnEntityCreated | Entity.class |
| OnEntityUpdated | Entity.class, FieldPath[].class, int.class |
| OnEntityDeleted | Entity.class |
| OnEntityEntered | Entity.class |
| OnEntityLeft | Entity.class |
| OnEntityPropertyCountChanged | Entity.class |
| OnEntityPropertyChanged | Entity.class, FieldPath.class |
| OnEntityUpdatesCompleted | (empty) |
| OnMessage | GeneratedMessage.class |
| OnPostEmbeddedMessage | GeneratedMessage.class, BitStream.class |
| OnMessageContainer | Class.class, ByteString.class |
| OnFullPacket | Demo.CDemoFullPacket.class |
| OnReset | Demo.CDemoStringTables.class, ResetPhase.class |
| OnTickStart | boolean.class |
| OnTickEnd | boolean.class |
| OnInit | (empty) |
| OnInputSource | Source.class, LoopController.class |
| OnDTClass | DTClass.class |
| OnDTClassesComplete | (empty) |
| OnStringTableEntry | StringTable.class, int.class, String.class, ByteString.class |
| OnStringTableCreated | int.class, StringTable.class |
| OnStringTableClear | (empty) |
| OnPlayerInfo | int.class, PlayerInfoType.class |
| OnGameEvent | GameEvent.class |
| OnGameEventDescriptor | GameEventDescriptor.class |
| OnCombatLogEntry | CombatLogEntry.class |
| OnTempEntity | Entity.class |
| OnModifierTableEntry | DOTAModifiers.CDOTAModifierBuffTableEntry.class |

### clarity-examples (7)

| Annotation | parameterClasses |
|-----------|-----------------|
| OnEntitySpawned | Entity.class |
| OnEntityDying | Entity.class |
| OnEntityDied | Entity.class |
| OnAbilityCooldownStart | Entity.class, Entity.class, Float.class |
| OnAbilityCooldownReset | Entity.class, Entity.class |
| OnAbilityCooldownEnd | Entity.class, Entity.class |
| OnEffectDispatch | String.class, CMsgEffectData.class |

## Discrepancies with tasks.md

- **OnSyncTick**: mentioned in task 8.10 but does NOT EXIST in the codebase
- **OnEntityUpdatesCompleted**: exists but NOT listed in tasks — needs migration (empty parameterClasses, used in Entities.java)
- **OnEffectDispatch**: listed in task 8.10 as a core annotation but it's in clarity-examples only
- **AbstractEngineType.java:113** and **CsGoS1EngineType.java:99**: call `ctx.createEvent(OnMessage.class, headerClass).raise(header)` inline — not covered by tasks. These must be updated when createEvent loses its varargs.

## @InsertEvent fields (27 in clarity, 0 in clarity-examples)

See agent output above. Key: none use `override=true` or `parameterTypes=...` — confirmed dead code.

## setInvocationPredicate callers (9 sites)

- Entities.java: 6× (classPattern-based entity event filters)
- GameEvents.java: 2× (event-name filters)
- InputSourceProcessor.java: 1× (OnMessageContainer class filter)
- BaseStringTableEmitter.java: 1× (table-name filter)

## setParameterClasses callers (2 sites)

- InputSourceProcessor.java:63: `setParameterClasses(messageClass)` for OnMessage
- InputSourceProcessor.java:71: `setParameterClasses(messageClass, BitStream.class)` for OnPostEmbeddedMessage

## Context.createEvent callers

- InputSourceProcessor.java:86,95 — OnMessage/OnPostEmbeddedMessage (creates per-class Events, stored in maps)
- AbstractEngineType.java:113 — OnMessage (inline, fire-and-forget)
- CsGoS1EngineType.java:99 — OnMessage (inline, fire-and-forget)
- clarity-examples: SpawnsAndDeaths.java (3×), Cooldowns.java (3×), EffectDispatches.java (1×)
