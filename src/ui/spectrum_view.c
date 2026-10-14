#include "ui/spectrum_view.h"

#include <math.h>
#include <string.h>

#define SMOOTHING 0.8f
#define LOG_POWER 2.5

static void draw_content(BaseView *base) {
    SpectrumView *view = (SpectrumView *) base;
    const float *data = view->spectrum;
    int length = view->spectrum_length;

    if (data == NULL || length == 0) return;

    // Frequency binning
    for (int i = 0; i < SPECTRUM_NUM_BARS; i++) {
        int start_idx, end_idx;

        if (view->linear_scale) {
            // Linear scale: equal frequency width per bar
            start_idx = (int) ((double) i / SPECTRUM_NUM_BARS * length);
            end_idx = (int) ((double) (i + 1) / SPECTRUM_NUM_BARS * length);
        } else {
            // Logarithmic scale: more resolution for low frequencies
            double t0 = (double) i / SPECTRUM_NUM_BARS;
            double t1 = (double) (i + 1) / SPECTRUM_NUM_BARS;
            start_idx = (int) (pow(t0, LOG_POWER) * length);
            end_idx = (int) (pow(t1, LOG_POWER) * length);
        }

        if (start_idx > length - 1) start_idx = length - 1;
        if (start_idx < 0) start_idx = 0;
        if (end_idx > length) end_idx = length;
        if (end_idx < start_idx + 1) end_idx = start_idx + 1;

        float sum = 0;
        for (int j = start_idx; j < end_idx; j++) {
            sum += data[j];
        }
        float bar = sum / (float) (end_idx - start_idx);

        // Apply smoothing
        view->smoothed_bars[i] = SMOOTHING * view->smoothed_bars[i] + (1 - SMOOTHING) * bar;
    }

    // Draw bars
    float x0 = base->bounds.x;
    float y0 = base->bounds.y;
    float width = base->bounds.width;
    float height = base->bounds.height;
    float bar_width = width / SPECTRUM_NUM_BARS;
    float gap = 1;
    float center_y = y0 + height / 2;
    Color highlight = Fade(WHITE, 0.3f);

    for (int i = 0; i < SPECTRUM_NUM_BARS; i++) {
        // Magnitudes are already log-scaled, normalize to 0-1 range
        float normalized = fminf(view->smoothed_bars[i] / 2.5f, 1.0f);
        float x = x0 + i * bar_width + gap / 2;
        float w = bar_width - gap;

        // Dynamic color based on magnitude
        Color bar_color = color_theme_amplitude_color(base->color_theme, normalized, 1.0f, 1.0f, 1.0f);

        if (view->mirrored) {
            float bar_height = normalized * (height / 2 - 5);

            DrawRectangleRec((Rectangle) { x, center_y - bar_height, w, bar_height }, bar_color);
            DrawRectangleRec((Rectangle) { x, center_y, w, bar_height }, bar_color);

            DrawRectangleRec((Rectangle) { x, center_y - bar_height, w, 2 }, highlight);
            DrawRectangleRec((Rectangle) { x, center_y + bar_height - 2, w, 2 }, highlight);
        } else {
            float bar_height = normalized * (height - 10);
            float y = y0 + height - bar_height;

            DrawRectangleRec((Rectangle) { x, y, w, bar_height }, bar_color);
            DrawRectangleRec((Rectangle) { x, y, w, 2 }, highlight);
        }
    }
}

static void draw_grid(BaseView *base) {
    SpectrumView *view = (SpectrumView *) base;
    Rectangle b = base->bounds;
    Color grid = base->color_theme->grid_line_color;

    if (view->mirrored) {
        float center_y = b.y + b.height / 2;
        for (int i = 1; i <= 2; i++) {
            float offset = i * b.height / 5;
            DrawLineV((Vector2) { b.x, center_y - offset }, (Vector2) { b.x + b.width, center_y - offset }, grid);
            DrawLineV((Vector2) { b.x, center_y + offset }, (Vector2) { b.x + b.width, center_y + offset }, grid);
        }
        DrawLineV((Vector2) { b.x, center_y }, (Vector2) { b.x + b.width, center_y },
                  base->color_theme->center_line_color);
    } else {
        for (int i = 1; i <= 4; i++) {
            float y = b.y + b.height - (i * b.height / 5);
            DrawLineV((Vector2) { b.x, y }, (Vector2) { b.x + b.width, y }, grid);
        }
    }
}

static void reset(BaseView *base) {
    SpectrumView *view = (SpectrumView *) base;
    memset(view->smoothed_bars, 0, sizeof(view->smoothed_bars));
}

static const BaseViewVTable SPECTRUM_VTABLE = { draw_content, draw_grid, reset };

void spectrum_view_init(SpectrumView *view) {
    base_view_init(&view->base, &SPECTRUM_VTABLE);
    view->spectrum = NULL;
    view->spectrum_length = 0;
    view->linear_scale = false;
    view->mirrored = false;
    reset(&view->base);
}

void spectrum_view_set_spectrum(SpectrumView *view, const float *spectrum, int length) {
    view->spectrum = spectrum;
    view->spectrum_length = spectrum ? length : 0;
}
