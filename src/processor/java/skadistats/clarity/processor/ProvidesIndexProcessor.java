package skadistats.clarity.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Collects all classes annotated with {@code @Provides} and writes their qualified
 * names to {@code META-INF/clarity/providers.txt}.
 */
@SupportedAnnotationTypes("skadistats.clarity.event.Provides")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class ProvidesIndexProcessor extends AbstractProcessor {

    private static final String PROVIDES = "skadistats.clarity.event.Provides";

    private final TreeSet<String> providerClasses = new TreeSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (!roundEnv.processingOver()) {
                var providesType = processingEnv.getElementUtils().getTypeElement(PROVIDES);
                if (providesType != null) {
                    for (var element : roundEnv.getElementsAnnotatedWith(providesType)) {
                        if (element.getKind() == ElementKind.CLASS) {
                            providerClasses.add(((TypeElement) element).getQualifiedName().toString());
                        }
                    }
                }
            } else {
                writeProvidersFile();
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "clarity ProvidesIndexProcessor internal error: " + e
            );
        }
        return false;
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
                    "clarity ProvidesIndexProcessor: failed to write providers.txt: " + e.getMessage()
            );
        }
    }
}
