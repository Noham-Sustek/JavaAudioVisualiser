package fft;

public class FFT {

    private final int size;
    private final int logN;
    private final double[] hanningWindow;
    private final int[] bitReversalTable;
    private final double[] cosTable;
    private final double[] sinTable;

    public FFT(int size) {
        if ((size & (size - 1)) != 0) {
            throw new IllegalArgumentException("FFT size must be a power of 2");
        }
        this.size = size;
        this.logN = Integer.numberOfTrailingZeros(size);
        this.hanningWindow = createHanningWindow(size);
        this.bitReversalTable = createBitReversalTable(size);
        this.cosTable = new double[size / 2];
        this.sinTable = new double[size / 2];
        precomputeTwiddles();
    }

    private double[] createHanningWindow(int size) {
        double[] window = new double[size];
        for (int i = 0; i < size; i++) {
            window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (size - 1)));
        }
        return window;
    }

    private int[] createBitReversalTable(int size) {
        int[] table = new int[size];
        for (int i = 0; i < size; i++) {
            table[i] = reverseBits(i, logN);
        }
        return table;
    }

    private int reverseBits(int x, int bits) {
        int result = 0;
        for (int i = 0; i < bits; i++) {
            result = (result << 1) | (x & 1);
            x >>= 1;
        }
        return result;
    }

    private void precomputeTwiddles() {
        for (int i = 0; i < size / 2; i++) {
            double angle = -2 * Math.PI * i / size;
            cosTable[i] = Math.cos(angle);
            sinTable[i] = Math.sin(angle);
        }
    }

    public double[] computeMagnitude(double[] samples) {
        if (samples.length != size) {
            throw new IllegalArgumentException("Sample array must match FFT size");
        }

        double[] real = new double[size];
        double[] imag = new double[size];

        // Apply window and bit-reversal in one pass
        for (int i = 0; i < size; i++) {
            int j = bitReversalTable[i];
            real[j] = samples[i] * hanningWindow[i];
            imag[j] = 0;
        }

        // Cooley-Tukey iterative FFT with precomputed twiddles
        for (int len = 2; len <= size; len *= 2) {
            int halfLen = len / 2;
            int step = size / len;

            for (int i = 0; i < size; i += len) {
                for (int j = 0; j < halfLen; j++) {
                    int twiddleIdx = j * step;
                    double wReal = cosTable[twiddleIdx];
                    double wImag = sinTable[twiddleIdx];

                    int u = i + j;
                    int v = i + j + halfLen;

                    double tReal = wReal * real[v] - wImag * imag[v];
                    double tImag = wReal * imag[v] + wImag * real[v];

                    real[v] = real[u] - tReal;
                    imag[v] = imag[u] - tImag;
                    real[u] = real[u] + tReal;
                    imag[u] = imag[u] + tImag;
                }
            }
        }

        // Compute magnitude with logarithmic scale
        double[] magnitude = new double[size / 2];
        for (int i = 0; i < size / 2; i++) {
            double mag = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            magnitude[i] = Math.log10(1 + mag);
        }

        return magnitude;
    }

    public int getSize() {
        return size;
    }
}
