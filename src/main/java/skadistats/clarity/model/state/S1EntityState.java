package skadistats.clarity.model.state;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.s1.S1FieldPath;

public sealed interface S1EntityState extends EntityState permits S1FlatEntityState, S1ObjectArrayEntityState {

    <T> T getValueForFieldPath(S1FieldPath fp);

    boolean write(S1FieldPath fp, Object decoded);

    boolean decodeInto(S1FieldPath fp, Decoder decoder, BitStream bs);

    boolean applyMutation(S1FieldPath fp, StateMutation mutation);

}
