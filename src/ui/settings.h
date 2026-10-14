#ifndef UI_SETTINGS_H
#define UI_SETTINGS_H

#include "ui/color_theme.h"

#include <stdbool.h>

#define SETTINGS_MAX_HISTORY 20
#define SETTINGS_MAX_PATH 1024

/**
 * User preferences and song history, persisted in the config directory.
 * Every setter saves immediately.
 */
typedef struct {
    ThemeType theme;
    bool linear_scale;
    bool mirrored;
    char song_history[SETTINGS_MAX_HISTORY][SETTINGS_MAX_PATH];
    int song_history_count;
} Settings;

/** Load settings and history from disk (defaults if missing). */
void settings_load(Settings *settings);

void settings_set_theme(Settings *settings, ThemeType theme);
void settings_set_linear_scale(Settings *settings, bool linear_scale);
void settings_set_mirrored(Settings *settings, bool mirrored);

/** Move or insert a file at the top of the history. */
void settings_add_to_song_history(Settings *settings, const char *file_path);

#endif
