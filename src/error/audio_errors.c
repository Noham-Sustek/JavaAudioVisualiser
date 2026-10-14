#include "error/audio_errors.h"

#include <stdio.h>

void audio_file_error_set(AudioFileError *error, AudioFileErrorType type, const char *file_path, int cause) {
    if (error == NULL) return;
    error->type = type;
    error->cause = cause;
    snprintf(error->file_path, sizeof(error->file_path), "%s", file_path ? file_path : "");
}

void microphone_error_set(MicrophoneError *error, MicrophoneErrorType type, int cause) {
    if (error == NULL) return;
    error->type = type;
    error->cause = cause;
}

const char *audio_file_error_message(AudioFileErrorType type) {
    switch (type) {
        case AUDIO_FILE_OK:                 return "Aucune erreur";
        case AUDIO_FILE_NOT_FOUND:          return "Le fichier n'existe pas";
        case AUDIO_FILE_UNSUPPORTED_FORMAT: return "Format audio non supporte";
        case AUDIO_FILE_CORRUPTED:          return "Le fichier est corrompu ou illisible";
        case AUDIO_FILE_ACCESS_DENIED:      return "Acces au fichier refuse";
        case AUDIO_FILE_DEVICE_UNAVAILABLE: return "Sortie audio indisponible";
        case AUDIO_FILE_UNKNOWN:
        default:                            return "Erreur inconnue";
    }
}

const char *microphone_error_message(MicrophoneErrorType type) {
    switch (type) {
        case MICROPHONE_OK:                 return "Aucune erreur";
        case MICROPHONE_NOT_FOUND:          return "Aucun microphone detecte";
        case MICROPHONE_ACCESS_DENIED:      return "Acces au microphone refuse";
        case MICROPHONE_BUSY:               return "Microphone utilise par une autre application";
        case MICROPHONE_UNSUPPORTED_FORMAT: return "Format audio non supporte par le microphone";
        case MICROPHONE_UNKNOWN:
        default:                            return "Erreur microphone inconnue";
    }
}

const char *microphone_error_name(MicrophoneErrorType type) {
    switch (type) {
        case MICROPHONE_OK:                 return "OK";
        case MICROPHONE_NOT_FOUND:          return "NOT_FOUND";
        case MICROPHONE_ACCESS_DENIED:      return "ACCESS_DENIED";
        case MICROPHONE_BUSY:               return "BUSY";
        case MICROPHONE_UNSUPPORTED_FORMAT: return "UNSUPPORTED_FORMAT";
        case MICROPHONE_UNKNOWN:
        default:                            return "UNKNOWN";
    }
}
