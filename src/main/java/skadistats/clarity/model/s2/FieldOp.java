package skadistats.clarity.model.s2;

import skadistats.clarity.io.bitstream.BitStream;

public record FieldOp(int weight, String name) {

    public static final int PLUS_ONE                                    =  0;
    public static final int PLUS_TWO                                    =  1;
    public static final int PLUS_THREE                                  =  2;
    public static final int PLUS_FOUR                                   =  3;
    public static final int PLUS_N                                      =  4;
    public static final int PUSH_ONE_LEFT_DELTA_ZERO_RIGHT_ZERO         =  5;
    public static final int PUSH_ONE_LEFT_DELTA_ZERO_RIGHT_NON_ZERO     =  6;
    public static final int PUSH_ONE_LEFT_DELTA_ONE_RIGHT_ZERO          =  7;
    public static final int PUSH_ONE_LEFT_DELTA_ONE_RIGHT_NON_ZERO      =  8;
    public static final int PUSH_ONE_LEFT_DELTA_N_RIGHT_ZERO            =  9;
    public static final int PUSH_ONE_LEFT_DELTA_N_RIGHT_NON_ZERO        = 10;
    public static final int PUSH_ONE_LEFT_DELTA_N_RIGHT_NZ_PACK_6       = 11;
    public static final int PUSH_ONE_LEFT_DELTA_N_RIGHT_NZ_PACK_8       = 12;
    public static final int PUSH_TWO_LEFT_DELTA_ZERO                    = 13;
    public static final int PUSH_TWO_PACK_5_LEFT_DELTA_ZERO             = 14;
    public static final int PUSH_THREE_LEFT_DELTA_ZERO                  = 15;
    public static final int PUSH_THREE_PACK_5_LEFT_DELTA_ZERO           = 16;
    public static final int PUSH_TWO_LEFT_DELTA_ONE                     = 17;
    public static final int PUSH_TWO_PACK_5_LEFT_DELTA_ONE              = 18;
    public static final int PUSH_THREE_LEFT_DELTA_ONE                   = 19;
    public static final int PUSH_THREE_PACK_5_LEFT_DELTA_ONE            = 20;
    public static final int PUSH_TWO_LEFT_DELTA_N                       = 21;
    public static final int PUSH_TWO_PACK_5_LEFT_DELTA_N                = 22;
    public static final int PUSH_THREE_LEFT_DELTA_N                     = 23;
    public static final int PUSH_THREE_PACK_5_LEFT_DELTA_N              = 24;
    public static final int PUSH_N                                      = 25;
    public static final int PUSH_N_AND_NON_TOPOGRAPHICAL                = 26;
    public static final int POP_ONE_PLUS_ONE                            = 27;
    public static final int POP_ONE_PLUS_N                              = 28;
    public static final int POP_ALL_BUT_ONE_PLUS_ONE                    = 29;
    public static final int POP_ALL_BUT_ONE_PLUS_N                      = 30;
    public static final int POP_ALL_BUT_ONE_PLUS_N_PACK_3               = 31;
    public static final int POP_ALL_BUT_ONE_PLUS_N_PACK_6               = 32;
    public static final int POP_N_PLUS_ONE                              = 33;
    public static final int POP_N_PLUS_N                                = 34;
    public static final int POP_N_AND_NON_TOPOGRAPHICAL                 = 35;
    public static final int NON_TOPO_COMPLEX                            = 36;
    public static final int NON_TOPO_PENULTIMATE_PLUS_ONE               = 37;
    public static final int NON_TOPO_COMPLEX_PACK_4                     = 38;
    public static final int FIELD_PATH_ENCODE_FINISH                    = 39;

    public static final FieldOp[] OPS = {
        new FieldOp(36271, "PlusOne"),                                  //  0
        new FieldOp(10334, "PlusTwo"),                                  //  1
        new FieldOp( 1375, "PlusThree"),                                //  2
        new FieldOp(  646, "PlusFour"),                                 //  3
        new FieldOp( 4128, "PlusN"),                                    //  4
        new FieldOp(   35, "PushOneLeftDeltaZeroRightZero"),            //  5
        new FieldOp(    3, "PushOneLeftDeltaZeroRightNonZero"),         //  6
        new FieldOp(  521, "PushOneLeftDeltaOneRightZero"),             //  7
        new FieldOp( 2942, "PushOneLeftDeltaOneRightNonZero"),          //  8
        new FieldOp(  560, "PushOneLeftDeltaNRightZero"),               //  9
        new FieldOp(  471, "PushOneLeftDeltaNRightNonZero"),            // 10
        new FieldOp(10530, "PushOneLeftDeltaNRightNonZeroPack6Bits"),   // 11
        new FieldOp(  251, "PushOneLeftDeltaNRightNonZeroPack8Bits"),   // 12
        new FieldOp(    0, "PushTwoLeftDeltaZero"),                     // 13
        new FieldOp(    0, "PushTwoPack5LeftDeltaZero"),                // 14
        new FieldOp(    0, "PushThreeLeftDeltaZero"),                   // 15
        new FieldOp(    0, "PushThreePack5LeftDeltaZero"),              // 16
        new FieldOp(    0, "PushTwoLeftDeltaOne"),                      // 17
        new FieldOp(    0, "PushTwoPack5LeftDeltaOne"),                 // 18
        new FieldOp(    0, "PushThreeLeftDeltaOne"),                    // 19
        new FieldOp(    0, "PushThreePack5LeftDeltaOne"),               // 20
        new FieldOp(    0, "PushTwoLeftDeltaN"),                        // 21
        new FieldOp(    0, "PushTwoPack5LeftDeltaN"),                   // 22
        new FieldOp(    0, "PushThreeLeftDeltaN"),                      // 23
        new FieldOp(    0, "PushThreePack5LeftDeltaN"),                 // 24
        new FieldOp(    0, "PushN"),                                    // 25
        new FieldOp(  310, "PushNAndNonTopographical"),                 // 26
        new FieldOp(    2, "PopOnePlusOne"),                            // 27
        new FieldOp(    0, "PopOnePlusN"),                              // 28
        new FieldOp( 1837, "PopAllButOnePlusOne"),                      // 29
        new FieldOp(  149, "PopAllButOnePlusN"),                        // 30
        new FieldOp(  300, "PopAllButOnePlusNPack3Bits"),               // 31
        new FieldOp(  634, "PopAllButOnePlusNPack6Bits"),               // 32
        new FieldOp(    0, "PopNPlusOne"),                              // 33
        new FieldOp(    0, "PopNPlusN"),                                // 34
        new FieldOp(    1, "PopNAndNonTopographical"),                  // 35
        new FieldOp(   76, "NonTopoComplex"),                           // 36
        new FieldOp(  271, "NonTopoPenultimatePluseOne"),               // 37
        new FieldOp(   99, "NonTopoComplexPack4Bits"),                  // 38
        new FieldOp(25474, "FieldPathEncodeFinish"),                    // 39
    };

    public static void execute(int opId, S2ModifiableFieldPath mfp, BitStream bs) {
        switch (opId) {
            case PLUS_ONE:
                mfp.inc(1);
                break;

            case PLUS_TWO:
                mfp.inc(2);
                break;

            case PLUS_THREE:
                mfp.inc(3);
                break;

            case PLUS_FOUR:
                mfp.inc(4);
                break;

            case PLUS_N:
                mfp.inc(bs.readUBitVarFieldPath() + 5);
                break;

            case PUSH_ONE_LEFT_DELTA_ZERO_RIGHT_ZERO:
                mfp.down();
                break;

            case PUSH_ONE_LEFT_DELTA_ZERO_RIGHT_NON_ZERO:
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                break;

            case PUSH_ONE_LEFT_DELTA_ONE_RIGHT_ZERO:
                mfp.inc(1);
                mfp.down();
                break;

            case PUSH_ONE_LEFT_DELTA_ONE_RIGHT_NON_ZERO:
                mfp.inc(1);
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                break;

            case PUSH_ONE_LEFT_DELTA_N_RIGHT_ZERO:
                mfp.inc(bs.readUBitVarFieldPath());
                mfp.down();
                break;

            case PUSH_ONE_LEFT_DELTA_N_RIGHT_NON_ZERO:
                mfp.inc(bs.readUBitVarFieldPath() + 2);
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath() + 1);
                break;

            case PUSH_ONE_LEFT_DELTA_N_RIGHT_NZ_PACK_6:
                mfp.inc(bs.readUBitInt(3) + 2);
                mfp.down();
                mfp.inc(bs.readUBitInt(3) + 1);
                break;

            case PUSH_ONE_LEFT_DELTA_N_RIGHT_NZ_PACK_8:
                mfp.inc(bs.readUBitInt(4) + 2);
                mfp.down();
                mfp.inc(bs.readUBitInt(4) + 1);
                break;

            case PUSH_TWO_LEFT_DELTA_ZERO:
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                break;

            case PUSH_TWO_PACK_5_LEFT_DELTA_ZERO:
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                break;

            case PUSH_THREE_LEFT_DELTA_ZERO:
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                break;

            case PUSH_THREE_PACK_5_LEFT_DELTA_ZERO:
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                break;

            case PUSH_TWO_LEFT_DELTA_ONE:
                mfp.inc(1);
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                break;

            case PUSH_TWO_PACK_5_LEFT_DELTA_ONE:
                mfp.inc(1);
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                break;

            case PUSH_THREE_LEFT_DELTA_ONE:
                mfp.inc(1);
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                break;

            case PUSH_THREE_PACK_5_LEFT_DELTA_ONE:
                mfp.inc(1);
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                break;

            case PUSH_TWO_LEFT_DELTA_N:
                mfp.inc(bs.readUBitVar() + 2);
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                break;

            case PUSH_TWO_PACK_5_LEFT_DELTA_N:
                mfp.inc(bs.readUBitVar() + 2);
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                break;

            case PUSH_THREE_LEFT_DELTA_N:
                mfp.inc(bs.readUBitVar() + 2);
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                mfp.down();
                mfp.inc(bs.readUBitVarFieldPath());
                break;

            case PUSH_THREE_PACK_5_LEFT_DELTA_N:
                mfp.inc(bs.readUBitVar() + 2);
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                mfp.down();
                mfp.inc(bs.readUBitInt(5));
                break;

            case PUSH_N: {
                var c = bs.readUBitVar();
                mfp.inc(bs.readUBitVar());
                for (var i = 0; i < c; i++) {
                    mfp.down();
                    mfp.inc(bs.readUBitVarFieldPath());
                }
                break;
            }

            case PUSH_N_AND_NON_TOPOGRAPHICAL: {
                for (var i = 0; i <= mfp.last(); i++) {
                    if (bs.readBitFlag()) {
                        mfp.inc(i, bs.readVarSInt() + 1);
                    }
                }
                var c = bs.readUBitVar();
                for (var i = 0; i < c; i++) {
                    mfp.down();
                    mfp.inc(bs.readUBitVarFieldPath());
                }
                break;
            }

            case POP_ONE_PLUS_ONE:
                mfp.up(1);
                mfp.inc(1);
                break;

            case POP_ONE_PLUS_N:
                mfp.up(1);
                mfp.inc(bs.readUBitVarFieldPath() + 1);
                break;

            case POP_ALL_BUT_ONE_PLUS_ONE:
                mfp.up(mfp.last());
                mfp.inc(1);
                break;

            case POP_ALL_BUT_ONE_PLUS_N:
                mfp.up(mfp.last());
                mfp.inc(bs.readUBitVarFieldPath() + 1);
                break;

            case POP_ALL_BUT_ONE_PLUS_N_PACK_3:
                mfp.up(mfp.last());
                mfp.inc(bs.readUBitInt(3) + 1);
                break;

            case POP_ALL_BUT_ONE_PLUS_N_PACK_6:
                mfp.up(mfp.last());
                mfp.inc(bs.readUBitInt(6) + 1);
                break;

            case POP_N_PLUS_ONE:
                mfp.up(bs.readUBitVarFieldPath());
                mfp.inc(1);
                break;

            case POP_N_PLUS_N:
                mfp.up(bs.readUBitVarFieldPath());
                mfp.inc(bs.readVarSInt());
                break;

            case POP_N_AND_NON_TOPOGRAPHICAL: {
                mfp.up(bs.readUBitVarFieldPath());
                for (var i = 0; i <= mfp.last(); i++) {
                    if (bs.readBitFlag()) {
                        mfp.inc(i, bs.readVarSInt());
                    }
                }
                break;
            }

            case NON_TOPO_COMPLEX: {
                for (var i = 0; i <= mfp.last(); i++) {
                    if (bs.readBitFlag()) {
                        mfp.inc(i, bs.readVarSInt());
                    }
                }
                break;
            }

            case NON_TOPO_PENULTIMATE_PLUS_ONE:
                mfp.inc(mfp.last() - 1, 1);
                break;

            case NON_TOPO_COMPLEX_PACK_4: {
                for (var i = 0; i <= mfp.last(); i++) {
                    if (bs.readBitFlag()) {
                        mfp.inc(i, bs.readUBitInt(4) - 7);
                    }
                }
                break;
            }

            case FIELD_PATH_ENCODE_FINISH:
                break;
        }
    }

}
