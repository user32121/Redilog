package redilog.routing;

public class RedilogPlacementException extends Exception {
    public RedilogPlacementException() {
    }

    public RedilogPlacementException(String message) {
        super(message);
    }

    public RedilogPlacementException(Throwable cause) {
        super(cause);
    }

    public RedilogPlacementException(String message, Throwable cause) {
        super(message, cause);
    }
}
