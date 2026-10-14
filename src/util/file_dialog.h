#ifndef UTIL_FILE_DIALOG_H
#define UTIL_FILE_DIALOG_H

#include <stdbool.h>
#include <stddef.h>

/**
 * Show a native "open file" dialog filtered on WAV files.
 * Returns true and fills out_path if the user picked a file.
 *
 * Windows: common dialog. Linux: zenity. macOS: osascript.
 */
bool file_dialog_open_wav(char *out_path, size_t out_size);

#endif
