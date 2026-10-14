#include "util/logger.h"
#include "util/config.h"

#include <stdarg.h>
#include <stdio.h>
#include <time.h>

#define LOG_FILE "visualizer.log"

static const char *LEVEL_PREFIX[] = { "[DEBUG]", "[INFO]", "[WARN]", "[ERROR]" };

static FILE *log_file;
static char log_file_path[1024];

void logger_init(void) {
    if (log_file != NULL) return;

    config_path(LOG_FILE, log_file_path, sizeof(log_file_path));
    log_file = fopen(log_file_path, "a");
    if (log_file == NULL) {
        fprintf(stderr, "Failed to open log file: %s\n", log_file_path);
    }

    log_info("Logger", "Application started");
}

void logger_shutdown(void) {
    if (log_file != NULL) {
        fclose(log_file);
        log_file = NULL;
    }
}

void logger_log(LogLevel level, const char *source, const char *fmt, ...) {
    char timestamp[32];
    time_t now = time(NULL);
    strftime(timestamp, sizeof(timestamp), "%Y-%m-%d %H:%M:%S", localtime(&now));

    char message[1024];
    va_list args;
    va_start(args, fmt);
    vsnprintf(message, sizeof(message), fmt, args);
    va_end(args);

    // Write to console
    FILE *console = (level >= LOG_LEVEL_WARN) ? stderr : stdout;
    fprintf(console, "%s %s [%s] %s\n", timestamp, LEVEL_PREFIX[level], source, message);

    // Write to file
    if (log_file != NULL) {
        fprintf(log_file, "%s %s [%s] %s\n", timestamp, LEVEL_PREFIX[level], source, message);
        fflush(log_file);
    }
}

const char *logger_file_path(void) {
    return log_file_path;
}
