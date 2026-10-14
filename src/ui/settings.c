#ifndef _WIN32
#define _XOPEN_SOURCE 700  // realpath
#endif

#include "ui/settings.h"
#include "util/config.h"
#include "util/logger.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define SETTINGS_FILE "visualizer_settings.properties"
#define HISTORY_FILE "song_history.txt"

static void trim_newline(char *line) {
    line[strcspn(line, "\r\n")] = '\0';
}

static bool parse_bool(const char *value) {
    return strcmp(value, "true") == 0;
}

static void save(const Settings *settings) {
    char path[SETTINGS_MAX_PATH];
    FILE *file = fopen(config_path(SETTINGS_FILE, path, sizeof(path)), "w");
    if (file == NULL) {
        log_error("Settings", "Could not save settings to %s", path);
        return;
    }

    fprintf(file, "# C Audio Visualizer Settings\n");
    fprintf(file, "theme=%s\n", color_theme_get(settings->theme)->id);
    fprintf(file, "linearScale=%s\n", settings->linear_scale ? "true" : "false");
    fprintf(file, "mirrored=%s\n", settings->mirrored ? "true" : "false");
    fclose(file);
    log_debug("Settings", "Settings saved");
}

static void load_properties(Settings *settings) {
    char path[SETTINGS_MAX_PATH];
    FILE *file = fopen(config_path(SETTINGS_FILE, path, sizeof(path)), "r");
    if (file == NULL) {
        log_info("Settings", "No settings file found, using defaults");
        return;
    }

    char line[256];
    while (fgets(line, sizeof(line), file) != NULL) {
        trim_newline(line);
        if (line[0] == '#' || line[0] == '\0') continue;

        char *separator = strchr(line, '=');
        if (separator == NULL) continue;
        *separator = '\0';
        const char *key = line;
        const char *value = separator + 1;

        if (strcmp(key, "theme") == 0) {
            settings->theme = color_theme_parse(value, THEME_DARK_BLUE);
            if (strcmp(color_theme_get(settings->theme)->id, value) != 0) {
                log_warn("Settings", "Invalid ThemeType value: %s, using default", value);
            }
        } else if (strcmp(key, "linearScale") == 0) {
            settings->linear_scale = parse_bool(value);
        } else if (strcmp(key, "mirrored") == 0) {
            settings->mirrored = parse_bool(value);
        }
    }

    fclose(file);
    log_info("Settings", "Settings loaded from file");
}

static void load_song_history(Settings *settings) {
    char path[SETTINGS_MAX_PATH];
    FILE *file = fopen(config_path(HISTORY_FILE, path, sizeof(path)), "r");
    if (file == NULL) return;

    char line[SETTINGS_MAX_PATH];
    while (settings->song_history_count < SETTINGS_MAX_HISTORY && fgets(line, sizeof(line), file) != NULL) {
        trim_newline(line);
        if (line[0] == '\0') continue;
        snprintf(settings->song_history[settings->song_history_count++], SETTINGS_MAX_PATH, "%s", line);
    }

    fclose(file);
    log_info("Settings", "Song history loaded: %d entries", settings->song_history_count);
}

static void save_song_history(const Settings *settings) {
    char path[SETTINGS_MAX_PATH];
    FILE *file = fopen(config_path(HISTORY_FILE, path, sizeof(path)), "w");
    if (file == NULL) {
        log_error("Settings", "Could not save song history to %s", path);
        return;
    }

    for (int i = 0; i < settings->song_history_count; i++) {
        fprintf(file, "%s\n", settings->song_history[i]);
    }
    fclose(file);
    log_debug("Settings", "Song history saved");
}

void settings_load(Settings *settings) {
    memset(settings, 0, sizeof(*settings));
    settings->theme = THEME_DARK_BLUE;
    load_properties(settings);
    load_song_history(settings);
}

void settings_set_theme(Settings *settings, ThemeType theme) {
    settings->theme = theme;
    save(settings);
}

void settings_set_linear_scale(Settings *settings, bool linear_scale) {
    settings->linear_scale = linear_scale;
    save(settings);
}

void settings_set_mirrored(Settings *settings, bool mirrored) {
    settings->mirrored = mirrored;
    save(settings);
}

void settings_add_to_song_history(Settings *settings, const char *file_path) {
    // Store absolute paths so history entries work from any working directory
    char entry[SETTINGS_MAX_PATH];
#ifdef _WIN32
    if (_fullpath(entry, file_path, sizeof(entry)) == NULL) {
        snprintf(entry, sizeof(entry), "%s", file_path);
    }
#else
    char *absolute = realpath(file_path, NULL);
    snprintf(entry, sizeof(entry), "%s", absolute ? absolute : file_path);
    free(absolute);
#endif

    // Remove if already exists (to move it to the top)
    int existing = -1;
    for (int i = 0; i < settings->song_history_count; i++) {
        if (strcmp(settings->song_history[i], entry) == 0) {
            existing = i;
            break;
        }
    }
    int last = existing >= 0 ? existing : settings->song_history_count;
    if (last >= SETTINGS_MAX_HISTORY) last = SETTINGS_MAX_HISTORY - 1;  // Drop the oldest

    // Shift down and insert at the beginning
    memmove(settings->song_history[1], settings->song_history[0], (size_t) last * SETTINGS_MAX_PATH);
    memcpy(settings->song_history[0], entry, SETTINGS_MAX_PATH);
    if (existing < 0 && settings->song_history_count < SETTINGS_MAX_HISTORY) {
        settings->song_history_count++;
    }

    save_song_history(settings);
    log_debug("Settings", "Added to song history: %s", entry);
}
