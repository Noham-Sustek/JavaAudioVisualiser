#ifndef AUDIO_AUDIO_LISTENER_H
#define AUDIO_AUDIO_LISTENER_H

/**
 * Receives audio as it is played or captured.
 * frames holds frame_count interleaved float frames in [-1, 1].
 *
 * Called from the audio thread: implementations must be fast and thread-safe.
 */
typedef void (*AudioDataCallback)(void *user_data, const float *frames,
                                  unsigned int frame_count, unsigned int channels);

typedef struct {
    AudioDataCallback on_audio_data;
    void *user_data;
} AudioDataListener;

#endif
