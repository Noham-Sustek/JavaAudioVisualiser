package audio;

import util.Logger;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures audio from the system microphone for real-time visualization.
 */
public class MicrophoneCapture {

    private static final Logger logger = Logger.getInstance();

    private final List<AudioPlayer.AudioDataListener> listeners = new ArrayList<>();
    private TargetDataLine microphone;
    private Thread captureThread;
    private volatile boolean capturing = false;
    private AudioFormat format;

    public MicrophoneCapture() {
        // Standard format for microphone capture
        this.format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100,      // Sample rate
                16,         // Sample size in bits
                2,          // Channels (stereo)
                4,          // Frame size (2 channels * 2 bytes)
                44100,      // Frame rate
                false       // Little endian
        );
    }

    public void addAudioDataListener(AudioPlayer.AudioDataListener listener) {
        listeners.add(listener);
    }

    public boolean start() {
        if (capturing) {
            return true;
        }

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                logger.error("MicrophoneCapture", "Microphone not supported with format: " + format);
                return false;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            capturing = true;
            captureThread = new Thread(this::captureLoop, "MicrophoneCapture");
            captureThread.start();

            logger.info("MicrophoneCapture", "Microphone capture started (44100Hz, 16-bit, stereo)");
            return true;

        } catch (LineUnavailableException e) {
            logger.error("MicrophoneCapture", "Could not access microphone", e);
            return false;
        }
    }

    private void captureLoop() {
        byte[] buffer = new byte[512];

        while (capturing) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);

            if (bytesRead > 0) {
                for (AudioPlayer.AudioDataListener listener : listeners) {
                    listener.onAudioData(buffer.clone(), bytesRead, format);
                }
            }
        }

        microphone.stop();
        microphone.close();
        logger.debug("MicrophoneCapture", "Capture loop ended");
    }

    public void stop() {
        if (!capturing) {
            return;
        }

        capturing = false;

        if (captureThread != null) {
            try {
                captureThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("MicrophoneCapture", "Interrupted while stopping capture");
            }
        }

        logger.info("MicrophoneCapture", "Microphone capture stopped");
    }

    public boolean isCapturing() {
        return capturing;
    }

    public AudioFormat getFormat() {
        return format;
    }

    /**
     * Get list of available microphones on the system.
     */
    public static List<String> getAvailableMicrophones() {
        List<String> microphones = new ArrayList<>();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);

        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(targetInfo)) {
                microphones.add(mixerInfo.getName());
            }
        }

        return microphones;
    }
}
