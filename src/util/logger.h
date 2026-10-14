#ifndef UTIL_LOGGER_H
#define UTIL_LOGGER_H

/**
 * Simple file-based logger for the application.
 * Logs messages to both console and a log file.
 */

typedef enum {
    LOG_LEVEL_DEBUG,
    LOG_LEVEL_INFO,
    LOG_LEVEL_WARN,
    LOG_LEVEL_ERROR
} LogLevel;

void logger_init(void);
void logger_shutdown(void);

/** Log a printf-style message at the specified level. */
void logger_log(LogLevel level, const char *source, const char *fmt, ...);

/** Path to the log file. */
const char *logger_file_path(void);

#define log_debug(source, ...) logger_log(LOG_LEVEL_DEBUG, source, __VA_ARGS__)
#define log_info(source, ...)  logger_log(LOG_LEVEL_INFO, source, __VA_ARGS__)
#define log_warn(source, ...)  logger_log(LOG_LEVEL_WARN, source, __VA_ARGS__)
#define log_error(source, ...) logger_log(LOG_LEVEL_ERROR, source, __VA_ARGS__)

#endif
