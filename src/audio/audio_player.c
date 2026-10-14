#include "audio/audio_player.h"
#include "util/logger.h"

#include "miniaudio.h"

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_LISTENERS 4

typedef enum {
    PLAYER_STOPPED,
    PLAYER_PLAYING,
    PLAYER_PAUSED
} PlayerState;

struct AudioPlayer {
    ma_decoder decoder;
    ma_device device;
    ma_mutex lock;          // Guards the decoder between UI and audio threads
    bool loaded;

    char file_name[256];
    ma_uint32 channels;
    ma_uint32 sample_rate;
    ma_uint64 total_frames;

    volatile PlayerState state;
    volatile ma_uint64 frames_played;
    volatile bool finished;

    AudioDataListener listeners[MAX_LISTENERS];
    int listener_count;
};

static const char *base_name(const char *path) {
    const char *slash = strrchr(path, '/');
    const char *backslash = strrchr(path, '\\');
    if (backslash > slash) slash = backslash;
    return slash ? slash + 1 : path;
}

// Audio thread: read from the decoder and forward to listeners
static void data_callback(ma_device *device, void *output, const void *input, ma_uint32 frame_count) {
    (void) input;
    AudioPlayer *player = device->pUserData;
    ma_uint64 frames_read = 0;

    ma_mutex_lock(&player->lock);
    if (player->state == PLAYER_PLAYING && !player->finished) {
        ma_decoder_read_pcm_frames(&player->decoder, output, frame_count, &frames_read);
        player->frames_played += frames_read;
        if (frames_read < frame_count) {
            player->finished = true;
        }
    }
    ma_mutex_unlock(&player->lock);

    if (frames_read > 0) {
        for (int i = 0; i < player->listener_count; i++) {
            player->listeners[i].on_audio_data(player->listeners[i].user_data, output,
                                               (unsigned int) frames_read, player->channels);
        }
    }
}

AudioPlayer *audio_player_create(void) {
    AudioPlayer *player = calloc(1, sizeof(AudioPlayer));
    if (player == NULL) return NULL;

    if (ma_mutex_init(&player->lock) != MA_SUCCESS) {
        free(player);
        return NULL;
    }
    return player;
}

static void unload(AudioPlayer *player) {
    if (!player->loaded) return;
    ma_device_uninit(&player->device);
    ma_decoder_uninit(&player->decoder);
    player->loaded = false;
}

void audio_player_destroy(AudioPlayer *player) {
    if (player == NULL) return;
    unload(player);
    ma_mutex_uninit(&player->lock);
    free(player);
    log_info("AudioPlayer", "Audio player closed");
}

bool audio_player_load_file(AudioPlayer *player, const char *path, AudioFileError *error) {
    audio_player_stop(player);
    unload(player);

    // Validate the file before handing it to the decoder
    FILE *file = fopen(path, "rb");
    if (file == NULL) {
        int err = errno;
        if (err == EACCES) {
            log_error("AudioPlayer", "Cannot read file: %s", path);
            audio_file_error_set(error, AUDIO_FILE_ACCESS_DENIED, path, err);
        } else {
            log_error("AudioPlayer", "File not found: %s", path);
            audio_file_error_set(error, AUDIO_FILE_NOT_FOUND, path, err);
        }
        return false;
    }
    fclose(file);

    // Decode to 32-bit float, keeping the file's channels and sample rate
    ma_decoder_config decoder_config = ma_decoder_config_init(ma_format_f32, 0, 0);
    decoder_config.encodingFormat = ma_encoding_format_wav;
    ma_result result = ma_decoder_init_file(path, &decoder_config, &player->decoder);
    if (result != MA_SUCCESS) {
        if (result == MA_INVALID_FILE || result == MA_FORMAT_NOT_SUPPORTED || result == MA_NOT_IMPLEMENTED) {
            log_error("AudioPlayer", "Unsupported audio format - %s", ma_result_description(result));
            audio_file_error_set(error, AUDIO_FILE_UNSUPPORTED_FORMAT, path, result);
        } else {
            log_error("AudioPlayer", "Error reading file - %s", ma_result_description(result));
            audio_file_error_set(error, AUDIO_FILE_CORRUPTED, path, result);
        }
        return false;
    }

    player->channels = player->decoder.outputChannels;
    player->sample_rate = player->decoder.outputSampleRate;
    if (ma_decoder_get_length_in_pcm_frames(&player->decoder, &player->total_frames) != MA_SUCCESS) {
        player->total_frames = 0;
    }

    ma_device_config device_config = ma_device_config_init(ma_device_type_playback);
    device_config.playback.format = ma_format_f32;
    device_config.playback.channels = player->channels;
    device_config.sampleRate = player->sample_rate;
    device_config.periodSizeInFrames = 512;
    device_config.dataCallback = data_callback;
    device_config.pUserData = player;

    result = ma_device_init(NULL, &device_config, &player->device);
    if (result != MA_SUCCESS) {
        log_error("AudioPlayer", "Audio line unavailable - %s", ma_result_description(result));
        ma_decoder_uninit(&player->decoder);
        audio_file_error_set(error, AUDIO_FILE_DEVICE_UNAVAILABLE, path, result);
        return false;
    }

    snprintf(player->file_name, sizeof(player->file_name), "%s", base_name(path));
    player->state = PLAYER_STOPPED;
    player->frames_played = 0;
    player->finished = false;
    player->loaded = true;

    log_info("AudioPlayer", "Loaded file: %s (Format: %uHz, %u channels)",
             player->file_name, player->sample_rate, player->channels);
    return true;
}

void audio_player_add_listener(AudioPlayer *player, AudioDataListener listener) {
    if (player->listener_count < MAX_LISTENERS) {
        player->listeners[player->listener_count++] = listener;
    }
}

void audio_player_play(AudioPlayer *player) {
    if (!player->loaded || player->state == PLAYER_PLAYING) return;

    bool resumed = player->state == PLAYER_PAUSED;
    player->state = PLAYER_PLAYING;

    if (ma_device_start(&player->device) != MA_SUCCESS) {
        log_error("AudioPlayer", "Could not start audio device");
        player->state = PLAYER_STOPPED;
        return;
    }

    if (resumed) {
        log_debug("AudioPlayer", "Resumed playback");
    } else {
        log_info("AudioPlayer", "Started playback: %s", player->file_name);
    }
}

void audio_player_pause(AudioPlayer *player) {
    if (player->state != PLAYER_PLAYING) return;

    player->state = PLAYER_PAUSED;
    ma_device_stop(&player->device);
    log_debug("AudioPlayer", "Paused playback");
}

void audio_player_stop(AudioPlayer *player) {
    if (!player->loaded) return;

    player->state = PLAYER_STOPPED;
    if (ma_device_is_started(&player->device)) {
        ma_device_stop(&player->device);
    }

    ma_mutex_lock(&player->lock);
    ma_decoder_seek_to_pcm_frame(&player->decoder, 0);
    player->frames_played = 0;
    player->finished = false;
    ma_mutex_unlock(&player->lock);

    log_debug("AudioPlayer", "Stopped playback");
}

void audio_player_seek_to(AudioPlayer *player, double progress) {
    if (!player->loaded || player->total_frames == 0) return;

    if (progress < 0) progress = 0;
    if (progress > 1) progress = 1;
    ma_uint64 target = (ma_uint64) (progress * (double) player->total_frames);

    ma_mutex_lock(&player->lock);
    if (ma_decoder_seek_to_pcm_frame(&player->decoder, target) == MA_SUCCESS) {
        player->frames_played = target;
        player->finished = false;
    }
    ma_mutex_unlock(&player->lock);

    log_debug("AudioPlayer", "Seeking to %.1f%%", progress * 100);
}

bool audio_player_is_loaded(const AudioPlayer *player) {
    return player->loaded;
}

bool audio_player_is_playing(const AudioPlayer *player) {
    return player->state == PLAYER_PLAYING;
}

bool audio_player_is_paused(const AudioPlayer *player) {
    return player->state == PLAYER_PAUSED;
}

bool audio_player_poll_finished(AudioPlayer *player) {
    if (player->state == PLAYER_PLAYING && player->finished) {
        log_debug("AudioPlayer", "Playback completed");
        return true;
    }
    return false;
}

double audio_player_get_progress(const AudioPlayer *player) {
    if (player->total_frames == 0) return 0.0;
    return (double) player->frames_played / (double) player->total_frames;
}

double audio_player_get_current_time_seconds(const AudioPlayer *player) {
    if (!player->loaded) return 0.0;
    return (double) player->frames_played / player->sample_rate;
}

double audio_player_get_total_duration_seconds(const AudioPlayer *player) {
    if (!player->loaded) return 0.0;
    return (double) player->total_frames / player->sample_rate;
}
