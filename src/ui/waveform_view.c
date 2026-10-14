#include "ui/waveform_view.h"

#include <math.h>
#include <string.h>

#define SMOOTHING 0.9f

static void draw_content(BaseView *base) {
    WaveformView *view = (WaveformView *) base;
    const float *samples = view->waveform;
    int length = view->waveform_length;

    if (samples == NULL || length == 0) return;

    float x0 = base->bounds.x;
    float width = base->bounds.width;
    float height = base->bounds.height;
    float mid_y = base->bounds.y + height / 2;
    float half = height / 2 - 10;

    // Downsample to fixed number of points mixing average and peak
    int samples_per_point = length / WAVEFORM_DISPLAY_POINTS;
    if (samples_per_point < 1) samples_per_point = 1;

    for (int i = 0; i < WAVEFORM_DISPLAY_POINTS; i++) {
        int start = i * samples_per_point;
        int end = start + samples_per_point;
        if (end > length) end = length;

        float value = 0;
        if (start < end) {
            float max = 0;
            float sum = 0;
            for (int j = start; j < end; j++) {
                sum += samples[j];
                if (fabsf(samples[j]) > fabsf(max)) max = samples[j];
            }
            // Mix average and peak for smooth but responsive display
            float avg = sum / (float) (end - start);
            value = avg * 0.3f + max * 0.7f;
        }

        // Apply temporal smoothing
        view->smoothed_waveform[i] = SMOOTHING * view->smoothed_waveform[i] + (1 - SMOOTHING) * value;
    }

    // Draw filled bars with hue based on amplitude
    float bar_width = width / WAVEFORM_DISPLAY_POINTS;
    for (int i = 0; i < WAVEFORM_DISPLAY_POINTS; i++) {
        float amplitude = fabsf(view->smoothed_waveform[i]);
        float normalized = fminf(amplitude, 1.0f);
        Color fill = color_theme_amplitude_color(base->color_theme, normalized, 0.8f, 0.9f, 0.4f);

        float bar_height = amplitude * half;
        DrawRectangleRec((Rectangle) { x0 + i * bar_width, mid_y - bar_height, bar_width, bar_height * 2 }, fill);
    }

    // Draw waveform line segments with hue based on amplitude
    for (int i = 0; i < WAVEFORM_DISPLAY_POINTS - 1; i++) {
        Vector2 p1 = { x0 + (float) i / WAVEFORM_DISPLAY_POINTS * width,
                       mid_y - view->smoothed_waveform[i] * half };
        Vector2 p2 = { x0 + (float) (i + 1) / WAVEFORM_DISPLAY_POINTS * width,
                       mid_y - view->smoothed_waveform[i + 1] * half };

        float normalized = fminf(fabsf(view->smoothed_waveform[i]), 1.0f);
        Color line = color_theme_amplitude_color(base->color_theme, normalized, 1.0f, 1.0f, 1.0f);
        DrawLineEx(p1, p2, 2.0f, line);
    }
}

static void draw_grid(BaseView *base) {
    Rectangle b = base->bounds;
    float mid_y = b.y + b.height / 2;
    Color color = base->color_theme->grid_line_color;

    DrawLineV((Vector2) { b.x, mid_y }, (Vector2) { b.x + b.width, mid_y }, color);
    DrawLineV((Vector2) { b.x, mid_y - b.height / 4 }, (Vector2) { b.x + b.width, mid_y - b.height / 4 }, color);
    DrawLineV((Vector2) { b.x, mid_y + b.height / 4 }, (Vector2) { b.x + b.width, mid_y + b.height / 4 }, color);
}

static void reset(BaseView *base) {
    WaveformView *view = (WaveformView *) base;
    memset(view->smoothed_waveform, 0, sizeof(view->smoothed_waveform));
}

static const BaseViewVTable WAVEFORM_VTABLE = { draw_content, draw_grid, reset };

void waveform_view_init(WaveformView *view) {
    base_view_init(&view->base, &WAVEFORM_VTABLE);
    view->waveform = NULL;
    view->waveform_length = 0;
    reset(&view->base);
}

void waveform_view_set_waveform(WaveformView *view, const float *waveform, int length) {
    view->waveform = waveform;
    view->waveform_length = waveform ? length : 0;
}
