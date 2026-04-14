package skadistats.clarity.model.state;

import skadistats.clarity.io.s2.Serializer;

public sealed interface FieldLayout {

    record Primitive(int offset, PrimitiveType type) implements FieldLayout {}

    record Ref(int offset) implements FieldLayout {}

    record Composite(FieldLayout[] children) implements FieldLayout {}

    record Array(int baseOffset, int stride, int length, FieldLayout element) implements FieldLayout {}

    record SubState(int offset, SubStateKind kind) implements FieldLayout {}

    sealed interface SubStateKind {

        record Vector(int elementBytes, FieldLayout elementLayout) implements SubStateKind {}

        record Pointer(int pointerId, Serializer[] serializers,
                       FieldLayout[] layouts, int[] layoutBytes) implements SubStateKind {}
    }
}
