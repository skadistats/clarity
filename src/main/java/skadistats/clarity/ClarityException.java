package skadistats.clarity;

/**
 * Unchecked exception for errors raised by the parser, such as inconsistent replay data or invalid processor setup.
 * Messages are built with {@link String#format(String, Object...)}.
 */
public class ClarityException extends RuntimeException {

    /**
     * @param cause the underlying cause
     * @param format format string for the message
     * @param parameters format arguments
     */
    public ClarityException(Exception cause, String format, Object... parameters) {
        super(String.format(format, parameters), cause);
    }

    /**
     * @param format format string for the message
     * @param parameters format arguments
     */
    public ClarityException(String format, Object... parameters) {
        super(String.format(format, parameters));
    }

    /**
     * @param cause the underlying cause
     */
    public ClarityException(Throwable cause) {
        super(cause);
    }

}
