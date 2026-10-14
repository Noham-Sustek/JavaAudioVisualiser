#ifndef AUDIO_AUDIO_PLAYER_H
#define AUDIO_AUDIO_PLAYER_H

#include "audio/audio_listener.h"
#include "error/audio_errors.h"

#include <stdbool.h>

/**
 * Loads a WAV file and plays it on the default output device,
 * forwarding every played buffer to the registered listeners.
 */
typedef struct AudioPlayer AudioPlayer;

AudioPlayer *audio_player_create(void);
void audio_player_destroy(AudioPlayer *player);

/** Load a file (stops current playback). On failure fills error and returns false. */
bool audio_player_load_file(AudioPlayer *player, const char *path, AudioFileError *error);

void audio_player_add_listener(AudioPlayer *player, AudioDataListener listener);

void audio_player_play(AudioPlayer *player);
void audio_player_pause(AudioPlayer *player);
void audio_player_stop(AudioPlayer *player);

/** Seek to a position in [0, 1]. */
void audio_player_seek_to(AudioPlayer *player, double progress);

bool audio_player_is_loaded(const AudioPlayer *player);
bool audio_player_is_playing(const AudioPlayer *player);
bool audio_player_is_paused(const AudioPlayer *player);

/** Returns true when playback reached the end of the file. */
bool audio_player_poll_finished(AudioPlayer *player);

double audio_player_get_progress(const AudioPlayer *player);
double audio_player_get_current_time_seconds(const AudioPlayer *player);
double audio_player_get_total_duration_seconds(const AudioPlayer *player);

#endif
