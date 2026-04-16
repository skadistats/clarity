package skadistats.clarity.model.state;

import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.io.decoder.StringZeroTerminatedDecoder;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.io.s2.field.ArrayField;
import skadistats.clarity.io.s2.field.PointerField;
import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.io.s2.field.ValueField;
import skadistats.clarity.io.s2.field.VectorField;

import java.util.HashMap;
import java.util.Map;

public final class FieldLayoutBuilder {

    public record Built(FieldLayout layout, int totalBytes) {}

    private static final int FLAG_BYTE = 1;
    private static final int SLOT_INDEX_BYTES = 4;
    private static final int STRING_LENGTH_PREFIX_BYTES = 2;

    /** Uniform reservation for strings without metadata bounds (S2 CUtlString, S2 CUtlSymbolLarge). */
    public static final int UNBOUNDED_STRING_MAX_LENGTH = 512;

    private final Map<Serializer, Built> serializerCache = new HashMap<>();

    public Built buildSerializer(Serializer serializer) {
        var cached = serializerCache.get(serializer);
        if (cached != null) return cached;
        var children = new FieldLayout[serializer.getFieldCount()];
        var cursor = 0;
        for (var i = 0; i < children.length; i++) {
            var built = buildField(serializer.getField(i), cursor);
            children[i] = built.layout;
            cursor += built.totalBytes;
        }
        var result = new Built(new FieldLayout.Composite(children), cursor);
        serializerCache.put(serializer, result);
        return result;
    }

    private Built buildField(Field field, int offset) {
        if (field instanceof SerializerField sf) {
            var children = new FieldLayout[sf.getSerializer().getFieldCount()];
            var cursor = offset;
            for (var i = 0; i < children.length; i++) {
                var built = buildField(sf.getSerializer().getField(i), cursor);
                children[i] = built.layout;
                cursor += built.totalBytes;
            }
            return new Built(new FieldLayout.Composite(children), cursor - offset);
        }
        if (field instanceof ArrayField af) {
            var elementBuilt = buildField(af.getElementField(), 0);
            var stride = elementBuilt.totalBytes;
            var length = af.getLength();
            var layout = new FieldLayout.Array(offset, stride, length, elementBuilt.layout);
            return new Built(layout, length * stride);
        }
        if (field instanceof VectorField vf) {
            var elementBuilt = buildField(vf.getElementField(), 0);
            var kind = new FieldLayout.SubStateKind.Vector(elementBuilt.totalBytes, elementBuilt.layout);
            var layout = new FieldLayout.SubState(offset, kind);
            return new Built(layout, FLAG_BYTE + SLOT_INDEX_BYTES);
        }
        if (field instanceof PointerField pf) {
            var serializers = pf.getSerializers();
            var layouts = new FieldLayout[serializers.length];
            var layoutBytes = new int[serializers.length];
            for (var i = 0; i < serializers.length; i++) {
                var built = buildSerializer(serializers[i]);
                layouts[i] = built.layout;
                layoutBytes[i] = built.totalBytes;
            }
            var kind = new FieldLayout.SubStateKind.Pointer(pf.getPointerId(), serializers, layouts, layoutBytes);
            var layout = new FieldLayout.SubState(offset, kind);
            return new Built(layout, FLAG_BYTE + SLOT_INDEX_BYTES);
        }
        if (field instanceof ValueField vf) {
            var decoder = vf.getDecoder();
            var primitive = decoder.getPrimitiveType();
            if (primitive != null) {
                return new Built(new FieldLayout.Primitive(offset, primitive), FLAG_BYTE + primitive.size());
            }
            if (decoder instanceof StringZeroTerminatedDecoder || decoder instanceof StringLenDecoder) {
                var maxLength = stringMaxLength(vf.getType());
                var layout = new FieldLayout.InlineString(offset, maxLength);
                return new Built(layout, FLAG_BYTE + STRING_LENGTH_PREFIX_BYTES + maxLength);
            }
            return new Built(new FieldLayout.Ref(offset), FLAG_BYTE + SLOT_INDEX_BYTES);
        }
        throw new IllegalStateException("unsupported field type: " + field.getClass().getName());
    }

    /**
     * Reserve size for an inline-string leaf. {@code char[N]} props use the declared
     * {@code N}; unbounded strings (CUtlString, CUtlSymbolLarge) use the uniform
     * 512-byte reservation grounded in the StringLenDecoder 9-bit wire cap.
     */
    private static int stringMaxLength(FieldType type) {
        if ("char".equals(type.getBaseType()) && type.getElementCount() != null) {
            try {
                return Integer.parseInt(type.getElementCount());
            } catch (NumberFormatException ignored) {
                // Non-literal element count (e.g. a named constant) — fall through to uniform.
            }
        }
        return UNBOUNDED_STRING_MAX_LENGTH;
    }
}
