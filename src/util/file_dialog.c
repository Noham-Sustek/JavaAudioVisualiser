// Kept separate from the UI code: windows.h clashes with raylib names.
#include "util/file_dialog.h"

#include <stdio.h>
#include <string.h>

#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <commdlg.h>

bool file_dialog_open_wav(char *out_path, size_t out_size) {
    OPENFILENAMEA ofn;
    memset(&ofn, 0, sizeof(ofn));
    out_path[0] = '\0';

    ofn.lStructSize = sizeof(ofn);
    ofn.hwndOwner = GetActiveWindow();
    ofn.lpstrFilter = "Fichiers WAV (*.wav)\0*.wav\0";
    ofn.lpstrFile = out_path;
    ofn.nMaxFile = (DWORD) out_size;
    ofn.lpstrTitle = "Ouvrir un fichier WAV";
    ofn.Flags = OFN_FILEMUSTEXIST | OFN_PATHMUSTEXIST | OFN_NOCHANGEDIR;

    return GetOpenFileNameA(&ofn) != 0;
}

#else

bool file_dialog_open_wav(char *out_path, size_t out_size) {
#ifdef __APPLE__
    const char *cmd = "osascript -e 'POSIX path of (choose file of type {\"wav\"} "
                      "with prompt \"Ouvrir un fichier WAV\")' 2>/dev/null";
#else
    const char *cmd = "zenity --file-selection --title='Ouvrir un fichier WAV' "
                      "--file-filter='Fichiers WAV | *.wav *.WAV' 2>/dev/null";
#endif
    FILE *pipe = popen(cmd, "r");
    if (pipe == NULL) return false;

    out_path[0] = '\0';
    if (fgets(out_path, (int) out_size, pipe) == NULL) out_path[0] = '\0';
    pclose(pipe);

    out_path[strcspn(out_path, "\r\n")] = '\0';
    return out_path[0] != '\0';
}

#endif
