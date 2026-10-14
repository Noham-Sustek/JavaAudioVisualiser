#ifndef UI_COLOR_THEME_H
#define UI_COLOR_THEME_H

#include "raylib.h"

typedef enum {
    THEME_DARK_BLUE,
    THEME_OCEAN,
    THEME_SUNSET,
    THEME_FOREST,
    THEME_PURPLE,
    THEME_MONOCHROME,
    THEME_SYNTHWAVE,
    THEME_EVA_01,
    THEME_FIRE_ICE,
    THEME_COUNT
} ThemeType;

typedef struct {
    ThemeType type;
    const char *id;             // Stable name stored in the settings file
    const char *display_name;
    Color background_color;
    Color canvas_background;
    Color grid_line_color;
    Color center_line_color;
    Color button_background;
    Color text_color;
    Color secondary_text_color;
    float hue_start;            // Starting hue for amplitude coloring (0-1)
    float hue_end;              // Ending hue for amplitude coloring (0-1)
} ColorTheme;

const ColorTheme *color_theme_get(ThemeType type);

/** Look up a theme by id ("DARK_BLUE"...); returns default_type if unknown. */
ThemeType color_theme_parse(const char *id, ThemeType default_type);

/** Look up a theme by display name; returns default_type if unknown. */
ThemeType color_theme_find_by_display_name(const char *display_name, ThemeType default_type);

/**
 * Color based on normalized amplitude (0-1).
 * For most themes, interpolates hue from hue_start to hue_end.
 * For monochrome, returns grayscale based on amplitude.
 */
Color color_theme_amplitude_color(const ColorTheme *theme, float normalized_amplitude,
                                  float saturation, float brightness, float alpha);

#endif
