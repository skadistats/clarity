package skadistats.clarity.processor;

import com.palantir.javapoet.ClassName;
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

/**
 * Scans {@code @RegisterDecoder}-annotated classes and generates
 * {@code DecoderDispatch} with type-pattern switches on {@code Decoder}.
 * Every concrete decoder class annotated with {@code @RegisterDecoder}
 * gets a case arm; unknown subtypes fall through to the {@code default}
 * throw. This is "sealing by construction" — the generator covers the
 * full {@code @RegisterDecoder} set without requiring {@code Decoder}
 * itself to be a {@code sealed} class (which would force every new
 * decoder to edit {@code Decoder.java}'s {@code permits} clause).
 */
@SupportedAnnotationTypes("skadistats.clarity.io.decoder.RegisterDecoder")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DecoderAnnotationProcessor extends AbstractProcessor {

    private static final String REGISTER_DECODER = "skadistats.clarity.io.decoder.RegisterDecoder";
    private static final String DECODER_BASE = "skadistats.clarity.io.decoder.Decoder";
    private static final String BITSTREAM = "skadistats.clarity.io.bitstream.BitStream";
    private static final String PACKAGE = "skadistats.clarity.io.decoder";

    private static final ClassName DECODER_CLASS = ClassName.get(PACKAGE, "Decoder");
    private static final ClassName BITSTREAM_CLASS = ClassName.get("skadistats.clarity.io.bitstream", "BitStream");

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
                var decodeIntoMethod = findDecodeIntoMethod(typeElement);
                var hasDecodeInto = decodeIntoMethod != null
                        && validateDecodeIntoMethod(typeElement, decodeIntoMethod);
                decoders.add(new DecoderInfo(typeElement, stateful, hasDecodeInto));
            }

            if (hasErrors) return false;

            decoders.sort(Comparator.comparing(d -> d.type.getSimpleName().toString()));

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

    private ExecutableElement findDecodeIntoMethod(TypeElement typeElement) {
        var candidates = typeElement.getEnclosedElements().stream()
                .filter(e -> e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .filter(e -> e.getSimpleName().contentEquals("decodeInto"))
                .filter(e -> e.getModifiers().containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC)))
                .toList();
        if (candidates.isEmpty()) return null;
        if (candidates.size() > 1) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@RegisterDecoder class must have at most one public static decodeInto method, found " + candidates.size(),
                    typeElement
            );
            return null;
        }
        return candidates.get(0);
    }

    private boolean validateDecodeIntoMethod(TypeElement typeElement, ExecutableElement decodeIntoMethod) {
        var params = decodeIntoMethod.getParameters();
        var types = processingEnv.getTypeUtils();
        var elements = processingEnv.getElementUtils();

        // Expected signatures:
        //   stateless:  decodeInto(BitStream, byte[], int)
        //   stateful:   decodeInto(BitStream, <Self>, byte[], int)
        if (params.size() != 3 && params.size() != 4) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "decodeInto must have 3 (stateless) or 4 (stateful) parameters, found " + params.size(),
                    decodeIntoMethod
            );
            return false;
        }

        var bitstreamType = elements.getTypeElement(BITSTREAM);
        if (bitstreamType != null) {
            var firstParamType = params.get(0).asType();
            if (!types.isSameType(types.erasure(firstParamType), types.erasure(bitstreamType.asType()))) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "decodeInto first parameter must be BitStream",
                        decodeIntoMethod
                );
                return false;
            }
        }

        // Last two params: byte[], int
        var dataParam = params.get(params.size() - 2).asType();
        var dataParamStr = dataParam.toString();
        if (!"byte[]".equals(dataParamStr)) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "decodeInto penultimate parameter must be byte[] (found " + dataParamStr + ")",
                    decodeIntoMethod
            );
            return false;
        }
        var offsetParam = params.get(params.size() - 1).asType();
        if (!"int".equals(offsetParam.toString())) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "decodeInto last parameter must be int",
                    decodeIntoMethod
            );
            return false;
        }

        // If 4 params, second must match declaring class
        if (params.size() == 4) {
            var secondParamType = params.get(1).asType();
            if (!types.isSameType(types.erasure(secondParamType), types.erasure(typeElement.asType()))) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "decodeInto second parameter must be " + typeElement.getSimpleName() + " (found " + secondParamType + ")",
                        decodeIntoMethod
                );
                return false;
            }
        }

        return true;
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

    private void generateDecoderDispatch(List<DecoderInfo> decoders) throws IOException {
        var decodeSwitch = new StringBuilder();
        decodeSwitch.append("return switch (d) {\n");

        for (var info : decoders) {
            var decoderClassName = ClassName.get(info.type);
            var fqn = decoderClassName.packageName() + "." + decoderClassName.simpleName();
            if (info.stateful) {
                decodeSwitch.append(String.format(
                        "    case %s x -> %s.decode(bs, x);\n",
                        fqn, fqn
                ));
            } else {
                decodeSwitch.append(String.format(
                        "    case %s x -> %s.decode(bs);\n",
                        fqn, fqn
                ));
            }
        }

        decodeSwitch.append("    default -> throw new IllegalStateException(\"Unknown decoder: \" + d.getClass());\n");
        decodeSwitch.append("};\n");

        var decodeMethod = MethodSpec.methodBuilder("decode")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(Object.class)
                .addParameter(BITSTREAM_CLASS, "bs")
                .addParameter(DECODER_CLASS, "d")
                .addCode(decodeSwitch.toString())
                .build();

        var decodeIntoSwitch = new StringBuilder();
        decodeIntoSwitch.append("switch (d) {\n");
        for (var info : decoders) {
            if (!info.hasDecodeInto) continue;
            var decoderClassName = ClassName.get(info.type);
            var fqn = decoderClassName.packageName() + "." + decoderClassName.simpleName();
            if (info.stateful) {
                decodeIntoSwitch.append(String.format(
                        "    case %s x -> %s.decodeInto(bs, x, data, offset);\n",
                        fqn, fqn
                ));
            } else {
                decodeIntoSwitch.append(String.format(
                        "    case %s x -> %s.decodeInto(bs, data, offset);\n",
                        fqn, fqn
                ));
            }
        }
        decodeIntoSwitch.append("    default -> throw new IllegalStateException(\"Decoder \" + d.getClass().getSimpleName() + \" has no decodeInto\");\n");
        decodeIntoSwitch.append("}\n");

        var decodeIntoMethod = MethodSpec.methodBuilder("decodeInto")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(BITSTREAM_CLASS, "bs")
                .addParameter(DECODER_CLASS, "d")
                .addParameter(byte[].class, "data")
                .addParameter(int.class, "offset")
                .addCode(decodeIntoSwitch.toString())
                .build();

        var classBuilder = TypeSpec.classBuilder("DecoderDispatch")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(decodeMethod)
                .addMethod(decodeIntoMethod);

        JavaFile.builder(PACKAGE, classBuilder.build())
                .skipJavaLangImports(true)
                .build()
                .writeTo(processingEnv.getFiler());
    }

    private record DecoderInfo(TypeElement type, boolean stateful, boolean hasDecodeInto) {}
}
