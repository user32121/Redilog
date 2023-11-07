package redilog.synthesis;

public class RedilogSynthesisException extends Exception {
    public RedilogSynthesisException() {
    }

    public RedilogSynthesisException(String message) {
        super(message);
    }

    public RedilogSynthesisException(Throwable cause) {
        super(cause);
    }

    public RedilogSynthesisException(String message, Throwable cause) {
        super(message, cause);
    }
}
