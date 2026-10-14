#include "audio/audio_analyzer.h"
#include "fft/fft.h"

#include "miniaudio.h"

#include <stdlib.h>
#include <string.h>

struct AudioAnalyzer {
    int fft_size;
    FFT fft;
    float *circular_buffer;
    int write_position;
    float *waveform_snapshot;
    float *spectrum_snapshot;
    ma_mutex lock;
};

static void on_audio_data(void *user_data, const float *frames,
                          unsigned int frame_count, unsigned int channels) {
    AudioAnalyzer *analyzer = user_data;

    ma_mutex_lock(&analyzer->lock);
    for (unsigned int i = 0; i < frame_count; i++) {
        // Mono or average stereo
        float sample = 0;
        for (unsigned int ch = 0; ch < channels; ch++) {
            sample += frames[i * channels + ch];
        }
        analyzer->circular_buffer[analyzer->write_position] = sample / (float) channels;
        analyzer->write_position = (analyzer->write_position + 1) % analyzer->fft_size;
    }
    ma_mutex_unlock(&analyzer->lock);
}

AudioAnalyzer *audio_analyzer_create(int fft_size) {
    AudioAnalyzer *analyzer = calloc(1, sizeof(AudioAnalyzer));
    if (analyzer == NULL) return NULL;

    if (fft_init(&analyzer->fft, fft_size) != 0) {
        free(analyzer);
        return NULL;
    }

    analyzer->fft_size = fft_size;
    analyzer->circular_buffer = calloc(fft_size, sizeof(float));
    analyzer->waveform_snapshot = calloc(fft_size, sizeof(float));
    analyzer->spectrum_snapshot = calloc(fft_size / 2, sizeof(float));

    if (!analyzer->circular_buffer || !analyzer->waveform_snapshot || !analyzer->spectrum_snapshot ||
        ma_mutex_init(&analyzer->lock) != MA_SUCCESS) {
        fft_free(&analyzer->fft);
        free(analyzer->circular_buffer);
        free(analyzer->waveform_snapshot);
        free(analyzer->spectrum_snapshot);
        free(analyzer);
        return NULL;
    }

    return analyzer;
}

void audio_analyzer_destroy(AudioAnalyzer *analyzer) {
    if (analyzer == NULL) return;
    ma_mutex_uninit(&analyzer->lock);
    fft_free(&analyzer->fft);
    free(analyzer->circular_buffer);
    free(analyzer->waveform_snapshot);
    free(analyzer->spectrum_snapshot);
    free(analyzer);
}

AudioDataListener audio_analyzer_listener(AudioAnalyzer *analyzer) {
    AudioDataListener listener = { on_audio_data, analyzer };
    return listener;
}

void audio_analyzer_update(AudioAnalyzer *analyzer) {
    const int size = analyzer->fft_size;

    // Copy buffer in order for analysis
    ma_mutex_lock(&analyzer->lock);
    int start = analyzer->write_position;
    memcpy(analyzer->waveform_snapshot, analyzer->circular_buffer + start, sizeof(float) * (size - start));
    memcpy(analyzer->waveform_snapshot + (size - start), analyzer->circular_buffer, sizeof(float) * start);
    ma_mutex_unlock(&analyzer->lock);

    // Compute FFT outside the lock so the audio thread is never blocked by it
    fft_compute_magnitude(&analyzer->fft, analyzer->waveform_snapshot, analyzer->spectrum_snapshot);
}

void audio_analyzer_reset(AudioAnalyzer *analyzer) {
    ma_mutex_lock(&analyzer->lock);
    memset(analyzer->circular_buffer, 0, sizeof(float) * analyzer->fft_size);
    analyzer->write_position = 0;
    ma_mutex_unlock(&analyzer->lock);

    memset(analyzer->waveform_snapshot, 0, sizeof(float) * analyzer->fft_size);
    memset(analyzer->spectrum_snapshot, 0, sizeof(float) * (analyzer->fft_size / 2));
}

const float *audio_analyzer_get_waveform(const AudioAnalyzer *analyzer) {
    return analyzer->waveform_snapshot;
}

const float *audio_analyzer_get_spectrum(const AudioAnalyzer *analyzer) {
    return analyzer->spectrum_snapshot;
}

int audio_analyzer_get_fft_size(const AudioAnalyzer *analyzer) {
    return analyzer->fft_size;
}
