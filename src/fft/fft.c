#include "fft/fft.h"

#include <math.h>
#include <stdlib.h>
#include <string.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

static int reverse_bits(int x, int bits) {
    int result = 0;
    for (int i = 0; i < bits; i++) {
        result = (result << 1) | (x & 1);
        x >>= 1;
    }
    return result;
}

int fft_init(FFT *fft, int size) {
    memset(fft, 0, sizeof(*fft));
    if (size < 2 || (size & (size - 1)) != 0) {
        return -1; // FFT size must be a power of 2
    }

    fft->size = size;
    while ((1 << fft->log_n) < size) fft->log_n++;

    fft->hanning_window = malloc(sizeof(float) * size);
    fft->bit_reversal_table = malloc(sizeof(int) * size);
    fft->cos_table = malloc(sizeof(float) * (size / 2));
    fft->sin_table = malloc(sizeof(float) * (size / 2));
    fft->real = malloc(sizeof(float) * size);
    fft->imag = malloc(sizeof(float) * size);

    if (!fft->hanning_window || !fft->bit_reversal_table || !fft->cos_table ||
        !fft->sin_table || !fft->real || !fft->imag) {
        fft_free(fft);
        return -1;
    }

    for (int i = 0; i < size; i++) {
        fft->hanning_window[i] = (float) (0.5 * (1 - cos(2 * M_PI * i / (size - 1))));
        fft->bit_reversal_table[i] = reverse_bits(i, fft->log_n);
    }

    for (int i = 0; i < size / 2; i++) {
        double angle = -2 * M_PI * i / size;
        fft->cos_table[i] = (float) cos(angle);
        fft->sin_table[i] = (float) sin(angle);
    }

    return 0;
}

void fft_free(FFT *fft) {
    free(fft->hanning_window);
    free(fft->bit_reversal_table);
    free(fft->cos_table);
    free(fft->sin_table);
    free(fft->real);
    free(fft->imag);
    memset(fft, 0, sizeof(*fft));
}

void fft_compute_magnitude(FFT *fft, const float *samples, float *magnitude) {
    const int size = fft->size;
    float *real = fft->real;
    float *imag = fft->imag;

    // Apply window and bit-reversal in one pass
    for (int i = 0; i < size; i++) {
        int j = fft->bit_reversal_table[i];
        real[j] = samples[i] * fft->hanning_window[i];
        imag[j] = 0;
    }

    // Cooley-Tukey iterative FFT with precomputed twiddles
    for (int len = 2; len <= size; len *= 2) {
        int half_len = len / 2;
        int step = size / len;

        for (int i = 0; i < size; i += len) {
            for (int j = 0; j < half_len; j++) {
                float w_real = fft->cos_table[j * step];
                float w_imag = fft->sin_table[j * step];

                int u = i + j;
                int v = i + j + half_len;

                float t_real = w_real * real[v] - w_imag * imag[v];
                float t_imag = w_real * imag[v] + w_imag * real[v];

                real[v] = real[u] - t_real;
                imag[v] = imag[u] - t_imag;
                real[u] = real[u] + t_real;
                imag[u] = imag[u] + t_imag;
            }
        }
    }

    // Compute magnitude with logarithmic scale
    for (int i = 0; i < size / 2; i++) {
        float mag = sqrtf(real[i] * real[i] + imag[i] * imag[i]);
        magnitude[i] = log10f(1 + mag);
    }
}
