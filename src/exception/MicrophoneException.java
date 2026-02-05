package exception;

/**
 * Custom exception for microphone related errors.
 * Thrown when the microphone cannot be accessed or used.
 */
public class MicrophoneException extends Exception {

    private final ErrorType errorType;

    /**
     * Types of microphone errors.
     */
    public enum ErrorType {
        NOT_FOUND("Aucun microphone detecte"),
        ACCESS_DENIED("Acces au microphone refuse"),
        BUSY("Microphone utilise par une autre application"),
        UNSUPPORTED_FORMAT("Format audio non supporte par le microphone"),
        UNKNOWN("Erreur microphone inconnue");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Create a new MicrophoneException.
     *
     * @param errorType the type of error
     */
    public MicrophoneException(ErrorType errorType) {
        super(errorType.getDescription());
        this.errorType = errorType;
    }

    /**
     * Create a new MicrophoneException with a cause.
     *
     * @param errorType the type of error
     * @param cause     the underlying cause
     */
    public MicrophoneException(ErrorType errorType, Throwable cause) {
        super(errorType.getDescription(), cause);
        this.errorType = errorType;
    }

    /**
     * Get the type of error.
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * Get a user-friendly error message in French.
     */
    public String getUserMessage() {
        return errorType.getDescription();
    }
}
