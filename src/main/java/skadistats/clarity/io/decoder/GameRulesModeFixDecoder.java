package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

public class GameRulesModeFixDecoder implements Decoder<Boolean> {

    @Override
    public Boolean decode(BitStream bs) {
        var v = bs.readBitFlag();
        // FIXME: There are 6 bits encoded in the replay 003628632841199288407_0580788690.dem that should not be there
        // FIXME: add a proper patch function
        bs.readUBitInt(6);
        return v;
    }

}
