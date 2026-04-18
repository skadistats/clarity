package skadistats.clarity.model.s2;

import java.util.function.Supplier;

public enum S2FieldPathType {

    LONG(S2LongFieldPathBuilder::new);

    private final Supplier<S2FieldPathBuilder> builderFactory;

    S2FieldPathType(Supplier<S2FieldPathBuilder> builderFactory) {
        this.builderFactory = builderFactory;
    }

    public S2FieldPathBuilder newBuilder() {
        return builderFactory.get();
    }

}
