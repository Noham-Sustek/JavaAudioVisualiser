#ifndef AUDIO_AUDIO_ANALYZER_H
#define AUDIO_AUDIO_ANALYZER_H

#include "audio/audio_listener.h"

/**
 * Accumulates incoming audio in a circular buffer (audio thread),
 * and turns it into a waveform + spectrum snapshot on demand (UI thread).
 */
typedef struct AudioAnalyzer AudioAnalyzer;

AudioAnalyzer *audio_analyzer_create(int fft_size);
void audio_analyzer_destroy(AudioAnalyzer *analyzer);

/** Listener to register on a player or microphone. */
AudioDataListener audio_analyzer_listener(AudioAnalyzer *analyzer);

/**
 * Take a snapshot of the last fft_size samples and run the FFT.
 * Results are available through get_waveform / get_spectrum.
 */
void audio_analyzer_update(AudioAnalyzer *analyzer);

/** Clear the circular buffer and snapshots. */
void audio_analyzer_reset(AudioAnalyzer *analyzer);

/** Waveform snapshot: fft_size samples. Owned by the analyzer. */
const float *audio_analyzer_get_waveform(const AudioAnalyzer *analyzer);

/** Spectrum snapshot: fft_size / 2 magnitudes. Owned by the analyzer. */
const float *audio_analyzer_get_spectrum(const AudioAnalyzer *analyzer);

int audio_analyzer_get_fft_size(const AudioAnalyzer *analyzer);

#endif
