#include "ui/main_window.h"

#include "audio/audio_analyzer.h"
#include "audio/audio_player.h"
#include "audio/microphone_capture.h"
#include "ui/color_theme.h"
#include "ui/settings.h"
#include "ui/spectrum_view.h"
#include "ui/waveform_view.h"
#include "util/file_dialog.h"
#include "util/logger.h"

#include "raylib.h"
#define RAYGUI_IMPLEMENTATION
#include "raygui.h"

#include <stdio.h>
#include <string.h>

#define FFT_SIZE 8192

#define FONT_SIZE 20
#define FONT_SPACING 2
#define PADDING 10
#define CONTROLS_HEIGHT 56
#define CONTROL_HEIGHT 36
#define LABEL_HEIGHT 24
#define CHECKBOX_SIZE 20
#define HISTORY_ITEM_HEIGHT 32
#define HISTORY_WIDTH 460

static struct {
    AudioPlayer *audio_player;
    AudioAnalyzer *audio_analyzer;
    MicrophoneCapture *microphone_capture;
    Settings settings;
    const ColorTheme *theme;

    WaveformView waveform_view;
    SpectrumView spectrum_view;

    char file_label[512];
    bool is_mic_mode;
    float slider_value;
    bool slider_dragging;

    char theme_items[512];          // "Bleu sombre;Ocean;..." for the dropdown
    int theme_index;
    bool theme_dropdown_open;

    bool history_open;
    bool open_file_requested;       // File dialog runs outside of BeginDrawing/EndDrawing

    bool alert_open;
    char alert_title[128];
    char alert_message[1024];
} window;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

static const char *base_name(const char *path) {
    const char *slash = strrchr(path, '/');
    const char *backslash = strrchr(path, '\\');
    if (backslash > slash) slash = backslash;
    return slash ? slash + 1 : path;
}

static float text_width(const char *text) {
    return MeasureTextEx(GuiGetFont(), text, FONT_SIZE, FONT_SPACING).x;
}

static void draw_text(const char *text, float x, float y, float size, Color color) {
    DrawTextEx(GuiGetFont(), text, (Vector2) { x, y }, size, FONT_SPACING, color);
}

/** Copy text into out, shortened with "..." so that it fits in max_width. */
static const char *fit_text(const char *text, float max_width, char *out, size_t out_size) {
    snprintf(out, out_size, "%s", text);
    if (text_width(out) <= max_width) return out;

    size_t len = strlen(out);
    while (len > 0) {
        len--;
        snprintf(out + len, out_size - len, "...");
        if (text_width(out) <= max_width) break;
    }
    return out;
}

static void format_time(double seconds, char *out, size_t out_size) {
    int total = (int) seconds;
    snprintf(out, out_size, "%d:%02d", total / 60, total % 60);
}

static void show_alert(const char *title, const char *header, const char *content) {
    snprintf(window.alert_title, sizeof(window.alert_title), "%s", title);
    snprintf(window.alert_message, sizeof(window.alert_message), "%s\n\n%s", header, content);
    window.alert_open = true;
}

static bool file_exists(const char *path) {
    FILE *file = fopen(path, "rb");
    if (file == NULL) return false;
    fclose(file);
    return true;
}

// ---------------------------------------------------------------------------
// Theme
// ---------------------------------------------------------------------------

static void apply_theme(const ColorTheme *theme) {
    window.theme = theme;
    base_view_set_color_theme(&window.waveform_view.base, theme);
    base_view_set_color_theme(&window.spectrum_view.base, theme);

    Color button = theme->button_background;

    // DEFAULT properties propagate to every control
    GuiSetStyle(DEFAULT, BASE_COLOR_NORMAL, ColorToInt(button));
    GuiSetStyle(DEFAULT, BASE_COLOR_FOCUSED, ColorToInt(ColorBrightness(button, 0.15f)));
    GuiSetStyle(DEFAULT, BASE_COLOR_PRESSED, ColorToInt(ColorBrightness(button, 0.3f)));
    GuiSetStyle(DEFAULT, BASE_COLOR_DISABLED, ColorToInt(ColorBrightness(button, -0.3f)));
    GuiSetStyle(DEFAULT, BORDER_COLOR_NORMAL, ColorToInt(theme->grid_line_color));
    GuiSetStyle(DEFAULT, BORDER_COLOR_FOCUSED, ColorToInt(theme->secondary_text_color));
    GuiSetStyle(DEFAULT, BORDER_COLOR_PRESSED, ColorToInt(theme->text_color));
    GuiSetStyle(DEFAULT, BORDER_COLOR_DISABLED, ColorToInt(ColorBrightness(theme->grid_line_color, -0.3f)));
    GuiSetStyle(DEFAULT, TEXT_COLOR_NORMAL, ColorToInt(WHITE));
    GuiSetStyle(DEFAULT, TEXT_COLOR_FOCUSED, ColorToInt(WHITE));
    GuiSetStyle(DEFAULT, TEXT_COLOR_PRESSED, ColorToInt(WHITE));
    GuiSetStyle(DEFAULT, TEXT_COLOR_DISABLED, ColorToInt(Fade(theme->secondary_text_color, 0.6f)));
    GuiSetStyle(DEFAULT, BACKGROUND_COLOR, ColorToInt(theme->background_color));
    GuiSetStyle(DEFAULT, LINE_COLOR, ColorToInt(theme->grid_line_color));

    // Checkboxes: label and check mark in the theme text color
    GuiSetStyle(CHECKBOX, TEXT_COLOR_NORMAL, ColorToInt(theme->text_color));
    GuiSetStyle(CHECKBOX, TEXT_COLOR_FOCUSED, ColorToInt(theme->text_color));
    GuiSetStyle(CHECKBOX, TEXT_COLOR_PRESSED, ColorToInt(theme->text_color));

    // Slider fill
    GuiSetStyle(SLIDER, BASE_COLOR_PRESSED, ColorToInt(theme->center_line_color));
    GuiSetStyle(SLIDER, TEXT_COLOR_FOCUSED, ColorToInt(theme->text_color));
}

// ---------------------------------------------------------------------------
// Actions
// ---------------------------------------------------------------------------

static void clear_visualization(void) {
    audio_analyzer_reset(window.audio_analyzer);
    waveform_view_set_waveform(&window.waveform_view, NULL, 0);
    spectrum_view_set_spectrum(&window.spectrum_view, NULL, 0);
    base_view_reset(&window.waveform_view.base);
    base_view_reset(&window.spectrum_view.base);
}

static void stop(void) {
    if (window.is_mic_mode) return;

    audio_player_stop(window.audio_player);
    clear_visualization();
    window.slider_value = 0;
}

static void toggle_play_pause(void) {
    if (window.is_mic_mode) return;

    if (audio_player_is_playing(window.audio_player)) {
        audio_player_pause(window.audio_player);
    } else {
        audio_player_play(window.audio_player);
    }
}

static void load_file(const char *path) {
    AudioFileError error;

    if (audio_player_load_file(window.audio_player, path, &error)) {
        clear_visualization();
        window.slider_value = 0;
        snprintf(window.file_label, sizeof(window.file_label), "%s", base_name(path));
        settings_add_to_song_history(&window.settings, path);
        log_info("MainWindow", "File loaded: %s", base_name(path));
    } else {
        const char *message = audio_file_error_message(error.type);
        snprintf(window.file_label, sizeof(window.file_label), "Erreur: %s", message);
        log_error("MainWindow", "Failed to load file: %s - %s", path, message);

        char content[1100];
        snprintf(content, sizeof(content), "Fichier: %s", base_name(path));
        show_alert("Erreur de chargement", message, content);
    }
}

static void toggle_microphone(void) {
    if (window.is_mic_mode) {
        microphone_capture_stop(window.microphone_capture);
        window.is_mic_mode = false;
        snprintf(window.file_label, sizeof(window.file_label), "Aucun fichier");
        clear_visualization();
        log_info("MainWindow", "Microphone mode disabled");
    } else {
        // Stop any file playback first
        if (audio_player_is_playing(window.audio_player) || audio_player_is_paused(window.audio_player)) {
            stop();
        }

        MicrophoneError error;
        if (microphone_capture_start(window.microphone_capture, &error)) {
            window.is_mic_mode = true;
            clear_visualization();
            snprintf(window.file_label, sizeof(window.file_label), "Microphone actif");
            log_info("MainWindow", "Microphone mode enabled");
        } else {
            const char *message = microphone_error_message(error.type);
            snprintf(window.file_label, sizeof(window.file_label), "Erreur: %s", message);
            log_error("MainWindow", "Microphone error: %s", microphone_error_name(error.type));
            show_alert("Erreur microphone", message, "Verifiez que votre microphone est connecte et autorise.");
        }
    }
}

static void open_file(void) {
    // Stop microphone if active
    if (window.is_mic_mode) toggle_microphone();

    char path[SETTINGS_MAX_PATH];
    if (file_dialog_open_wav(path, sizeof(path))) {
        load_file(path);
    }
}

static void load_from_history(int index) {
    char path[SETTINGS_MAX_PATH];
    snprintf(path, sizeof(path), "%s", window.settings.song_history[index]);

    if (window.is_mic_mode) toggle_microphone();

    if (file_exists(path)) {
        load_file(path);
    } else {
        snprintf(window.file_label, sizeof(window.file_label), "Fichier introuvable");
        log_warn("MainWindow", "File not found in history: %s", path);
    }
}

// ---------------------------------------------------------------------------
// Frame update
// ---------------------------------------------------------------------------

static void update_audio(void) {
    if (audio_player_poll_finished(window.audio_player)) {
        stop();
    }

    if (window.is_mic_mode || audio_player_is_playing(window.audio_player)) {
        audio_analyzer_update(window.audio_analyzer);
        waveform_view_set_waveform(&window.waveform_view,
                                   audio_analyzer_get_waveform(window.audio_analyzer), FFT_SIZE);
        spectrum_view_set_spectrum(&window.spectrum_view,
                                   audio_analyzer_get_spectrum(window.audio_analyzer), FFT_SIZE / 2);
    }

    if (!window.slider_dragging && !window.is_mic_mode) {
        window.slider_value = (float) audio_player_get_progress(window.audio_player);
    }
}

static void handle_dropped_files(void) {
    if (!IsFileDropped()) return;

    FilePathList files = LoadDroppedFiles();
    if (files.count > 0 && !window.alert_open) {
        if (window.is_mic_mode) toggle_microphone();
        load_file(files.paths[0]);
    }
    UnloadDroppedFiles(files);
}

// ---------------------------------------------------------------------------
// Drawing
// ---------------------------------------------------------------------------

static void draw_spectrum_header(float x, float y) {
    const ColorTheme *theme = window.theme;
    float text_y = y + (LABEL_HEIGHT - FONT_SIZE) / 2.0f;

    draw_text("Spectre (Frequence)", x, text_y, FONT_SIZE, theme->text_color);
    x += text_width("Spectre (Frequence)") + 15;

    float box_y = y + (LABEL_HEIGHT - CHECKBOX_SIZE) / 2.0f;

    bool linear = window.spectrum_view.linear_scale;
    GuiCheckBox((Rectangle) { x, box_y, CHECKBOX_SIZE, CHECKBOX_SIZE }, "Echelle lineaire", &linear);
    if (linear != window.spectrum_view.linear_scale) {
        window.spectrum_view.linear_scale = linear;
        settings_set_linear_scale(&window.settings, linear);
        log_debug("MainWindow", "Linear scale: %s", linear ? "true" : "false");
    }
    x += CHECKBOX_SIZE + GuiGetStyle(CHECKBOX, TEXT_PADDING) + text_width("Echelle lineaire") + 15;

    bool mirrored = window.spectrum_view.mirrored;
    GuiCheckBox((Rectangle) { x, box_y, CHECKBOX_SIZE, CHECKBOX_SIZE }, "Miroir", &mirrored);
    if (mirrored != window.spectrum_view.mirrored) {
        window.spectrum_view.mirrored = mirrored;
        settings_set_mirrored(&window.settings, mirrored);
        log_debug("MainWindow", "Mirror mode: %s", mirrored ? "true" : "false");
    }
}

static Rectangle theme_dropdown_bounds(float spectrum_header_y) {
    float x = PADDING + text_width("Spectre (Frequence)") + 15
              + CHECKBOX_SIZE + GuiGetStyle(CHECKBOX, TEXT_PADDING) + text_width("Echelle lineaire") + 15
              + CHECKBOX_SIZE + GuiGetStyle(CHECKBOX, TEXT_PADDING) + text_width("Miroir") + 15;
    return (Rectangle) { x, spectrum_header_y - 4, 240, LABEL_HEIGHT + 8 };
}

static void draw_theme_dropdown(Rectangle bounds) {
    int previous = window.theme_index;

    if (GuiDropdownBox(bounds, window.theme_items, &window.theme_index, window.theme_dropdown_open)) {
        window.theme_dropdown_open = !window.theme_dropdown_open;
    }

    if (window.theme_index != previous) {
        const ColorTheme *theme = color_theme_get((ThemeType) window.theme_index);
        settings_set_theme(&window.settings, theme->type);
        apply_theme(theme);
        log_info("MainWindow", "Theme changed to: %s", theme->display_name);
    }
}

/** Returns the bounds of the history button (the popup is anchored to it). */
static Rectangle draw_controls(float y) {
    const ColorTheme *theme = window.theme;
    float x = PADDING;
    float text_y = y + (CONTROL_HEIGHT - FONT_SIZE) / 2.0f;
    bool loaded = audio_player_is_loaded(window.audio_player);
    char buffer[512];

    // File controls are disabled in microphone mode
    if (window.is_mic_mode) GuiDisable();
    if (GuiButton((Rectangle) { x, y, 110, CONTROL_HEIGHT }, "Ouvrir")) {
        window.open_file_requested = true;
    }
    x += 110 + PADDING;

    if (!loaded) GuiDisable();
    const char *play_icon = GuiIconText(audio_player_is_playing(window.audio_player)
                                        ? ICON_PLAYER_PAUSE : ICON_PLAYER_PLAY, NULL);
    if (GuiButton((Rectangle) { x, y, 56, CONTROL_HEIGHT }, play_icon)) {
        toggle_play_pause();
    }
    x += 56 + PADDING;

    if (GuiButton((Rectangle) { x, y, 56, CONTROL_HEIGHT }, GuiIconText(ICON_PLAYER_STOP, NULL))) {
        stop();
    }
    x += 56 + PADDING;
    GuiEnable();

    bool mic_active = window.is_mic_mode;
    GuiToggle((Rectangle) { x, y, 90, CONTROL_HEIGHT }, "Micro", &mic_active);
    if (mic_active != window.is_mic_mode) {
        toggle_microphone();
    }
    x += 90 + PADDING;

    // History button sits on the far right, slider takes the remaining space
    float history_width = 140;
    float time_width = text_width("00:00 / 00:00") + PADDING;
    float file_width = 280;
    Rectangle history_bounds = { GetScreenWidth() - PADDING - history_width, y, history_width, CONTROL_HEIGHT };
    float slider_width = history_bounds.x - PADDING - time_width - PADDING - (x + file_width + PADDING);
    if (slider_width < 80) {
        file_width -= 80 - slider_width;
        slider_width = 80;
    }

    draw_text(fit_text(window.file_label, file_width, buffer, sizeof(buffer)),
              x, text_y, FONT_SIZE, theme->secondary_text_color);
    x += file_width + PADDING;

    Rectangle slider_bounds = { x, y + (CONTROL_HEIGHT - 16) / 2.0f, slider_width, 16 };
    bool slider_enabled = loaded && !window.is_mic_mode;
    if (slider_enabled && !GuiIsLocked() && IsMouseButtonPressed(MOUSE_BUTTON_LEFT) &&
        CheckCollisionPointRec(GetMousePosition(), slider_bounds)) {
        window.slider_dragging = true;
    }
    if (!slider_enabled) GuiDisable();
    GuiSlider(slider_bounds, NULL, NULL, &window.slider_value, 0.0f, 1.0f);
    GuiEnable();
    if (window.slider_dragging && IsMouseButtonReleased(MOUSE_BUTTON_LEFT)) {
        window.slider_dragging = false;
        audio_player_seek_to(window.audio_player, window.slider_value);
    }
    x += slider_width + PADDING;

    if (window.is_mic_mode) {
        snprintf(buffer, sizeof(buffer), "Microphone");
    } else {
        char current[16], total[16];
        format_time(audio_player_get_current_time_seconds(window.audio_player), current, sizeof(current));
        format_time(audio_player_get_total_duration_seconds(window.audio_player), total, sizeof(total));
        snprintf(buffer, sizeof(buffer), "%s / %s", current, total);
    }
    draw_text(buffer, x, y + (CONTROL_HEIGHT - 16) / 2.0f, 16, theme->secondary_text_color);

    if (window.is_mic_mode) GuiDisable();
    if (GuiButton(history_bounds, "Historique")) {
        window.history_open = true;
    }
    GuiEnable();

    return history_bounds;
}

static void draw_history_popup(Rectangle anchor) {
    int count = window.settings.song_history_count;
    int rows = count > 0 ? count : 1;
    float height = rows * HISTORY_ITEM_HEIGHT + 2 * 5;
    float max_height = anchor.y - 2 * PADDING;
    if (height > max_height) {
        rows = (int) ((max_height - 10) / HISTORY_ITEM_HEIGHT);
        height = rows * HISTORY_ITEM_HEIGHT + 10;
    }

    Rectangle popup = { anchor.x + anchor.width - HISTORY_WIDTH, anchor.y - height - 4, HISTORY_WIDTH, height };

    // Click outside closes the popup
    if (IsMouseButtonPressed(MOUSE_BUTTON_LEFT) && !CheckCollisionPointRec(GetMousePosition(), popup)) {
        window.history_open = false;
        return;
    }

    DrawRectangleRec(popup, window.theme->background_color);
    DrawRectangleLinesEx(popup, 1, window.theme->grid_line_color);

    if (count == 0) {
        GuiDisable();
        GuiButton((Rectangle) { popup.x + 5, popup.y + 5, popup.width - 10, HISTORY_ITEM_HEIGHT - 4 },
                  "(Aucun historique)");
        GuiEnable();
        return;
    }

    int alignment = GuiGetStyle(BUTTON, TEXT_ALIGNMENT);
    GuiSetStyle(BUTTON, TEXT_ALIGNMENT, TEXT_ALIGN_LEFT);

    char label[256];
    for (int i = 0; i < rows && i < count; i++) {
        Rectangle item = { popup.x + 5, popup.y + 5 + i * HISTORY_ITEM_HEIGHT, popup.width - 10, HISTORY_ITEM_HEIGHT - 4 };
        fit_text(base_name(window.settings.song_history[i]), item.width - 20, label, sizeof(label));
        if (GuiButton(item, label)) {
            window.history_open = false;
            load_from_history(i);
            break;
        }
    }

    GuiSetStyle(BUTTON, TEXT_ALIGNMENT, alignment);
}

static void draw_alert(void) {
    DrawRectangle(0, 0, GetScreenWidth(), GetScreenHeight(), Fade(BLACK, 0.5f));

    // Size the box to the longest line of the message
    float width = text_width(window.alert_title);
    char line[1024];
    const char *start = window.alert_message;
    while (*start) {
        size_t len = strcspn(start, "\n");
        snprintf(line, sizeof(line), "%.*s", (int) len, start);
        float w = text_width(line);
        if (w > width) width = w;
        start += len;
        if (*start == '\n') start++;
    }
    width += 60;
    float height = 200;

    Rectangle bounds = { (GetScreenWidth() - width) / 2, (GetScreenHeight() - height) / 2, width, height };
    if (GuiMessageBox(bounds, window.alert_title, window.alert_message, "OK") >= 0) {
        window.alert_open = false;
    }
}

static void draw_frame(void) {
    const ColorTheme *theme = window.theme;
    float width = (float) GetScreenWidth();
    float height = (float) GetScreenHeight();

    // Layout: waveform (300) and spectrum (350) share the space above the controls
    float view_width = width - 2 * PADDING;
    float available = height - CONTROLS_HEIGHT - 2 * PADDING - 2 * (LABEL_HEIGHT + 5);
    float waveform_height = available * 300 / 650;
    float spectrum_height = available - waveform_height;

    float y = PADDING;
    float waveform_label_y = y;
    y += LABEL_HEIGHT + 5;
    base_view_set_bounds(&window.waveform_view.base, (Rectangle) { PADDING, y, view_width, waveform_height });
    y += waveform_height + PADDING;
    float spectrum_header_y = y;
    y += LABEL_HEIGHT + 5;
    base_view_set_bounds(&window.spectrum_view.base, (Rectangle) { PADDING, y, view_width, spectrum_height });

    BeginDrawing();
    ClearBackground(theme->background_color);

    // Popups capture input: lock everything underneath them
    bool popup_open = window.alert_open || window.history_open || window.theme_dropdown_open;
    if (popup_open) GuiLock();

    draw_text("Forme d'onde (Temps)", PADDING, waveform_label_y + (LABEL_HEIGHT - FONT_SIZE) / 2.0f,
              FONT_SIZE, theme->text_color);
    base_view_draw(&window.waveform_view.base);

    draw_spectrum_header(PADDING, spectrum_header_y);
    base_view_draw(&window.spectrum_view.base);

    Rectangle history_bounds = draw_controls(height - CONTROLS_HEIGHT + (CONTROLS_HEIGHT - CONTROL_HEIGHT) / 2.0f);

    // Dropdown is drawn last so its list covers the views
    if (window.theme_dropdown_open && !window.alert_open) GuiUnlock();
    draw_theme_dropdown(theme_dropdown_bounds(spectrum_header_y));

    if (window.history_open && !window.alert_open) {
        GuiUnlock();
        draw_history_popup(history_bounds);
    }

    if (window.alert_open) {
        GuiUnlock();
        draw_alert();
    }

    GuiUnlock();
    EndDrawing();
}

// ---------------------------------------------------------------------------
// Lifecycle
// ---------------------------------------------------------------------------

void main_window_init(const char *initial_file) {
    memset(&window, 0, sizeof(window));

    window.audio_player = audio_player_create();
    window.audio_analyzer = audio_analyzer_create(FFT_SIZE);
    window.microphone_capture = microphone_capture_create();
    settings_load(&window.settings);

    log_info("MainWindow", "Initializing application");

    // Available themes (and dropdown items)
    for (int i = 0; i < THEME_COUNT; i++) {
        if (i > 0) strcat(window.theme_items, ";");
        strcat(window.theme_items, color_theme_get((ThemeType) i)->display_name);
    }
    log_info("MainWindow", "Available themes: %s", window.theme_items);

    // Setup audio listeners
    audio_player_add_listener(window.audio_player, audio_analyzer_listener(window.audio_analyzer));
    microphone_capture_add_listener(window.microphone_capture, audio_analyzer_listener(window.audio_analyzer));

    // Window
    SetTraceLogLevel(LOG_WARNING);
    SetConfigFlags(FLAG_WINDOW_RESIZABLE | FLAG_VSYNC_HINT | FLAG_MSAA_4X_HINT);
    InitWindow(950, 750, "C Audio Visualiser");
    SetWindowMinSize(900, 500);
    MaximizeWindow();
    SetTargetFPS(60);
    SetExitKey(KEY_NULL);

    GuiSetStyle(DEFAULT, TEXT_SIZE, FONT_SIZE);
    GuiSetStyle(DEFAULT, TEXT_SPACING, FONT_SPACING);
    GuiSetStyle(CHECKBOX, TEXT_PADDING, 8);

    waveform_view_init(&window.waveform_view);
    spectrum_view_init(&window.spectrum_view);
    snprintf(window.file_label, sizeof(window.file_label), "Aucun fichier");

    // Load and apply saved settings
    window.spectrum_view.linear_scale = window.settings.linear_scale;
    window.spectrum_view.mirrored = window.settings.mirrored;
    window.theme_index = (int) window.settings.theme;
    apply_theme(color_theme_get(window.settings.theme));

    log_info("MainWindow", "Settings loaded - Theme: %s, Linear: %s, Mirror: %s",
             window.theme->display_name,
             window.settings.linear_scale ? "true" : "false",
             window.settings.mirrored ? "true" : "false");

    if (initial_file != NULL) {
        load_file(initial_file);
    }

    log_info("MainWindow", "Application initialized successfully");
}

void main_window_run(void) {
    while (!WindowShouldClose()) {
        handle_dropped_files();
        update_audio();
        draw_frame();

        if (window.open_file_requested) {
            window.open_file_requested = false;
            open_file();
        }
    }
}

void main_window_cleanup(void) {
    log_info("MainWindow", "Application closing");
    audio_player_destroy(window.audio_player);
    microphone_capture_destroy(window.microphone_capture);
    audio_analyzer_destroy(window.audio_analyzer);
    CloseWindow();
}
