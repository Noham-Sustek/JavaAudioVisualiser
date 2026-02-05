package exception;

/**
 * Custom exception for audio file related errors.
 * Thrown when an audio file cannot be loaded, parsed, or played.
 */
public class AudioFileException extends Exception {

    private final ErrorType errorType;
    private final String filePath;

    /**
     * Types of audio file errors.
     */
    public enum ErrorType {
        FILE_NOT_FOUND("Le fichier n'existe pas"),
        UNSUPPORTED_FORMAT("Format audio non supporte"),
        CORRUPTED_FILE("Le fichier est corrompu ou illisible"),
        ACCESS_DENIED("Acces au fichier refuse"),
        UNKNOWN("Erreur inconnue");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Create a new AudioFileException.
     *
     * @param errorType the type of error
     * @param filePath  the path to the problematic file
     */
    public AudioFileException(ErrorType errorType, String filePath) {
        super(errorType.getDescription() + ": " + filePath);
        this.errorType = errorType;
        this.filePath = filePath;
    }

    /**
     * Create a new AudioFileException with a cause.
     *
     * @param errorType the type of error
     * @param filePath  the path to the problematic file
     * @param cause     the underlying cause
     */
    public AudioFileException(ErrorType errorType, String filePath, Throwable cause) {
        super(errorType.getDescription() + ": " + filePath, cause);
        this.errorType = errorType;
        this.filePath = filePath;
    }

    /**
     * Create a new AudioFileException with a custom message.
     *
     * @param errorType the type of error
     * @param filePath  the path to the problematic file
     * @param message   custom error message
     */
    public AudioFileException(ErrorType errorType, String filePath, String message) {
        super(message);
        this.errorType = errorType;
        this.filePath = filePath;
    }

    /**
     * Get the type of error.
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * Get the file path that caused the error.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Get a user-friendly error message in French.
     */
    public String getUserMessage() {
        return errorType.getDescription();
    }
}
