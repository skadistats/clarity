package skadistats.clarity.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class EventAnnotationProcessor extends AbstractProcessor {

    private static final String USAGE_POINT_MARKER = "skadistats.clarity.event.UsagePointMarker";
    private static final String PROVIDES = "skadistats.clarity.event.Provides";
    private static final String CONTEXT = "skadistats.clarity.processor.runner.Context";

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
    }

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

    private boolean isEventListenerAnnotation(TypeElement annotationType) {
        for (var am : annotationType.getAnnotationMirrors()) {
            var amType = (TypeElement) am.getAnnotationType().asElement();
            if (amType.getQualifiedName().contentEquals(USAGE_POINT_MARKER)) {
                // Check value() == EVENT_LISTENER
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
