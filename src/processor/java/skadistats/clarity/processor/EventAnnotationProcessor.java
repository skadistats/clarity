package skadistats.clarity.processor;

import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.WildcardTypeName;


import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Compile-time annotation processor for clarity's typed event dispatch system.
 *
 * <p><b>Task A — Listener-signature validation:</b> For every method bearing an
 * annotation that is itself marked with {@code @UsagePointMarker(EVENT_LISTENER)},
 * checks that the method's parameter list is compatible with the annotation's
 * nested {@code Listener} SAM. Mismatches become compile errors.
 *
 * <p><b>Task B — {@code @Provides} indexing:</b> Collects all classes annotated
 * with {@code @Provides} and writes their qualified names to
 * {@code META-INF/clarity/providers.txt}, replacing the classindex dependency.
 *
 * <p><b>Task C — Event class generation:</b> For annotations marked with
 * {@code @GenerateEvent}, generates a top-level {@code <Annotation>_Event} class
 * implementing the annotation's nested {@code Event} interface, with typed
 * dispatch logic derived from the {@code Listener} and {@code Filter} SAMs.
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class EventAnnotationProcessor extends AbstractProcessor {

    private static final String USAGE_POINT_MARKER = "skadistats.clarity.event.UsagePointMarker";
    private static final String PROVIDES = "skadistats.clarity.event.Provides";
    private static final String CONTEXT = "skadistats.clarity.processor.runner.Context";
    private static final String GENERATE_EVENT = "skadistats.clarity.event.GenerateEvent";

    private static final ClassName EVENT_BASE = ClassName.get("skadistats.clarity.event", "Event");
    private static final ClassName EVENT_LISTENER = ClassName.get("skadistats.clarity.event", "EventListener");
    private static final ClassName RUNNER = ClassName.get("skadistats.clarity.processor.runner", "Runner");

    private final TreeSet<String> providerClasses = new TreeSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (!roundEnv.processingOver()) {
                processRound(roundEnv);
            } else {
                writeProvidersFile();
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "clarity processor internal error: " + e
            );
        }
        return false;
    }

    private void processRound(RoundEnvironment roundEnv) {
        // Task B: collect @Provides classes
        var providesType = processingEnv.getElementUtils().getTypeElement(PROVIDES);
        if (providesType != null) {
            for (var element : roundEnv.getElementsAnnotatedWith(providesType)) {
                if (element.getKind() == ElementKind.CLASS) {
                    providerClasses.add(((TypeElement) element).getQualifiedName().toString());
                }
            }
        }

        // Task A: validate listener method signatures
        for (var element : roundEnv.getRootElements()) {
            scanForListenerMethods(element);
        }

        // Task C: generate Event classes for @GenerateEvent annotations
        var generateEventType = processingEnv.getElementUtils().getTypeElement(GENERATE_EVENT);
        if (generateEventType != null) {
            for (var element : roundEnv.getElementsAnnotatedWith(generateEventType)) {
                if (element.getKind() == ElementKind.ANNOTATION_TYPE) {
                    generateEventClass((TypeElement) element);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Task C: Event class generation
    // -----------------------------------------------------------------------

    private void generateEventClass(TypeElement annotationType) {
        var strategy = getGenerateEventStrategy(annotationType);
        var listenerSam = findListenerSam(annotationType);
        if (listenerSam == null) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@GenerateEvent requires a nested Listener interface with a SAM method",
                    annotationType
            );
            return;
        }

        var filterSam = findFilterSam(annotationType);
        var annotationClassName = ClassName.get(annotationType);
        var pkg = annotationClassName.packageName();
        var generatedName = annotationType.getSimpleName() + "_Event";
        var listenerType = annotationClassName.nestedClass("Listener");
        var filterType = filterSam != null ? annotationClassName.nestedClass("Filter") : null;
        var eventInterface = annotationClassName.nestedClass("Event");

        // Parameterized Event<AnnotationType>
        var superClass = ParameterizedTypeName.get(EVENT_BASE, annotationClassName);
        // Set<EventListener<AnnotationType>>
        var listenerSetType = ParameterizedTypeName.get(
                ClassName.get(Set.class),
                ParameterizedTypeName.get(EVENT_LISTENER, annotationClassName)
        );

        TypeSpec eventClass;
        if ("BUCKETED".equals(strategy)) {
            eventClass = generateBucketedEvent(
                    generatedName, annotationType, annotationClassName, superClass,
                    listenerSetType, listenerType, listenerSam, eventInterface
            );
        } else {
            eventClass = generateStandardEvent(
                    generatedName, annotationClassName, superClass,
                    listenerSetType, listenerType, filterType, listenerSam, eventInterface
            );
        }

        try {
            JavaFile.builder(pkg, eventClass)
                    .skipJavaLangImports(true)
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to write generated Event class: " + e.getMessage(),
                    annotationType
            );
        }
    }

    private TypeSpec generateStandardEvent(
            String generatedName,
            ClassName annotationClassName,
            ParameterizedTypeName superClass,
            ParameterizedTypeName listenerSetType,
            ClassName listenerType,
            ClassName filterType,
            ExecutableElement listenerSam,
            ClassName eventInterface
    ) {
        var classBuilder = TypeSpec.classBuilder(generatedName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(superClass)
                .addSuperinterface(eventInterface);

        // Fields
        var listenerArrayType = ArrayTypeName.of(listenerType);
        classBuilder.addField(FieldSpec.builder(listenerArrayType, "listeners", Modifier.PRIVATE, Modifier.FINAL).build());

        if (filterType != null) {
            classBuilder.addField(FieldSpec.builder(ArrayTypeName.of(filterType), "filters", Modifier.PRIVATE, Modifier.FINAL).build());
        }

        // Constructor
        var ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(RUNNER, "runner")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), annotationClassName), "eventType")
                .addParameter(listenerSetType, "listenerSet")
                .addStatement("super(runner, eventType, listenerSet)")
                .addStatement("var els = listeners()");

        ctor.addStatement("listeners = new $T[els.length]", listenerType);
        if (filterType != null) {
            ctor.addStatement("filters = new $T[els.length]", filterType);
        }

        ctor.beginControlFlow("for (int i = 0; i < els.length; i++)");
        ctor.addStatement("listeners[i] = ($T) els[i].getListenerSam()", listenerType);
        if (filterType != null) {
            ctor.addStatement("filters[i] = ($T) els[i].getFilterSam()", filterType);
        }
        ctor.endControlFlow();

        classBuilder.addMethod(ctor.build());

        // raise() method
        var raiseParams = listenerSam.getParameters().stream()
                .map(p -> ParameterSpec.builder(TypeName.get(p.asType()), p.getSimpleName().toString()).build())
                .collect(Collectors.toList());

        var paramNames = listenerSam.getParameters().stream()
                .map(p -> p.getSimpleName().toString())
                .collect(Collectors.joining(", "));

        var raise = MethodSpec.methodBuilder("raise")
                .addModifiers(Modifier.PUBLIC)
                .addParameters(raiseParams)
                .beginControlFlow("for (int i = 0; i < listeners.length; i++)");

        if (filterType != null) {
            raise.addStatement("var f = filters[i]");
            raise.beginControlFlow("if (f != null && !f.test($L))", paramNames)
                    .addStatement("continue")
                    .endControlFlow();
        }

        raise.beginControlFlow("try")
                .addStatement("listeners[i].invoke($L)", paramNames)
                .nextControlFlow("catch ($T t)", Throwable.class)
                .addStatement("handleListenerException(i, t)")
                .endControlFlow()
                .endControlFlow();

        classBuilder.addMethod(raise.build());

        return classBuilder.build();
    }

    private TypeSpec generateBucketedEvent(
            String generatedName,
            TypeElement annotationType,
            ClassName annotationClassName,
            ParameterizedTypeName superClass,
            ParameterizedTypeName listenerSetType,
            ClassName listenerType,
            ExecutableElement listenerSam,
            ClassName eventInterface
    ) {
        // Find the value() method on the annotation to determine the bucket key type
        var valueMethod = findAnnotationValueMethod(annotationType);
        if (valueMethod == null) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "BUCKETED strategy requires a value() method on the annotation",
                    annotationType
            );
            return TypeSpec.classBuilder(generatedName).build();
        }

        // value() returns Class<? extends X>, extract X
        var valueReturnType = valueMethod.getReturnType();
        TypeName bucketKeyBound = extractClassBound(valueReturnType);

        // Entry inner class
        var entryClass = TypeSpec.classBuilder("Entry")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addField(int.class, "listenerIndex", Modifier.FINAL)
                .addField(listenerType, "listener", Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(int.class, "listenerIndex")
                        .addParameter(listenerType, "listener")
                        .addStatement("this.listenerIndex = listenerIndex")
                        .addStatement("this.listener = listener")
                        .build())
                .build();

        var entryArrayType = ArrayTypeName.of(ClassName.bestGuess("Entry"));
        var classWildcard = ParameterizedTypeName.get(
                ClassName.get(Class.class),
                WildcardTypeName.subtypeOf(bucketKeyBound)
        );

        var classBuilder = TypeSpec.classBuilder(generatedName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(superClass)
                .addSuperinterface(eventInterface)
                .addType(entryClass)
                .addField(FieldSpec.builder(entryArrayType, "EMPTY", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new Entry[0]")
                        .build())
                .addField(FieldSpec.builder(
                        ParameterizedTypeName.get(ClassName.get(java.util.Map.class), classWildcard, entryArrayType),
                        "byClass", Modifier.PRIVATE, Modifier.FINAL
                ).build())
                .addField(FieldSpec.builder(entryArrayType, "wildcardEntries", Modifier.PRIVATE, Modifier.FINAL).build());

        // Constructor
        var ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(RUNNER, "runner")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), annotationClassName), "eventType")
                .addParameter(listenerSetType, "listenerSet")
                .addStatement("super(runner, eventType, listenerSet)")
                .addStatement("var els = listeners()")
                .addStatement("$T<$T, $T<Entry>> buckets = new $T<>()",
                        java.util.Map.class, classWildcard, java.util.List.class, java.util.HashMap.class)
                .addStatement("$T<Entry> wildcards = new $T<>()", java.util.List.class, java.util.ArrayList.class)
                .beginControlFlow("for (int i = 0; i < els.length; i++)")
                .addStatement("var msgClass = els[i].getAnnotation().value()")
                .addStatement("var entry = new Entry(i, ($T) els[i].getListenerSam())", listenerType)
                .beginControlFlow("if (msgClass == $T.class)", bucketKeyBound)
                .addStatement("wildcards.add(entry)")
                .nextControlFlow("else")
                .addStatement("buckets.computeIfAbsent(msgClass, k -> new $T<>()).add(entry)", java.util.ArrayList.class)
                .endControlFlow()
                .endControlFlow()
                .addStatement("this.byClass = new $T<>()", java.util.HashMap.class)
                .beginControlFlow("for (var e : buckets.entrySet())")
                .addStatement("this.byClass.put(e.getKey(), e.getValue().toArray(EMPTY))")
                .endControlFlow()
                .addStatement("this.wildcardEntries = wildcards.toArray(EMPTY)");

        classBuilder.addMethod(ctor.build());

        // isListenedTo(Class<?>) overload
        classBuilder.addMethod(MethodSpec.methodBuilder("isListenedTo")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(classWildcard, "messageClass")
                .addStatement("return byClass.containsKey(messageClass) || wildcardEntries.length > 0")
                .build());

        // raise() method
        var raiseParams = listenerSam.getParameters().stream()
                .map(p -> ParameterSpec.builder(TypeName.get(p.asType()), p.getSimpleName().toString()).build())
                .collect(Collectors.toList());

        // First param is the message - use its class for bucketing
        var firstParamName = listenerSam.getParameters().get(0).getSimpleName().toString();
        var paramNames = listenerSam.getParameters().stream()
                .map(p -> p.getSimpleName().toString())
                .collect(Collectors.joining(", "));

        var raise = MethodSpec.methodBuilder("raise")
                .addModifiers(Modifier.PUBLIC)
                .addParameters(raiseParams)
                .addStatement("var bucket = byClass.get($L.getClass())", firstParamName)
                .beginControlFlow("if (bucket != null)")
                .beginControlFlow("for (var e : bucket)")
                .beginControlFlow("try")
                .addStatement("e.listener.invoke($L)", paramNames)
                .nextControlFlow("catch ($T t)", Throwable.class)
                .addStatement("handleListenerException(e.listenerIndex, t)")
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .beginControlFlow("for (var e : wildcardEntries)")
                .beginControlFlow("try")
                .addStatement("e.listener.invoke($L)", paramNames)
                .nextControlFlow("catch ($T t)", Throwable.class)
                .addStatement("handleListenerException(e.listenerIndex, t)")
                .endControlFlow()
                .endControlFlow();

        classBuilder.addMethod(raise.build());

        return classBuilder.build();
    }

    private ExecutableElement findAnnotationValueMethod(TypeElement annotationType) {
        for (var enclosed : annotationType.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD
                    && enclosed.getSimpleName().contentEquals("value")) {
                return (ExecutableElement) enclosed;
            }
        }
        return null;
    }

    private TypeName extractClassBound(TypeMirror classType) {
        // classType is Class<? extends X>, extract X
        if (classType instanceof DeclaredType dt) {
            var typeArgs = dt.getTypeArguments();
            if (!typeArgs.isEmpty()) {
                var arg = typeArgs.get(0);
                if (arg.getKind() == TypeKind.WILDCARD) {
                    var wildcard = (javax.lang.model.type.WildcardType) arg;
                    if (wildcard.getExtendsBound() != null) {
                        return TypeName.get(wildcard.getExtendsBound());
                    }
                }
                return TypeName.get(arg);
            }
        }
        return TypeName.get(classType);
    }

    private String getGenerateEventStrategy(TypeElement annotationType) {
        for (var am : annotationType.getAnnotationMirrors()) {
            var amType = (TypeElement) am.getAnnotationType().asElement();
            if (amType.getQualifiedName().contentEquals(GENERATE_EVENT)) {
                for (var entry : am.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().contentEquals("strategy")) {
                        return entry.getValue().getValue().toString();
                    }
                }
            }
        }
        return "STANDARD";
    }

    // -----------------------------------------------------------------------
    // Task A: Listener signature validation
    // -----------------------------------------------------------------------

    private void scanForListenerMethods(Element element) {
        if (element.getKind() == ElementKind.METHOD) {
            validateListenerMethod((ExecutableElement) element);
        }
        for (var enclosed : element.getEnclosedElements()) {
            scanForListenerMethods(enclosed);
        }
    }

    private void validateListenerMethod(ExecutableElement method) {
        for (var annotationMirror : method.getAnnotationMirrors()) {
            var annotationType = (TypeElement) annotationMirror.getAnnotationType().asElement();

            // Check if this annotation is itself marked with @UsagePointMarker(EVENT_LISTENER)
            if (!isEventListenerAnnotation(annotationType)) continue;

            // Find the nested Listener interface
            var listenerSam = findListenerSam(annotationType);
            if (listenerSam == null) continue; // annotation not yet migrated

            // Compare parameter lists
            var samParams = listenerSam.getParameters();
            var methodParams = method.getParameters();

            // Tolerate a leading Context parameter
            int methodOffset = 0;
            if (!methodParams.isEmpty()) {
                var firstParam = methodParams.get(0).asType();
                if (isContextType(firstParam)) {
                    methodOffset = 1;
                }
            }

            int methodArity = methodParams.size() - methodOffset;

            // Check for dynamicParameters
            if (isDynamicParameters(annotationType)) {
                // Best-effort: just check arity matches
                if (methodArity != samParams.size()) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            String.format(
                                    "@%s listener has %d parameter(s) (excluding Context), but %s.Listener expects %d",
                                    annotationType.getSimpleName(), methodArity,
                                    annotationType.getSimpleName(), samParams.size()
                            ),
                            method
                    );
                }
                return;
            }

            // Strict check: arity and types
            if (methodArity != samParams.size()) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format(
                                "@%s listener has %d parameter(s) (excluding Context), but %s.Listener expects %d. Expected: %s",
                                annotationType.getSimpleName(), methodArity,
                                annotationType.getSimpleName(), samParams.size(),
                                formatSamParams(samParams)
                        ),
                        method
                );
                return;
            }

            var types = processingEnv.getTypeUtils();
            for (int i = 0; i < samParams.size(); i++) {
                var samType = samParams.get(i).asType();
                var methodType = methodParams.get(i + methodOffset).asType();

                // For primitives, exact match required
                if (samType.getKind().isPrimitive() || methodType.getKind().isPrimitive()) {
                    if (!types.isSameType(samType, methodType)) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                String.format(
                                        "@%s listener parameter %d: expected %s, got %s",
                                        annotationType.getSimpleName(), i + 1, samType, methodType
                                ),
                                method
                        );
                    }
                } else {
                    // For reference types, method param must be assignable to/from SAM param
                    var erasedSam = types.erasure(samType);
                    var erasedMethod = types.erasure(methodType);
                    if (!types.isAssignable(erasedMethod, erasedSam) && !types.isAssignable(erasedSam, erasedMethod)) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                String.format(
                                        "@%s listener parameter %d: expected %s (or subtype/supertype), got %s",
                                        annotationType.getSimpleName(), i + 1, samType, methodType
                                ),
                                method
                        );
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    private boolean isEventListenerAnnotation(TypeElement annotationType) {
        for (var am : annotationType.getAnnotationMirrors()) {
            var amType = (TypeElement) am.getAnnotationType().asElement();
            if (amType.getQualifiedName().contentEquals(USAGE_POINT_MARKER)) {
                for (var entry : am.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().contentEquals("value")) {
                        return entry.getValue().getValue().toString().equals("EVENT_LISTENER");
                    }
                }
            }
        }
        return false;
    }

    private boolean isDynamicParameters(TypeElement annotationType) {
        for (var am : annotationType.getAnnotationMirrors()) {
            var amType = (TypeElement) am.getAnnotationType().asElement();
            if (amType.getQualifiedName().contentEquals(USAGE_POINT_MARKER)) {
                for (var entry : am.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().contentEquals("dynamicParameters")) {
                        return Boolean.TRUE.equals(entry.getValue().getValue());
                    }
                }
            }
        }
        return false;
    }

    private ExecutableElement findListenerSam(TypeElement annotationType) {
        for (var enclosed : annotationType.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.INTERFACE
                    && enclosed.getSimpleName().contentEquals("Listener")) {
                for (var member : enclosed.getEnclosedElements()) {
                    if (member.getKind() == ElementKind.METHOD) {
                        return (ExecutableElement) member;
                    }
                }
            }
        }
        return null;
    }

    private ExecutableElement findFilterSam(TypeElement annotationType) {
        for (var enclosed : annotationType.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.INTERFACE
                    && enclosed.getSimpleName().contentEquals("Filter")) {
                for (var member : enclosed.getEnclosedElements()) {
                    if (member.getKind() == ElementKind.METHOD) {
                        return (ExecutableElement) member;
                    }
                }
            }
        }
        return null;
    }

    private boolean isContextType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) return false;
        var element = processingEnv.getTypeUtils().asElement(type);
        if (element instanceof TypeElement te) {
            return te.getQualifiedName().contentEquals(CONTEXT);
        }
        return false;
    }

    private String formatSamParams(List<? extends javax.lang.model.element.VariableElement> params) {
        var sb = new StringBuilder("(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(params.get(i).asType());
        }
        sb.append(")");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Task B: @Provides indexing
    // -----------------------------------------------------------------------

    private void writeProvidersFile() {
        if (providerClasses.isEmpty()) return;
        try {
            var resource = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, "", "META-INF/clarity/providers.txt"
            );
            try (var writer = resource.openWriter()) {
                for (var name : providerClasses) {
                    writer.write(name);
                    writer.write('\n');
                }
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "clarity processor: failed to write providers.txt: " + e.getMessage()
            );
        }
    }

}
