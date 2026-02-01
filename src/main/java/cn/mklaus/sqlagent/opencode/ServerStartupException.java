package cn.mklaus.sqlagent.opencode;

/**
 * Exception thrown when OpenCode server fails to start
 */
public class ServerStartupException extends Exception {
    private final Reason reason;

    public enum Reason {
        NOT_INSTALLED,
        STARTUP_FAILED,
        TIMEOUT,
        PORT_IN_USE
    }

    public ServerStartupException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    public ServerStartupException(String message, Reason reason, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
