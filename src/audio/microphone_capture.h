#ifndef AUDIO_MICROPHONE_CAPTURE_H
#define AUDIO_MICROPHONE_CAPTURE_H

#include "audio/audio_listener.h"
#include "error/audio_errors.h"

#include <stdbool.h>

/**
 * Captures audio from the default microphone for real-time visualization
 * (44100 Hz, stereo, 32-bit float).
 */
typedef struct MicrophoneCapture MicrophoneCapture;

MicrophoneCapture *microphone_capture_create(void);
void microphone_capture_destroy(MicrophoneCapture *mic);

void microphone_capture_add_listener(MicrophoneCapture *mic, AudioDataListener listener);

/** Start capturing. On failure fills error and returns false. */
bool microphone_capture_start(MicrophoneCapture *mic, MicrophoneError *error);
void microphone_capture_stop(MicrophoneCapture *mic);

bool microphone_capture_is_capturing(const MicrophoneCapture *mic);

/** Number of capture devices on the system, -1 on error. */
int microphone_capture_count_devices(void);

#endif
