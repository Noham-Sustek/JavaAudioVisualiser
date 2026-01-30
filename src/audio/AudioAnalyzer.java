package audio;

import fft.FFT;
import javax.sound.sampled.AudioFormat;

public class AudioAnalyzer implements AudioPlayer.AudioDataListener {

    public interface AnalysisListener {
        void onAnalysis(double[] waveform, double[] spectrum);
    }

    private final int fftSize;
    private final FFT fft;
    private final double[] circularBuffer;
    private int writePosition = 0;
    private final double[] waveformSnapshot;
    private final double[] spectrumSnapshot;
    private final Object lock = new Object();
    private AnalysisListener listener;

    public AudioAnalyzer(int fftSize) {
        this.fftSize = fftSize;
        this.fft = new FFT(fftSize);
        this.circularBuffer = new double[fftSize];
        this.waveformSnapshot = new double[fftSize];
        this.spectrumSnapshot = new double[fftSize / 2];
    }

    public void setAnalysisListener(AnalysisListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAudioData(byte[] data, int bytesRead, AudioFormat format) {
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
        int channels = format.getChannels();
        boolean bigEndian = format.isBigEndian();
        int frameSize = sampleSizeInBytes * channels;

        int numSamples = bytesRead / frameSize;

        synchronized (lock) {
            for (int i = 0; i < numSamples; i++) {
                int offset = i * frameSize;
                double sample = extractSample(data, offset, sampleSizeInBytes, bigEndian, channels);
                circularBuffer[writePosition] = sample;
                writePosition = (writePosition + 1) % fftSize;
            }

            // Copy buffer in order for analysis
            for (int i = 0; i < fftSize; i++) {
                waveformSnapshot[i] = circularBuffer[(writePosition + i) % fftSize];
            }

            // Compute FFT
            double[] spectrum = fft.computeMagnitude(waveformSnapshot);
            System.arraycopy(spectrum, 0, spectrumSnapshot, 0, spectrum.length);
        }

        if (listener != null) {
            listener.onAnalysis(waveformSnapshot.clone(), spectrumSnapshot.clone());
        }
    }

    private double extractSample(byte[] data, int offset, int sampleSizeInBytes, boolean bigEndian, int channels) {
        double sample = 0;

        // Extract sample (mono or average stereo)
        for (int ch = 0; ch < channels; ch++) {
            int chOffset = offset + ch * sampleSizeInBytes;
            int value = 0;

            if (sampleSizeInBytes == 1) {
                value = data[chOffset];
            } else if (sampleSizeInBytes == 2) {
                if (bigEndian) {
                    value = (data[chOffset] << 8) | (data[chOffset + 1] & 0xFF);
                } else {
                    value = (data[chOffset + 1] << 8) | (data[chOffset] & 0xFF);
                }
            }

            // Normalize to [-1, 1]
            if (sampleSizeInBytes == 1) {
                sample += value / 128.0;
            } else if (sampleSizeInBytes == 2) {
                sample += value / 32768.0;
            }
        }
        return sample / channels;
    }

    public double[] getWaveform() {
        synchronized (lock) {
            return waveformSnapshot.clone();
        }
    }

    public double[] getSpectrum() {
        synchronized (lock) {
            return spectrumSnapshot.clone();
        }
    }

    public int getFFTSize() {
        return fftSize;
    }
}
