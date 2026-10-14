#ifndef UI_SPECTRUM_VIEW_H
#define UI_SPECTRUM_VIEW_H

#include "ui/base_view.h"

#include <stdbool.h>

#define SPECTRUM_NUM_BARS 256

/**
 * Visualization view for audio spectrum (frequency domain).
 * "Extends" BaseView.
 */
typedef struct {
    BaseView base;
    const float *spectrum;      // Not owned, NULL when nothing is playing
    int spectrum_length;
    float smoothed_bars[SPECTRUM_NUM_BARS];
    bool linear_scale;
    bool mirrored;
} SpectrumView;

void spectrum_view_init(SpectrumView *view);

/** Set the magnitudes to display (not copied), or NULL to clear. */
void spectrum_view_set_spectrum(SpectrumView *view, const float *spectrum, int length);

#endif
