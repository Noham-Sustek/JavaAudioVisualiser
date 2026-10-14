#include "ui/color_theme.h"

#include <string.h>

#define RGB(r, g, b) { r, g, b, 255 }

static const ColorTheme THEMES[THEME_COUNT] = {
    {
        THEME_DARK_BLUE, "DARK_BLUE", "Bleu sombre",
        RGB(26, 26, 46),        // #1a1a2e
        RGB(20, 20, 30),        // Canvas background
        RGB(50, 50, 70),        // Grid lines
        RGB(80, 80, 100),       // Center line
        RGB(64, 64, 80),        // #404050
        RGB(170, 170, 170),     // #AAAAAA
        RGB(136, 136, 136),     // #888888
        0.33f,                  // Green (low amplitude)
        0.0f                    // Red (high amplitude)
    },
    {
        THEME_OCEAN, "OCEAN", "Ocean",
        RGB(10, 25, 40),        // Dark navy background
        RGB(15, 30, 50),        // Canvas background
        RGB(40, 70, 100),       // Grid lines
        RGB(60, 100, 140),      // Center line
        RGB(30, 60, 90),        // Button background
        RGB(150, 200, 255),     // Text color
        RGB(100, 150, 200),     // Secondary text
        0.55f,                  // Cyan (low amplitude)
        0.95f                   // Magenta (high amplitude) - opposite spectrum
    },
    {
        THEME_SUNSET, "SUNSET", "Coucher de soleil",
        RGB(35, 20, 25),        // Dark red-brown background
        RGB(40, 25, 30),        // Canvas background
        RGB(80, 50, 60),        // Grid lines
        RGB(120, 70, 80),       // Center line
        RGB(70, 40, 50),        // Button background
        RGB(255, 200, 180),     // Text color
        RGB(200, 150, 130),     // Secondary text
        0.15f,                  // Yellow-orange (low amplitude)
        0.85f                   // Purple (high amplitude) - opposite spectrum
    },
    {
        THEME_FOREST, "FOREST", "Foret",
        RGB(15, 30, 20),        // Dark green background
        RGB(20, 35, 25),        // Canvas background
        RGB(50, 80, 60),        // Grid lines
        RGB(70, 110, 80),       // Center line
        RGB(40, 70, 50),        // Button background
        RGB(180, 230, 190),     // Text color
        RGB(130, 180, 140),     // Secondary text
        0.33f,                  // Green (low amplitude)
        0.83f                   // Magenta-pink (high amplitude) - opposite spectrum
    },
    {
        THEME_PURPLE, "PURPLE", "Violet",
        RGB(25, 15, 35),        // Dark purple background
        RGB(30, 20, 40),        // Canvas background
        RGB(60, 45, 80),        // Grid lines
        RGB(90, 70, 120),       // Center line
        RGB(50, 35, 70),        // Button background
        RGB(220, 190, 255),     // Text color
        RGB(170, 140, 200),     // Secondary text
        0.75f,                  // Purple (low amplitude)
        0.25f                   // Yellow-green (high amplitude) - opposite spectrum
    },
    {
        THEME_MONOCHROME, "MONOCHROME", "Monochrome",
        RGB(20, 20, 20),        // Dark gray background
        RGB(25, 25, 25),        // Canvas background
        RGB(60, 60, 60),        // Grid lines
        RGB(90, 90, 90),        // Center line
        RGB(50, 50, 50),        // Button background
        RGB(200, 200, 200),     // Text color
        RGB(140, 140, 140),     // Secondary text
        0.0f,                   // Not used (grayscale)
        0.0f                    // Not used (grayscale)
    },
    {
        THEME_SYNTHWAVE, "SYNTHWAVE", "Synthwave",
        RGB(15, 5, 25),         // Deep dark purple background
        RGB(20, 8, 35),         // Canvas background
        RGB(80, 30, 100),       // Grid lines (purple)
        RGB(255, 50, 150),      // Center line (hot pink)
        RGB(60, 20, 80),        // Button background
        RGB(255, 150, 255),     // Text color (pink)
        RGB(200, 100, 200),     // Secondary text
        0.83f,                  // Purple (low amplitude)
        0.05f                   // Red-orange (high amplitude)
    },
    {
        THEME_EVA_01, "EVA_01", "EVA-01",
        RGB(20, 12, 28),        // Dark purple background
        RGB(25, 15, 35),        // Canvas background
        RGB(115, 79, 154),      // Grid lines (#734f9a - EVA purple)
        RGB(139, 212, 80),      // Center line (#8bd450 - EVA green)
        RGB(70, 50, 90),        // Button background
        RGB(139, 212, 80),      // Text color (EVA green)
        RGB(115, 79, 154),      // Secondary text (EVA purple)
        0.77f,                  // Purple #734f9a (low amplitude)
        0.26f                   // Green #8bd450 (high amplitude)
    },
    {
        THEME_FIRE_ICE, "FIRE_ICE", "Feu & Glace",
        RGB(25, 15, 20),        // Dark red-ish background
        RGB(30, 18, 25),        // Canvas background
        RGB(80, 40, 50),        // Grid lines (dark red)
        RGB(100, 150, 200),     // Center line (light blue)
        RGB(60, 30, 40),        // Button background
        RGB(255, 200, 200),     // Text color (light red/pink)
        RGB(150, 180, 220),     // Secondary text (light blue)
        1.0f,                   // Deep red (low amplitude)
        0.55f                   // Light blue/cyan (high amplitude)
    },
};

const ColorTheme *color_theme_get(ThemeType type) {
    if (type < 0 || type >= THEME_COUNT) type = THEME_DARK_BLUE;
    return &THEMES[type];
}

ThemeType color_theme_parse(const char *id, ThemeType default_type) {
    if (id == NULL || id[0] == '\0') return default_type;
    for (int i = 0; i < THEME_COUNT; i++) {
        if (strcmp(THEMES[i].id, id) == 0) return THEMES[i].type;
    }
    return default_type;
}

ThemeType color_theme_find_by_display_name(const char *display_name, ThemeType default_type) {
    if (display_name == NULL) return default_type;
    for (int i = 0; i < THEME_COUNT; i++) {
        if (strcmp(THEMES[i].display_name, display_name) == 0) return THEMES[i].type;
    }
    return default_type;
}

Color color_theme_amplitude_color(const ColorTheme *theme, float normalized_amplitude,
                                  float saturation, float brightness, float alpha) {
    if (theme->type == THEME_MONOCHROME) {
        unsigned char gray = (unsigned char) ((0.3f + normalized_amplitude * 0.7f) * 255);
        return (Color) { gray, gray, gray, (unsigned char) (alpha * 255) };
    }

    float hue = theme->hue_start - normalized_amplitude * (theme->hue_start - theme->hue_end);
    Color color = ColorFromHSV(hue * 360, saturation, brightness);
    color.a = (unsigned char) (alpha * 255);
    return color;
}
