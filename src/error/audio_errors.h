#ifndef ERROR_AUDIO_ERRORS_H
#define ERROR_AUDIO_ERRORS_H

/**
 * Error reporting for audio files and the microphone.
 * C counterpart of the Java AudioFileException / MicrophoneException:
 * functions that can fail fill one of these structs and return false.
 */

typedef enum {
    AUDIO_FILE_OK = 0,
    AUDIO_FILE_NOT_FOUND,
    AUDIO_FILE_UNSUPPORTED_FORMAT,
    AUDIO_FILE_CORRUPTED,
    AUDIO_FILE_ACCESS_DENIED,
    AUDIO_FILE_DEVICE_UNAVAILABLE,
    AUDIO_FILE_UNKNOWN
} AudioFileErrorType;

typedef struct {
    AudioFileErrorType type;
    char file_path[1024];
    int cause;  // Underlying miniaudio result code or errno, 0 if none
} AudioFileError;

typedef enum {
    MICROPHONE_OK = 0,
    MICROPHONE_NOT_FOUND,
    MICROPHONE_ACCESS_DENIED,
    MICROPHONE_BUSY,
    MICROPHONE_UNSUPPORTED_FORMAT,
    MICROPHONE_UNKNOWN
} MicrophoneErrorType;

typedef struct {
    MicrophoneErrorType type;
    int cause;  // Underlying miniaudio result code, 0 if none
} MicrophoneError;

void audio_file_error_set(AudioFileError *error, AudioFileErrorType type, const char *file_path, int cause);
void microphone_error_set(MicrophoneError *error, MicrophoneErrorType type, int cause);

/** User-friendly error messages in French. */
const char *audio_file_error_message(AudioFileErrorType type);
const char *microphone_error_message(MicrophoneErrorType type);

/** Enum names, for logs. */
const char *microphone_error_name(MicrophoneErrorType type);

#endif
