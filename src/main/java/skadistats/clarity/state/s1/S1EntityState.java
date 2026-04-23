package skadistats.clarity.state.s1;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.StateDelta;
import skadistats.clarity.state.StateMutation;

public sealed interface S1EntityState extends EntityState permits S1FlatEntityState, S1ObjectArrayEntityState {

    <T> T getValueForFieldPath(S1FieldPath fp);

    boolean write(S1FieldPath fp, Object decoded);

    boolean decodeInto(S1FieldPath fp, Decoder decoder, BitStream bs);

    boolean applyMutation(S1FieldPath fp, StateMutation mutation);

    int getInt(S1FieldPath fp);

    long getLong(S1FieldPath fp);

    float getFloat(S1FieldPath fp);

    Object getObject(S1FieldPath fp);

    StateDelta captureChanged(S1FieldPath[] fps, int num);

    void applyFrom(StateDelta delta, S1FieldPath fp);

    void applyAll(StateDelta delta);

}
