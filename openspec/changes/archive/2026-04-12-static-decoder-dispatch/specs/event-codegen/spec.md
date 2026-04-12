## MODIFIED Requirements

### Requirement: Processor modularization
The single `EventAnnotationProcessor` class SHALL be split into three separate annotation processors, each registered independently in `META-INF/services/javax.annotation.processing.Processor`. Each processor SHALL handle exactly one concern. All existing behavior SHALL be preserved — this is a structural refactor only.

#### Scenario: Listener validation is separate processor
- **WHEN** the project is compiled and a method bears an annotation marked with `@UsagePointMarker(EVENT_LISTENER)`
- **THEN** `ListenerValidationProcessor` SHALL validate the method signature against the annotation's nested `Listener` SAM, emitting compile errors on mismatch

#### Scenario: Provides indexing is separate processor
- **WHEN** the project is compiled and classes are annotated with `@Provides`
- **THEN** `ProvidesIndexProcessor` SHALL collect their qualified names and write `META-INF/clarity/providers.txt`

#### Scenario: Event generation is separate processor
- **WHEN** the project is compiled and annotations are marked with `@GenerateEvent`
- **THEN** `EventGenerationProcessor` SHALL generate `*_Event` classes using JavaPoet, with identical output to the current monolithic processor

#### Scenario: All processors registered in services file
- **WHEN** `META-INF/services/javax.annotation.processing.Processor` is read
- **THEN** it SHALL list `ListenerValidationProcessor`, `ProvidesIndexProcessor`, and `EventGenerationProcessor` as separate entries
