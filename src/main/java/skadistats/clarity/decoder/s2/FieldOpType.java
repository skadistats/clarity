package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.FieldPath;

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
            fp.path[fp.last] += bs.readUBitVarFieldPath() + 5;
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
            fp.path[++fp.last] = bs.readUBitVarFieldPath();
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
            fp.path[++fp.last] = bs.readUBitVarFieldPath();
        }
    },
    PushOneLeftDeltaNRightZero(560) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += bs.readUBitVarFieldPath();
            fp.path[++fp.last] = 0;
        }
    },
    PushOneLeftDeltaNRightNonZero(471) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += bs.readUBitVarFieldPath() + 2;
            fp.path[++fp.last] = bs.readUBitVarFieldPath() + 1;
        }
    },
    PushOneLeftDeltaNRightNonZeroPack6Bits(10530) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += bs.readUBitInt(3) + 2;
            fp.path[++fp.last] = bs.readUBitInt(3) + 1;
        }
    },
    PushOneLeftDeltaNRightNonZeroPack8Bits(251) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += bs.readUBitInt(4) + 2;
            fp.path[++fp.last] = bs.readUBitInt(4) + 1;
        }
    },
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
            fp.path[++fp.last] = bs.readUBitInt(5);
            fp.path[++fp.last] = bs.readUBitInt(5);
        }
    },
    PushThreeLeftDeltaZero(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
        }
    },
    PushThreePack5LeftDeltaZero(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[++fp.last] = bs.readUBitInt(5);
            fp.path[++fp.last] = bs.readUBitInt(5);
            fp.path[++fp.last] = bs.readUBitInt(5);
        }
    },
    PushTwoLeftDeltaOne(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last]++;
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
        }
    },
    PushTwoPack5LeftDeltaOne(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last]++;
            fp.path[++fp.last] += bs.readUBitInt(5);
            fp.path[++fp.last] += bs.readUBitInt(5);
        }
    },
    PushThreeLeftDeltaOne(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last]++;
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
        }
    },
    PushThreePack5LeftDeltaOne(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last]++;
            fp.path[++fp.last] += bs.readUBitInt(5);
            fp.path[++fp.last] += bs.readUBitInt(5);
            fp.path[++fp.last] += bs.readUBitInt(5);
        }
    },
    PushTwoLeftDeltaN(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += bs.readUBitVar() + 2;
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
        }
    },
    PushTwoPack5LeftDeltaN(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += bs.readUBitVar() + 2;
            fp.path[++fp.last] += bs.readUBitInt(5);
            fp.path[++fp.last] += bs.readUBitInt(5);
        }
    },
    PushThreeLeftDeltaN(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += bs.readUBitVar() + 2;
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
            fp.path[++fp.last] += bs.readUBitVarFieldPath();
        }
    },
    PushThreePack5LeftDeltaN(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last] += bs.readUBitVar() + 2;
            fp.path[++fp.last] += bs.readUBitInt(5);
            fp.path[++fp.last] += bs.readUBitInt(5);
            fp.path[++fp.last] += bs.readUBitInt(5);
        }
    },
    PushN(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            int n = bs.readUBitVar();
            fp.path[fp.last] += bs.readUBitVar();
            for (int i = 0; i < n; i++) {
                fp.path[++fp.last] += bs.readUBitVarFieldPath();
            }
        }
    },
    PushNAndNonTopographical(310) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            for (int i = 0; i <= fp.last; i++) {
                if (bs.readBitFlag()) {
                    fp.path[i] += bs.readVarSInt() + 1;
                }
            }
            int c = bs.readUBitVar();
            for (int i = 0; i < c; i++) {
                fp.path[++fp.last] = bs.readUBitVarFieldPath();
            }
        }
    },
    PopOnePlusOne(2) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[--fp.last]++;
        }
    },
    PopOnePlusN(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[--fp.last] += bs.readUBitVarFieldPath() + 1;
        }
    },
    PopAllButOnePlusOne(1837) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.last = 0;
            fp.path[0]++;
        }
    },
    PopAllButOnePlusN(149) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.last = 0;
            fp.path[0] += bs.readUBitVarFieldPath() + 1;
        }
    },
    PopAllButOnePlusNPack3Bits(300) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.last = 0;
            fp.path[0] += bs.readUBitInt(3) + 1;
        }
    },
    PopAllButOnePlusNPack6Bits(634) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.last = 0;
            fp.path[0] += bs.readUBitInt(6) + 1;
        }
    },
    PopNPlusOne(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.last -= bs.readUBitVarFieldPath();
            fp.path[fp.last]++;
        }
    },
    PopNPlusN(0) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.last -= bs.readUBitVarFieldPath();
            fp.path[fp.last] += bs.readVarSInt();
        }
    },
    PopNAndNonTopographical(1) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.last -= bs.readUBitVarFieldPath();
            for (int i = 0; i <= fp.last; i++) {
                if (bs.readBitFlag()) {
                    fp.path[i] += bs.readVarSInt();
                }
            }
        }
    },
    NonTopoComplex(76) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            for (int i = 0; i <= fp.last; i++) {
                if (bs.readBitFlag()) {
                    fp.path[i] += bs.readVarSInt();
                }
            }
        }
    },
    NonTopoPenultimatePluseOne(271) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            fp.path[fp.last - 1]++;
        }
    },
    NonTopoComplexPack4Bits(99) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
            for (int i = 0; i <= fp.last; i++) {
                if (bs.readBitFlag()) {
                    fp.path[i] += bs.readUBitInt(4) - 7;
                }
            }
        }
    },
    FieldPathEncodeFinish(25474) {
        @Override
        public void execute(FieldPath fp, BitStream bs) {
        }
    };

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
