#include "ui/main_window.h"
#include "util/logger.h"

#include <stddef.h>

int main(int argc, char **argv) {
    logger_init();

    main_window_init(argc > 1 ? argv[1] : NULL);
    main_window_run();
    main_window_cleanup();

    logger_shutdown();
    return 0;
}
