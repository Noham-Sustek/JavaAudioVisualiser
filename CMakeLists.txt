cmake_minimum_required(VERSION 3.20)
project(CAudioVisualiser C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Some dependencies still declare very old minimum CMake versions
set(CMAKE_POLICY_VERSION_MINIMUM 3.5)

include(FetchContent)

# raylib: window, rendering, input. Its audio module is disabled because
# audio goes through our own copy of miniaudio (playback + capture).
set(BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(CUSTOMIZE_BUILD ON CACHE BOOL "" FORCE)
set(USE_AUDIO OFF CACHE BOOL "" FORCE)
FetchContent_Declare(raylib
    URL https://github.com/raysan5/raylib/archive/refs/tags/5.5.tar.gz)

# raygui: immediate-mode widgets (header only)
FetchContent_Declare(raygui
    URL https://github.com/raysan5/raygui/archive/refs/tags/4.0.tar.gz
    SOURCE_SUBDIR do-not-add)

# miniaudio: WAV decoding, playback and microphone capture (header only)
FetchContent_Declare(miniaudio
    URL https://github.com/mackron/miniaudio/archive/refs/tags/0.11.21.tar.gz
    SOURCE_SUBDIR do-not-add)

FetchContent_MakeAvailable(raylib raygui miniaudio)

add_executable(CAudioVisualiser
    src/main.c
    src/audio/audio_analyzer.c
    src/audio/audio_player.c
    src/audio/microphone_capture.c
    src/audio/miniaudio_impl.c
    src/error/audio_errors.c
    src/fft/fft.c
    src/ui/color_theme.c
    src/ui/main_window.c
    src/ui/settings.c
    src/ui/spectrum_view.c
    src/ui/waveform_view.c
    src/util/config.c
    src/util/file_dialog.c
    src/util/logger.c
)

target_include_directories(CAudioVisualiser PRIVATE
    src
    ${raygui_SOURCE_DIR}/src
    ${miniaudio_SOURCE_DIR}
)

target_link_libraries(CAudioVisualiser PRIVATE raylib)

if(WIN32)
    target_link_libraries(CAudioVisualiser PRIVATE comdlg32)
    target_compile_definitions(CAudioVisualiser PRIVATE _CRT_SECURE_NO_WARNINGS)
elseif(UNIX)
    target_link_libraries(CAudioVisualiser PRIVATE m pthread dl)
endif()
