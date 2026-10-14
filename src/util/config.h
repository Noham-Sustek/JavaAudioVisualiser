#ifndef UTIL_CONFIG_H
#define UTIL_CONFIG_H

#include <stddef.h>

/**
 * Directory holding settings, history and log files
 * (~/.c-audio-visualizer). Created on first call.
 */
const char *config_dir(void);

/**
 * Build the full path of a file inside the config directory.
 * Returns out.
 */
char *config_path(const char *file_name, char *out, size_t out_size);

#endif
