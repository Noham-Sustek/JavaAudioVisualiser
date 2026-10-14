#include "util/config.h"

#include <stdio.h>
#include <stdlib.h>

#ifdef _WIN32
#include <direct.h>
#define MKDIR(path) _mkdir(path)
#else
#include <sys/stat.h>
#define MKDIR(path) mkdir(path, 0755)
#endif

#define CONFIG_DIR_NAME ".c-audio-visualizer"

static char dir[1024];

const char *config_dir(void) {
    if (dir[0] == '\0') {
#ifdef _WIN32
        const char *home = getenv("USERPROFILE");
#else
        const char *home = getenv("HOME");
#endif
        if (home == NULL) home = ".";

        snprintf(dir, sizeof(dir), "%s/%s", home, CONFIG_DIR_NAME);
        MKDIR(dir); // Fails harmlessly if it already exists
    }
    return dir;
}

char *config_path(const char *file_name, char *out, size_t out_size) {
    snprintf(out, out_size, "%s/%s", config_dir(), file_name);
    return out;
}
