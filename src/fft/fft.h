#ifndef FFT_FFT_H
#define FFT_FFT_H

/**
 * Iterative radix-2 Cooley-Tukey FFT with a Hanning window,
 * precomputed twiddle factors and bit-reversal table.
 */
typedef struct {
    int size;
    int log_n;
    float *hanning_window;
    int *bit_reversal_table;
    float *cos_table;
    float *sin_table;
    float *real;   // Work buffers
    float *imag;
} FFT;

/** Returns 0 on success, -1 if size is not a power of 2 or allocation failed. */
int fft_init(FFT *fft, int size);
void fft_free(FFT *fft);

/**
 * Compute log10(1 + |X[k]|) for k in [0, size/2).
 * samples has fft->size entries, magnitude receives fft->size / 2 entries.
 */
void fft_compute_magnitude(FFT *fft, const float *samples, float *magnitude);

#endif
