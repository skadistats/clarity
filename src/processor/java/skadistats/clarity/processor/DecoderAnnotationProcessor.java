package skadistats.clarity.processor;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Scans {@code @RegisterDecoder}-annotated classes and generates:
 * <ul>
 *   <li>{@code DecoderIds} — int constants for each decoder type</li>
 *   <li>{@code DecoderDispatch} — tableswitch-based static dispatch method</li>
 * </ul>
 */
@SupportedAnnotationTypes("skadistats.clarity.io.decoder.RegisterDecoder")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class DecoderAnnotationProcessor extends AbstractProcessor {

    private static final String REGISTER_DECODER = "skadistats.clarity.io.decoder.RegisterDecoder";
    private static final String DECODER_BASE = "skadistats.clarity.io.decoder.Decoder";
    private static final String BITSTREAM = "skadistats.clarity.io.bitstream.BitStream";
    private static final String PACKAGE = "skadistats.clarity.io.decoder";

    private static final ClassName DECODER_CLASS = ClassName.get(PACKAGE, "Decoder");
    private static final ClassName BITSTREAM_CLASS = ClassName.get("skadistats.clarity.io.bitstream", "BitStream");

    private static final Pattern UPPER_SNAKE = Pattern.compile("([a-z0-9])([A-Z])");

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (roundEnv.processingOver()) return false;

            var registerType = processingEnv.getElementUtils().getTypeElement(REGISTER_DECODER);
            if (registerType == null) return false;

            var annotated = roundEnv.getElementsAnnotatedWith(registerType);
            if (annotated.isEmpty()) return false;

            var decoders = new ArrayList<DecoderInfo>();
            boolean hasErrors = false;

            for (var element : annotated) {
                if (!(element instanceof TypeElement typeElement)) continue;

                if (!validateExtendsDecoder(typeElement)) {
                    hasErrors = true;
                    continue;
                }

                var decodeMethod = findDecodeMethod(typeElement);
                if (decodeMethod == null) {
                    hasErrors = true;
                    continue;
                }

                if (!validateDecodeMethod(typeElement, decodeMethod)) {
                    hasErrors = true;
                    continue;
                }

                var stateful = decodeMethod.getParameters().size() == 2;
                decoders.add(new DecoderInfo(typeElement, stateful));
            }

            if (hasErrors) return false;

            decoders.sort(Comparator.comparing(d -> d.type.getSimpleName().toString()));

            generateDecoderIds(decoders);
            generateDecoderDispatch(decoders);

        } catch (Exception e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "clarity DecoderAnnotationProcessor internal error: " + e
            );
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    private boolean validateExtendsDecoder(TypeElement typeElement) {
        var types = processingEnv.getTypeUtils();
        var decoderType = processingEnv.getElementUtils().getTypeElement(DECODER_BASE);
        if (decoderType == null) return true; // can't resolve — let javac handle it

        var superType = typeElement.getSuperclass();
        if (superType instanceof DeclaredType dt) {
            var superElement = (TypeElement) dt.asElement();
            if (types.isAssignable(superElement.asType(), decoderType.asType())) {
                return true;
            }
        }

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "@RegisterDecoder class must extend Decoder",
                typeElement
        );
        return false;
    }

    private ExecutableElement findDecodeMethod(TypeElement typeElement) {
        var decodeMethods = typeElement.getEnclosedElements().stream()
                .filter(e -> e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .filter(e -> e.getSimpleName().contentEquals("decode"))
                .filter(e -> e.getModifiers().containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC)))
                .toList();

        if (decodeMethods.isEmpty()) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@RegisterDecoder class must have a public static decode method",
                    typeElement
            );
            return null;
        }

        if (decodeMethods.size() > 1) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@RegisterDecoder class must have exactly one public static decode method, found " + decodeMethods.size(),
                    typeElement
            );
            return null;
        }

        return decodeMethods.get(0);
    }

    private boolean validateDecodeMethod(TypeElement typeElement, ExecutableElement decodeMethod) {
        var params = decodeMethod.getParameters();
        var types = processingEnv.getTypeUtils();

        if (params.isEmpty() || params.size() > 2) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@RegisterDecoder decode method must have 1 or 2 parameters, found " + params.size(),
                    decodeMethod
            );
            return false;
        }

        // First param must be BitStream
        var bitstreamType = processingEnv.getElementUtils().getTypeElement(BITSTREAM);
        if (bitstreamType == null) return true;

        var firstParamType = params.get(0).asType();
        if (!types.isSameType(types.erasure(firstParamType), types.erasure(bitstreamType.asType()))) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@RegisterDecoder decode method first parameter must be BitStream",
                    decodeMethod
            );
            return false;
        }

        // If 2 params, second must match declaring class
        if (params.size() == 2) {
            var secondParamType = params.get(1).asType();
            if (!types.isSameType(types.erasure(secondParamType), types.erasure(typeElement.asType()))) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "@RegisterDecoder decode method second parameter must be " + typeElement.getSimpleName(),
                        decodeMethod
                );
                return false;
            }
        }

        return true;
    }

    // -----------------------------------------------------------------------
    // Code generation
    // -----------------------------------------------------------------------

    private void generateDecoderIds(List<DecoderInfo> decoders) throws IOException {
        var classBuilder = TypeSpec.classBuilder("DecoderIds")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        var staticInit = CodeBlock.builder();

        for (int i = 0; i < decoders.size(); i++) {
            var info = decoders.get(i);
            var constantName = toUpperSnake(info.type.getSimpleName().toString());
            var decoderClassName = ClassName.get(info.type);

            classBuilder.addField(FieldSpec.builder(int.class, constantName,
                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$L", i)
                    .build());

            staticInit.addStatement("$T.register($T.class, $L)", DECODER_CLASS, decoderClassName, constantName);
        }

        classBuilder.addField(FieldSpec.builder(int.class, "COUNT",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", decoders.size())
                .build());

        classBuilder.addStaticBlock(staticInit.build());

        JavaFile.builder(PACKAGE, classBuilder.build())
                .skipJavaLangImports(true)
                .build()
                .writeTo(processingEnv.getFiler());
    }

    private void generateDecoderDispatch(List<DecoderInfo> decoders) throws IOException {
        var switchCode = new StringBuilder();
        switchCode.append("return switch (d.id) {\n");

        for (var info : decoders) {
            var constantName = toUpperSnake(info.type.getSimpleName().toString());
            var decoderClassName = ClassName.get(info.type);

            if (info.stateful) {
                switchCode.append(String.format(
                        "    case DecoderIds.%s -> %s.%s.decode(bs, (%s.%s) d);\n",
                        constantName,
                        decoderClassName.packageName(), decoderClassName.simpleName(),
                        decoderClassName.packageName(), decoderClassName.simpleName()
                ));
            } else {
                switchCode.append(String.format(
                        "    case DecoderIds.%s -> %s.%s.decode(bs);\n",
                        constantName,
                        decoderClassName.packageName(), decoderClassName.simpleName()
                ));
            }
        }

        switchCode.append("    default -> throw new IllegalArgumentException(\"Unknown decoder id: \" + d.id);\n");
        switchCode.append("};\n");

        var dispatchMethod = MethodSpec.methodBuilder("decode")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(Object.class)
                .addParameter(BITSTREAM_CLASS, "bs")
                .addParameter(DECODER_CLASS, "d")
                .addCode(switchCode.toString())
                .build();

        var classBuilder = TypeSpec.classBuilder("DecoderDispatch")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(dispatchMethod);

        JavaFile.builder(PACKAGE, classBuilder.build())
                .skipJavaLangImports(true)
                .build()
                .writeTo(processingEnv.getFiler());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String toUpperSnake(String className) {
        // Remove "Decoder" suffix, then convert CamelCase to UPPER_SNAKE_CASE
        var name = className;
        if (name.endsWith("Decoder")) {
            name = name.substring(0, name.length() - "Decoder".length());
        }
        return UPPER_SNAKE.matcher(name).replaceAll("$1_$2").toUpperCase();
    }

    private record DecoderInfo(TypeElement type, boolean stateful) {}
}
