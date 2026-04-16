package skadistats.clarity.model.state;

import skadistats.clarity.io.s2.Serializer;

public sealed interface FieldLayout {

    record Primitive(int offset, PrimitiveType type) implements FieldLayout {}

    record Ref(int offset) implements FieldLayout {}

    /**
     * Inline-string leaf. Reserves {@code 1 (flag) + 2 (length prefix) + maxLength}
     * bytes at {@code offset}. Flag byte at {@code offset}; 2-byte LE length prefix at
     * {@code offset+1..offset+2}; UTF-8 bytes at {@code offset+3..offset+3+length-1}.
     * Bytes past the recorded length may hold stale data from prior writes.
     */
    record InlineString(int offset, int maxLength) implements FieldLayout {}

    record Composite(FieldLayout[] children) implements FieldLayout {}

    record Array(int baseOffset, int stride, int length, FieldLayout element) implements FieldLayout {}

    record SubState(int offset, SubStateKind kind) implements FieldLayout {}

    sealed interface SubStateKind {

        record Vector(int elementBytes, FieldLayout elementLayout) implements SubStateKind {}

        record Pointer(int pointerId, Serializer[] serializers,
                       FieldLayout[] layouts, int[] layoutBytes) implements SubStateKind {}
    }
}
