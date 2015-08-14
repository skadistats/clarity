package skadistats.clarity.model.s2;

import skadistats.clarity.decoder.BitStream;

public enum FieldOpType {

    PlusOne(36271) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += 1;
        }
    },
    PlusTwo(10334) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += 2;
        }
    },
    PlusThree(1375) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += 3;
        }
    },
    PlusFour(646) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += 4;
        }
    },
    PlusN(4128) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            for (int bc : BIT_COUNTS) {
                if (bs.readBits(1) == 1) {
                    fp.path[fp.last] += bs.readBits(bc) + 5;
                }
            }
        }
    },
    PushOneLeftDeltaZeroRightZero(35) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[++fp.last] = 0;
        }
    },
    PushOneLeftDeltaZeroRightNonZero(3) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            for (int bc : BIT_COUNTS) {
                if (bs.readBits(1) == 1) {
                    fp.path[++fp.last] = bs.readBits(bc);
                    return;
                }
            }
        }
    },
    PushOneLeftDeltaOneRightZero(521) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last]++;
            fp.path[++fp.last] = 0;
        }
    },
    PushOneLeftDeltaOneRightNonZero(2942) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last]++;
            for (int bc : BIT_COUNTS) {
                if (bs.readBits(1) == 1) {
                    fp.path[++fp.last] = bs.readBits(bc);
                    return;
                }
            }
        }
    },
    PushOneLeftDeltaNRightZero(560),
    PushOneLeftDeltaNRightNonZero(471),
    PushOneLeftDeltaNRightNonZeroPack6Bits(10530),
    PushOneLeftDeltaNRightNonZeroPack8Bits(251),
    PushTwoLeftDeltaZero(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[++fp.last] = 0;
            fp.path[++fp.last] = 0;
        }
    },
    PushTwoPack5LeftDeltaZero(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[++fp.last] = bs.readBits(5);
            fp.path[++fp.last] = bs.readBits(5);
        }
    },
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
    PopOnePlusOne(2) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[--fp.last]++;
        }
    },
    PopOnePlusN(0),
    PopAllButOnePlusOne(1837) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.last = 0;
            fp.path[0]++;
        }
    },
    PopAllButOnePlusN(149),
    PopAllButOnePlusNPack3Bits(300),
    PopAllButOnePlusNPack6Bits(634),
    PopNPlusOne(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            for (int bc : BIT_COUNTS) {
                if (bs.readBits(1) == 1) {
                    fp.last -= bs.readBits(bc);
                    fp.path[fp.last]++;
                    return;
                }
            }
        }
    },
    PopNPlusN(0),
    PopNAndNonTopographical(1),
    NonTopoComplex(76) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            for (int i = 0; i <= fp.last; i++) {
                if (bs.readBits(1) == 1) {
                    fp.path[i] += bs.readVarInt32();
                }
            }
        }
    },
    NonTopoPenultimatePluseOne(271),
    NonTopoComplexPack4Bits(99) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            for (int i = 0; i <= fp.last; i++) {
                if (bs.readBits(1) == 1) {
                    fp.path[i] += bs.readBits(4) - 7;
                }
            }
        }
    },
    FieldPathEncodeFinish(25474) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
        }
    };

    private static final int[] BIT_COUNTS = {2, 4, 10, 17, 30};

    private final int weight;

    FieldOpType(int weight) {
        this.weight = weight;
    }

    public void execute(FieldPath fp, BitStream bs) {
        throw new UnsupportedOperationException(String.format("FieldOp '%s' not implemented!", this.toString()));
    }

    public int getWeight() {
        return weight;
    }


}
