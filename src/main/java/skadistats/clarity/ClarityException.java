package skadistats.clarity;

public class ClarityException extends RuntimeException {

    public ClarityException(Exception cause, String format, Object... parameters) {
        super(String.format(format, parameters), cause);
    }

    public ClarityException(String format, Object... parameters) {
        super(String.format(format, parameters));
    }

    public ClarityException(Throwable cause) {
        super(cause);
    }

}
