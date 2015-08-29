package skadistats.clarity.decoder.unpacker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.Util;

public class FloatDefaultUnpacker implements Unpacker<Float> {

    private static final int QFE_ROUNDDOWN = 0x1;
    private static final int QFE_ROUNDUP = 0x2;
    private static final int QFE_ENCODE_ZERO_EXACTLY = 0x4;
    private static final int QFE_ENCODE_INTEGERS_EXACTLY = 0x8;
    private static final int BITNUM_ENCODE_BIT_COUNT = 3;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String m_pFieldName;
    private int m_nBitCount;
    private float m_flMinValue;
    private float m_flMaxValue;
    private int m_nFlags;
    private int m_nEncodeFlags;

    private float m_flHighLowMul;
    private float m_flDecodeMul;

    public FloatDefaultUnpacker(String m_pFieldName, int m_nBitCount, int m_nFlags, float m_flMinValue, float m_flMaxValue) {
        this.m_pFieldName = m_pFieldName;
        this.m_nBitCount = m_nBitCount;
        this.m_flMinValue = m_flMinValue;
        this.m_flMaxValue = m_flMaxValue;
        this.m_nFlags = m_nFlags;
        this.m_nEncodeFlags = validateFlags(this.m_nFlags);
        initialize();
        if (this.m_nFlags != this.m_nEncodeFlags) {
            log.info("Flags changed for Field {}, [{}->{}]", m_pFieldName, this.m_nFlags, this.m_nEncodeFlags);
        }
    }

    private void initialize() {
        float m_flOffset;
        int nQuanta = (1 << m_nBitCount);

        if ((m_nFlags & (QFE_ROUNDDOWN | QFE_ROUNDUP)) == (QFE_ROUNDDOWN | QFE_ROUNDUP)) {
            log.warn("Field {} was flagged to both round up and down, these m_nFlags are mutually exclusive [{}->{}]\n", m_pFieldName, m_flMinValue, m_flMaxValue);
        }

        if ((m_nFlags & QFE_ROUNDDOWN) != 0) {
            float flFullRange = m_flMaxValue - m_flMinValue;
            m_flOffset = (flFullRange / nQuanta);
            m_flMaxValue -= m_flOffset;
        } else if ((m_nFlags & QFE_ROUNDUP) != 0) {
            float flFullRange = m_flMaxValue - m_flMinValue;
            m_flOffset = (flFullRange / nQuanta);
            m_flMinValue += m_flOffset;
        }

        if ((m_nFlags & QFE_ENCODE_INTEGERS_EXACTLY) != 0) {
            int delta = ((int) m_flMinValue) - ((int) m_flMaxValue);
            int nTrueRange = (1 << Util.calcBitsNeededFor(Math.max(delta, 1)));

            int nBits = this.m_nBitCount;
            while ((1 << nBits) < nTrueRange) {
                ++nBits;
            }
            if (nBits > m_nBitCount) {
                log.warn("Field {} was flagged QFE_ENCODE_INTEGERS_EXACTLY, but didn't specify enough bits, upping bitcount from {}to {} for range [{}->{}]", m_pFieldName, m_nBitCount, nBits, m_flMinValue, m_flMaxValue);
                m_nBitCount = nBits;
                nQuanta = (1 << m_nBitCount);
            }

            float flTrueRange = (float) nTrueRange;
            m_flOffset = (flTrueRange / (float) nQuanta);
            m_flMaxValue = m_flMinValue + flTrueRange - m_flOffset;
        }

        if (!assignMultipliers(nQuanta)) {
            throw new RuntimeException("oops, assignMultipliers failed.");
        }

        // On the wire we might not need special bits
        for (int bit = 0; bit < BITNUM_ENCODE_BIT_COUNT; bit++) {
            if ((m_nEncodeFlags & (1 << bit)) != 0) {
                float test = GetExactEncodeTestCase(bit);
                if (QuantizedFloatIsUnchanged(test)) {
                    m_nEncodeFlags &= ~(1 << bit);
                }
            }
        }

    }

    private float GetExactEncodeTestCase(int bit) {
        int nFlags = (1 << bit);

        if ((nFlags & QFE_ROUNDDOWN) != 0)
            return m_flMinValue;
        if ((nFlags & QFE_ROUNDUP) != 0)
            return m_flMaxValue;
        if ((nFlags & QFE_ENCODE_ZERO_EXACTLY) != 0)
            return 0.0f;
        throw new RuntimeException("oooops!");
    }


    private float QuantizeFloat(float flValue) {
        boolean bValidate = true;

        int ulVal;
        if (flValue < m_flMinValue) {
            // clamp < 0
            ulVal = 0;

            if (bValidate && (m_nFlags & QFE_ROUNDUP) == 0) {
                log.warn("Field {} tried to quantize an out-of-range value ({}, range is {}->{}), clamping.", m_pFieldName, flValue, m_flMinValue, m_flMaxValue);
            }
            return m_flMinValue;
        } else if (flValue > m_flMaxValue) {
            // clamp > 1
            ulVal = ((1 << m_nBitCount) - 1);

            if (bValidate && (m_nFlags & QFE_ROUNDDOWN) == 0) {
                log.warn("Field {} tried to quantize an out-of-range value ({}, range is {}->{}) clamping.", m_pFieldName, flValue, m_flMinValue, m_flMaxValue);
            }
            return m_flMaxValue;
        }

        // Actually quantize
        float fRangeVal = (flValue - m_flMinValue) * m_flHighLowMul;
        ulVal = (int) fRangeVal;
        float out = (float) ulVal * m_flDecodeMul;
        out = m_flMinValue + (m_flMaxValue - m_flMinValue) * out;
        return out;
    }


    private boolean QuantizedFloatIsUnchanged(float flInput) {
        float flOutput;
        //QuantizeFloat< true, false >( flInput, &flOutput );
        return flInput == QuantizeFloat(flInput);
    }


    private boolean assignMultipliers(int nQuanta) {
        m_flHighLowMul = AssignRangeMultiplier(m_nBitCount, m_flMaxValue - m_flMinValue);
        m_flDecodeMul = 1.0f / (nQuanta - 1);
        return (m_flHighLowMul != 0.0f);
    }

    private boolean CloseEnough(float a, float b) {
        return Math.abs(a - b) <= 0.001;
    }

    private float AssignRangeMultiplier(int nBits, float range) {
        int iHighValue;
        if (nBits == 32) {
            iHighValue = 0xFFFFFFFE;
        } else {
            iHighValue = ((1 << nBits) - 1);
        }

        float fHighLowMul;
        if (CloseEnough(range, 0.0f)) {
            fHighLowMul = (float) iHighValue;
        } else {
            fHighLowMul = iHighValue / range;
        }

        // If the precision is messing us up, then adjust it so it won't.
        if ((int) (fHighLowMul * range) > iHighValue || (fHighLowMul * range) > (double) iHighValue) {
            // Squeeze it down smaller and smaller until it's going to produce an integer
            // in the valid range when given the highest value.
            float multipliers[] = {0.9999f, 0.99f, 0.9f, 0.8f, 0.7f};
            int i;
            for (i = 0; i < multipliers.length; i++) {
                fHighLowMul = (float) (iHighValue / range) * multipliers[i];
                if ((int) (fHighLowMul * range) > iHighValue || (fHighLowMul * range) > (double) iHighValue)
                    continue;
                break;
            }

            if (i == multipliers.length) {
                throw new RuntimeException("Doh! We seem to be unable to represent this range.");
            }
        }

        return fHighLowMul;
    }


    private int validateFlags(int nFlags) {
        // If the min or max value is exactly zero and we are encoding min or max exactly, then don't need zero flag
        if ((m_flMinValue == 0.0f && (nFlags & QFE_ROUNDDOWN) != 0) || (m_flMaxValue == 0.0f && (nFlags & QFE_ROUNDUP) != 0)) {
            nFlags &= ~QFE_ENCODE_ZERO_EXACTLY;
        }

        // If specified encode zero but min or max actual value is zero, then convert that encode directive to be encode min or max exactly instead
        if (m_flMinValue == 0.0f && (nFlags & QFE_ENCODE_ZERO_EXACTLY) != 0) {
            nFlags |= QFE_ROUNDDOWN;
            nFlags &= ~QFE_ENCODE_ZERO_EXACTLY;
        }
        if (m_flMaxValue == 0.0f && (nFlags & QFE_ENCODE_ZERO_EXACTLY) != 0) {
            nFlags |= QFE_ROUNDUP;
            nFlags &= ~QFE_ENCODE_ZERO_EXACTLY;
        }

        // If the range doesn't span across zero, then also don't need the zero flag
        boolean bActuallyNeedToTestZero = (m_flMinValue < 0.0f && m_flMaxValue > 0.0f);
        if (!bActuallyNeedToTestZero) {
            if ((nFlags & QFE_ENCODE_ZERO_EXACTLY) != 0) {
                log.warn("Field {} was flagged to encode zero exactly, but min/max range doesn't span zero [{}->{}]", m_pFieldName, m_flMinValue, m_flMaxValue);
            }
            nFlags &= ~QFE_ENCODE_ZERO_EXACTLY;
        }

        if ((nFlags & QFE_ENCODE_INTEGERS_EXACTLY) != 0) {
            // Wipes out all other falgs
            nFlags &= ~(QFE_ROUNDUP | QFE_ROUNDDOWN | QFE_ENCODE_ZERO_EXACTLY);
        }

        return nFlags;
    }


    @Override
    public Float unpack(BitStream bs) {
        if ((m_nEncodeFlags & 0x1) != 0 && bs.readBitFlag()) {
            return m_flMinValue;
        }
        if ((m_nEncodeFlags & 0x2) != 0 && bs.readBitFlag()) {
            return m_flMaxValue;
        }
        if ((m_nEncodeFlags & 0x4) != 0 && bs.readBitFlag()) {
            return 0.0f;
        }
        float v = bs.readUBitInt(m_nBitCount) * m_flDecodeMul;
        return m_flMinValue + ( m_flMaxValue - m_flMinValue ) * v;
    }

}
