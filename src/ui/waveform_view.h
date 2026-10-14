#ifndef UI_WAVEFORM_VIEW_H
#define UI_WAVEFORM_VIEW_H

#include "ui/base_view.h"

#define WAVEFORM_DISPLAY_POINTS 8192

/**
 * Visualization view for audio waveform (time domain).
 * "Extends" BaseView.
 */
typedef struct {
    BaseView base;
    const float *waveform;      // Not owned, NULL when nothing is playing
    int waveform_length;
    float smoothed_waveform[WAVEFORM_DISPLAY_POINTS];
    float peak_envelope;        // Recent peak, drives the automatic gain
} WaveformView;

void waveform_view_init(WaveformView *view);

/** Set the samples to display (not copied), or NULL to clear. */
void waveform_view_set_waveform(WaveformView *view, const float *waveform, int length);

#endif
