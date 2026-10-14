#ifndef UI_MAIN_WINDOW_H
#define UI_MAIN_WINDOW_H

/**
 * Main window: visualizations, playback controls, settings and history.
 * initial_file may be NULL.
 */
void main_window_init(const char *initial_file);

/** Run the event/render loop until the window is closed. */
void main_window_run(void);

void main_window_cleanup(void);

#endif
