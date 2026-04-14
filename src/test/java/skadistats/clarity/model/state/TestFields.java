package skadistats.clarity.model.state;

import skadistats.clarity.io.decoder.BoolDecoder;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.FloatNoScaleDecoder;
import skadistats.clarity.io.decoder.IntSignedDecoder;
import skadistats.clarity.io.decoder.LongSignedDecoder;
import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.io.decoder.VectorDefaultDecoder;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.io.s2.SerializerId;
import skadistats.clarity.io.s2.field.ArrayField;
import skadistats.clarity.io.s2.field.PointerField;
import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.io.s2.field.ValueField;
import skadistats.clarity.io.s2.field.VectorField;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Programmatic Serializer/Field hierarchy construction for tests.
 * No replay needed — builds real Serializer/Field/Decoder instances.
 */
public final class TestFields {

    private static final FieldType GENERIC_TYPE = FieldType.forString("test");
    private static final AtomicInteger POINTER_ID_SEQ = new AtomicInteger();

    private TestFields() {}

    public static Decoder intDecoder() {
        return new IntSignedDecoder(32);
    }

    public static Decoder floatDecoder() {
        return new FloatNoScaleDecoder();
    }

    public static Decoder longDecoder() {
        return new LongSignedDecoder(64);
    }

    public static Decoder boolDecoder() {
        return new BoolDecoder();
    }

    public static Decoder vectorDecoder(int dim) {
        return new VectorDefaultDecoder(dim, floatDecoder());
    }

    public static Decoder stringDecoder() {
        return new StringLenDecoder();
    }

    public static ValueField intField() {
        return new ValueField(GENERIC_TYPE, intDecoder(), null);
    }

    public static ValueField floatField() {
        return new ValueField(GENERIC_TYPE, floatDecoder(), null);
    }

    public static ValueField longField() {
        return new ValueField(GENERIC_TYPE, longDecoder(), null);
    }

    public static ValueField boolField() {
        return new ValueField(GENERIC_TYPE, boolDecoder(), null);
    }

    public static ValueField vectorField(int dim) {
        return new ValueField(GENERIC_TYPE, vectorDecoder(dim), null);
    }

    public static ValueField stringField() {
        return new ValueField(GENERIC_TYPE, stringDecoder(), null);
    }

    public static NamedField named(String name, Field field) {
        return new NamedField(name, field);
    }

    public static Serializer serializer(String name, NamedField... fields) {
        var id = new SerializerId(name, 0);
        var fArr = new Field[fields.length];
        var nArr = new String[fields.length];
        for (var i = 0; i < fields.length; i++) {
            fArr[i] = fields[i].field();
            nArr[i] = fields[i].name();
        }
        return new Serializer(id, fArr, nArr);
    }

    public static SerializerField rootField(Serializer serializer) {
        return new SerializerField(GENERIC_TYPE, serializer);
    }

    public static SerializerField serializerField(Serializer serializer) {
        return new SerializerField(GENERIC_TYPE, serializer);
    }

    public static ArrayField arrayField(Field element, int length) {
        return new ArrayField(GENERIC_TYPE, element, length);
    }

    public static VectorField vectorFieldOf(Field element) {
        return new VectorField(GENERIC_TYPE, element);
    }

    public static PointerField pointerField(Serializer... serializers) {
        // PointerDecoder expects bits but we don't use it for FieldLayoutBuilder purposes
        var pf = new PointerField(GENERIC_TYPE, null, null, serializers);
        pf.setPointerId(POINTER_ID_SEQ.getAndIncrement());
        return pf;
    }

    public static S2FieldPath fp(int... indices) {
        var mfp = S2ModifiableFieldPath.newInstance();
        for (var i = 0; i < indices.length; i++) {
            if (i > 0) mfp.down();
            mfp.set(i, indices[i]);
        }
        return mfp.unmodifiable();
    }

    public record NamedField(String name, Field field) {}
}
