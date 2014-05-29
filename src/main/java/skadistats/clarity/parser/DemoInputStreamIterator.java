package skadistats.clarity.parser;

/**
 * @deprecated
 * this has been renamed to PeekIterator in 1.1, since there are now multiple ways to iterator a DemoInputStream
 */
@Deprecated
public class DemoInputStreamIterator extends PeekIterator {

    public DemoInputStreamIterator(DemoInputStream s) {
        super(s);
    }

}
