package skadistats.clarity.state;

import skadistats.clarity.model.s2.Serializer;

public sealed interface StateMutation {
    record WriteValue(Object value) implements StateMutation {}
    record ResizeVector(int count) implements StateMutation {}
    record SwitchPolymorphicPointer(int pointerId, Serializer newSerializer) implements StateMutation {}
    record SwitchFixedPointer(Serializer serializer) implements StateMutation {}
}
