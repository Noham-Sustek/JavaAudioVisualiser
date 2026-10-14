#include "audio/microphone_capture.h"
#include "util/logger.h"

#include "miniaudio.h"

#include <stdlib.h>

#define MAX_LISTENERS 4
#define SAMPLE_RATE 44100
#define CHANNELS 2

struct MicrophoneCapture {
    ma_device device;
    bool capturing;
    AudioDataListener listeners[MAX_LISTENERS];
    int listener_count;
};

static void data_callback(ma_device *device, void *output, const void *input, ma_uint32 frame_count) {
    (void) output;
    MicrophoneCapture *mic = device->pUserData;

    for (int i = 0; i < mic->listener_count; i++) {
        mic->listeners[i].on_audio_data(mic->listeners[i].user_data, input, frame_count, CHANNELS);
    }
}

static MicrophoneErrorType error_type_for(ma_result result) {
    switch (result) {
        case MA_NO_DEVICE:
        case MA_DOES_NOT_EXIST:
            return MICROPHONE_NOT_FOUND;
        case MA_ACCESS_DENIED:
            return MICROPHONE_ACCESS_DENIED;
        case MA_BUSY:
            return MICROPHONE_BUSY;
        case MA_FORMAT_NOT_SUPPORTED:
        case MA_SHARE_MODE_NOT_SUPPORTED:
            return MICROPHONE_UNSUPPORTED_FORMAT;
        default:
            return MICROPHONE_UNKNOWN;
    }
}

MicrophoneCapture *microphone_capture_create(void) {
    return calloc(1, sizeof(MicrophoneCapture));
}

void microphone_capture_destroy(MicrophoneCapture *mic) {
    if (mic == NULL) return;
    microphone_capture_stop(mic);
    free(mic);
}

void microphone_capture_add_listener(MicrophoneCapture *mic, AudioDataListener listener) {
    if (mic->listener_count < MAX_LISTENERS) {
        mic->listeners[mic->listener_count++] = listener;
    }
}

bool microphone_capture_start(MicrophoneCapture *mic, MicrophoneError *error) {
    if (mic->capturing) return true;

    // Check if any microphone is available
    if (microphone_capture_count_devices() == 0) {
        log_error("MicrophoneCapture", "No microphone found");
        microphone_error_set(error, MICROPHONE_NOT_FOUND, MA_NO_DEVICE);
        return false;
    }

    ma_device_config config = ma_device_config_init(ma_device_type_capture);
    config.capture.format = ma_format_f32;
    config.capture.channels = CHANNELS;
    config.sampleRate = SAMPLE_RATE;
    config.periodSizeInFrames = 512;
    config.dataCallback = data_callback;
    config.pUserData = mic;

    ma_result result = ma_device_init(NULL, &config, &mic->device);
    if (result != MA_SUCCESS) {
        log_error("MicrophoneCapture", "Could not access microphone - %s", ma_result_description(result));
        microphone_error_set(error, error_type_for(result), result);
        return false;
    }

    result = ma_device_start(&mic->device);
    if (result != MA_SUCCESS) {
        log_error("MicrophoneCapture", "Could not start microphone - %s", ma_result_description(result));
        ma_device_uninit(&mic->device);
        microphone_error_set(error, error_type_for(result), result);
        return false;
    }

    mic->capturing = true;
    log_info("MicrophoneCapture", "Microphone capture started (%dHz, float, stereo) on: %s",
             SAMPLE_RATE, mic->device.capture.name);
    return true;
}

void microphone_capture_stop(MicrophoneCapture *mic) {
    if (!mic->capturing) return;

    ma_device_uninit(&mic->device); // Stops the device and waits for the audio thread
    mic->capturing = false;
    log_info("MicrophoneCapture", "Microphone capture stopped");
}

bool microphone_capture_is_capturing(const MicrophoneCapture *mic) {
    return mic->capturing;
}

int microphone_capture_count_devices(void) {
    ma_context context;
    if (ma_context_init(NULL, 0, NULL, &context) != MA_SUCCESS) {
        return -1;
    }

    ma_device_info *capture_infos;
    ma_uint32 capture_count = 0;
    int count = -1;
    if (ma_context_get_devices(&context, NULL, NULL, &capture_infos, &capture_count) == MA_SUCCESS) {
        count = (int) capture_count;
        for (ma_uint32 i = 0; i < capture_count; i++) {
            log_debug("MicrophoneCapture", "Found microphone: %s", capture_infos[i].name);
        }
    }

    ma_context_uninit(&context);
    return count;
}
