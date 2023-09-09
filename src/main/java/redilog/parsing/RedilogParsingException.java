package redilog.parsing;

public class RedilogParsingException extends Exception {
    public RedilogParsingException() {
    }

    public RedilogParsingException(String message) {
        super(message);
    }

    public RedilogParsingException(Throwable cause) {
        super(cause);
    }

    public RedilogParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
