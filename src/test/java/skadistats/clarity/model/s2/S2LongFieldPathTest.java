package skadistats.clarity.model.s2;

import org.testng.annotations.Test;
import skadistats.clarity.ClarityException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class S2LongFieldPathTest {

    @Test
    public void maxIndexAtDepthAccountsForOffset() {
        assertEquals(S2LongFieldPathFormat.maxIndexAtDepth(0), 2046);
        assertEquals(S2LongFieldPathFormat.maxIndexAtDepth(1), 4095);
    }

    @Test
    public void component0RoundTripsAtMax() {
        var id = S2LongFieldPathFormat.set(0L, 0, 2046);
        assertEquals(S2LongFieldPathFormat.get(id, 0), 2046);
        assertTrue(id > 0);
    }

    @Test
    public void component0RejectsOverflow() {
        assertThrows(ClarityException.class, () -> S2LongFieldPathFormat.set(0L, 0, 2047));
    }

    @Test
    public void upperBoundAtMaxIndexIsSentinel() {
        var fp = new S2LongFieldPath(S2LongFieldPathFormat.set(0L, 0, 2046));
        assertEquals(((S2LongFieldPath) fp.upperBoundForSubtreeAt(0)).id(), Long.MAX_VALUE);
    }

    @Test
    public void upperBoundBelowMaxIndexOrdersCorrectly() {
        var below = new S2LongFieldPath(S2LongFieldPathFormat.set(0L, 0, 2045));
        var max = new S2LongFieldPath(S2LongFieldPathFormat.set(0L, 0, 2046));
        assertTrue(below.compareTo(max) < 0);
        assertEquals(below.upperBoundForSubtreeAt(0), max);
    }

}
