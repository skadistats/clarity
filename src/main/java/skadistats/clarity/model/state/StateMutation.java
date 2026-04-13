package skadistats.clarity.model.state;

import skadistats.clarity.io.s2.Serializer;

public sealed interface StateMutation {
    record WriteValue(Object value) implements StateMutation {}
    record ResizeVector(int count) implements StateMutation {}
    record SwitchPointer(Serializer newSerializer) implements StateMutation {}
}
