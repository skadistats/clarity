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
import java.util.List;
import java.util.Set;

/**
 * Validates listener method signatures against their annotation's nested Listener SAM.
 *
 * <p>For every method bearing an annotation that is itself marked with
 * {@code @UsagePointMarker(EVENT_LISTENER)}, checks that the method's parameter list
 * is compatible with the annotation's nested {@code Listener} SAM.
 * Mismatches become compile errors.
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class ListenerValidationProcessor extends AbstractProcessor {

    private static final String USAGE_POINT_MARKER = "skadistats.clarity.event.UsagePointMarker";
    private static final String CONTEXT = "skadistats.clarity.processor.runner.Context";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (!roundEnv.processingOver()) {
                for (var element : roundEnv.getRootElements()) {
                    scanForListenerMethods(element);
                }
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "clarity ListenerValidationProcessor internal error: " + e
            );
        }
        return false;
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

            if (!isEventListenerAnnotation(annotationType)) continue;

            var listenerSam = findListenerSam(annotationType);
            if (listenerSam == null) continue;

            var samParams = listenerSam.getParameters();
            var methodParams = method.getParameters();

            int methodOffset = 0;
            if (!methodParams.isEmpty()) {
                var firstParam = methodParams.get(0).asType();
                if (isContextType(firstParam)) {
                    methodOffset = 1;
                }
            }

            int methodArity = methodParams.size() - methodOffset;

            if (isDynamicParameters(annotationType)) {
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
}
