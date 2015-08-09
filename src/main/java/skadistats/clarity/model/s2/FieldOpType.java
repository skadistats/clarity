package skadistats.clarity.model.s2;

import skadistats.clarity.decoder.BitStream;

import java.util.Comparator;

enum FieldOpType {

    PlusOne(36271) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.len - 1] += 1;
        }
    },
    PlusTwo(10334) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.len - 1] += 2;
        }
    },
    PlusThree(1375),
    PlusFour(646),
    PlusN(4128),
    PushOneLeftDeltaZeroRightZero(35),
    PushOneLeftDeltaZeroRightNonZero(3),
    PushOneLeftDeltaOneRightZero(521),
    PushOneLeftDeltaOneRightNonZero(2942),
    PushOneLeftDeltaNRightZero(560),
    PushOneLeftDeltaNRightNonZero(471),
    PushOneLeftDeltaNRightNonZeroPack6Bits(10530),
    PushOneLeftDeltaNRightNonZeroPack8Bits(251),
    PushTwoLeftDeltaZero(0),
    PushTwoPack5LeftDeltaZero(0),
    PushThreeLeftDeltaZero(0),
    PushThreePack5LeftDeltaZero(0),
    PushTwoLeftDeltaOne(0),
    PushTwoPack5LeftDeltaOne(0),
    PushThreeLeftDeltaOne(0),
    PushThreePack5LeftDeltaOne(0),
    PushTwoLeftDeltaN(0),
    PushTwoPack5LeftDeltaN(0),
    PushThreeLeftDeltaN(0),
    PushThreePack5LeftDeltaN(0),
    PushN(0),
    PushNAndNonTopographical(310),
    PopOnePlusOne(2),
    PopOnePlusN(0),
    PopAllButOnePlusOne(1837),
    PopAllButOnePlusN(149),
    PopAllButOnePlusNPack3Bits(300),
    PopAllButOnePlusNPack6Bits(634),
    PopNPlusOne(0),
    PopNPlusN(0),
    PopNAndNonTopographical(1),
    NonTopoComplex(76),
    NonTopoPenultimatePluseOne(271),
    NonTopoComplexPack4Bits(99),
    FieldPathEncodeFinish(25474);

    public static final Comparator<FieldOpType> OP_COMPARATOR = new Comparator<FieldOpType>() {
        @Override
        public int compare(FieldOpType o1, FieldOpType o2) {
            int r = Integer.compare(o1.weight, o2.weight);
            return r != 0 ? r : Integer.compare(o1.ordinal(), o2.ordinal());
        }
    };

    private final int weight;

    FieldOpType(int weight) {
        this.weight = weight;
    }

    public void execute(FieldPath fp, BitStream bs) {
        throw new UnsupportedOperationException(String.format("FieldOp '%s' not implemented!", this.toString()));
    }

}
