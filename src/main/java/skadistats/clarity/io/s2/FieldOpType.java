package skadistats.clarity.io.s2;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;

public enum FieldOpType {

    PlusOne(36271) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(1);
        }
    },
    PlusTwo(10334) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(2);
        }
    },
    PlusThree(1375) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(3);
        }
    },
    PlusFour(646) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(4);
        }
    },
    PlusN(4128) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(bs.readUBitVarFieldPath() + 5);
        }
    },
    PushOneLeftDeltaZeroRightZero(35) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.down();
        }
    },
    PushOneLeftDeltaZeroRightNonZero(3) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
        }
    },
    PushOneLeftDeltaOneRightZero(521) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(1);
            fp.down();
        }
    },
    PushOneLeftDeltaOneRightNonZero(2942) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(1);
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
        }
    },
    PushOneLeftDeltaNRightZero(560) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(bs.readUBitVarFieldPath());
            fp.down();
        }
    },
    PushOneLeftDeltaNRightNonZero(471) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(bs.readUBitVarFieldPath() + 2);
            fp.down();
            fp.inc(bs.readUBitVarFieldPath() + 1);
        }
    },
    PushOneLeftDeltaNRightNonZeroPack6Bits(10530) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(bs.readUBitInt(3) + 2);
            fp.down();
            fp.inc(bs.readUBitInt(3) + 1);
        }
    },
    PushOneLeftDeltaNRightNonZeroPack8Bits(251) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(bs.readUBitInt(4) + 2);
            fp.down();
            fp.inc(bs.readUBitInt(4) + 1);
        }
    },
    PushTwoLeftDeltaZero(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
        }
    },
    PushTwoPack5LeftDeltaZero(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.down();
            fp.inc(bs.readUBitInt(5));
            fp.down();
            fp.inc(bs.readUBitInt(5));
        }
    },
    PushThreeLeftDeltaZero(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
        }
    },
    PushThreePack5LeftDeltaZero(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.down();
            fp.inc(bs.readUBitInt(5));
            fp.down();
            fp.inc(bs.readUBitInt(5));
            fp.down();
            fp.inc(bs.readUBitInt(5));
        }
    },
    PushTwoLeftDeltaOne(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(1);
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
        }
    },
    PushTwoPack5LeftDeltaOne(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(1);
            fp.down();
            fp.inc(bs.readUBitInt(5));
            fp.down();
            fp.inc(bs.readUBitInt(5));
        }
    },
    PushThreeLeftDeltaOne(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(1);
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
        }
    },
    PushThreePack5LeftDeltaOne(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(1);
            fp.down();
            fp.inc(bs.readUBitInt(5));
            fp.down();
            fp.inc(bs.readUBitInt(5));
            fp.down();
            fp.inc(bs.readUBitInt(5));
        }
    },
    PushTwoLeftDeltaN(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(bs.readUBitVar() + 2);
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
        }
    },
    PushTwoPack5LeftDeltaN(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(bs.readUBitVar() + 2);
            fp.down();
            fp.inc(bs.readUBitInt(5));
            fp.down();
            fp.inc(bs.readUBitInt(5));
        }
    },
    PushThreeLeftDeltaN(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(bs.readUBitVar() + 2);
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
            fp.down();
            fp.inc(bs.readUBitVarFieldPath());
        }
    },
    PushThreePack5LeftDeltaN(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(bs.readUBitVar() + 2);
            fp.down();
            fp.inc(bs.readUBitInt(5));
            fp.down();
            fp.inc(bs.readUBitInt(5));
            fp.down();
            fp.inc(bs.readUBitInt(5));
        }
    },
    PushN(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            var n = bs.readUBitVar();
            fp.inc(bs.readUBitVar());
            for (var i = 0; i < n; i++) {
                fp.down();
                fp.inc(bs.readUBitVarFieldPath());
            }
        }
    },
    PushNAndNonTopographical(310) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            for (var i = 0; i <= fp.last(); i++) {
                if (bs.readBitFlag()) {
                    fp.inc(i, bs.readVarSInt() + 1);
                }
            }
            var c = bs.readUBitVar();
            for (var i = 0; i < c; i++) {
                fp.down();
                fp.inc(bs.readUBitVarFieldPath());
            }
        }
    },
    PopOnePlusOne(2) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.up(1);
            fp.inc(1);
        }
    },
    PopOnePlusN(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.up(1);
            fp.inc(bs.readUBitVarFieldPath() + 1);
        }
    },
    PopAllButOnePlusOne(1837) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.up(fp.last());
            fp.inc(1);
        }
    },
    PopAllButOnePlusN(149) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.up(fp.last());
            fp.inc(bs.readUBitVarFieldPath() + 1);
        }
    },
    PopAllButOnePlusNPack3Bits(300) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.up(fp.last());
            fp.inc(bs.readUBitInt(3) + 1);
        }
    },
    PopAllButOnePlusNPack6Bits(634) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.up(fp.last());
            fp.inc(bs.readUBitInt(6) + 1);
        }
    },
    PopNPlusOne(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.up(bs.readUBitVarFieldPath());
            fp.inc(1);
        }
    },
    PopNPlusN(0) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.up(bs.readUBitVarFieldPath());
            fp.inc(bs.readVarSInt());
        }
    },
    PopNAndNonTopographical(1) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.up(bs.readUBitVarFieldPath());
            for (var i = 0; i <= fp.last(); i++) {
                if (bs.readBitFlag()) {
                    fp.inc(i, bs.readVarSInt());
                }
            }
        }
    },
    NonTopoComplex(76) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            for (var i = 0; i <= fp.last(); i++) {
                if (bs.readBitFlag()) {
                    fp.inc(i, bs.readVarSInt());
                }
            }
        }
    },
    NonTopoPenultimatePluseOne(271) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            fp.inc(fp.last() - 1, 1);
        }
    },
    NonTopoComplexPack4Bits(99) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
            for (var i = 0; i <= fp.last(); i++) {
                if (bs.readBitFlag()) {
                    fp.inc(i, bs.readUBitInt(4) - 7);
                }
            }
        }
    },
    FieldPathEncodeFinish(25474) {
        @Override
        public void execute(S2ModifiableFieldPath fp, BitStream bs) {
        }
    };

    private final int weight;

    FieldOpType(int weight) {
        this.weight = weight;
    }

    public void execute(S2ModifiableFieldPath fp, BitStream bs) {
        throw new UnsupportedOperationException(String.format("FieldOp '%s' not implemented!", this.toString()));
    }

    public int getWeight() {
        return weight;
    }


}
