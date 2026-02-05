package audio;

import exception.AudioFileException;
import util.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioPlayer {

    private static final Logger logger = Logger.getInstance();

    public interface AudioDataListener {
        void onAudioData(byte[] data, int bytesRead, AudioFormat format);
    }

    private File audioFile;
    private AudioFormat format;
    private Thread playbackThread;
    private volatile boolean playing = false;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();
    private final List<AudioDataListener> listeners = new ArrayList<>();

    private volatile long totalBytes = 0;
    private volatile long bytesPlayed = 0;
    private volatile long seekPosition = 0;

    public void loadFile(File file) throws AudioFileException {
        stop();

        if (!file.exists()) {
            logger.error("AudioPlayer", "File not found: " + file.getAbsolutePath());
            throw new AudioFileException(AudioFileException.ErrorType.FILE_NOT_FOUND, file.getAbsolutePath());
        }

        if (!file.canRead()) {
            logger.error("AudioPlayer", "Cannot read file: " + file.getAbsolutePath());
            throw new AudioFileException(AudioFileException.ErrorType.ACCESS_DENIED, file.getAbsolutePath());
        }

        try {
            this.audioFile = file;
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            format = audioInputStream.getFormat();
            totalBytes = audioInputStream.getFrameLength() * format.getFrameSize();
            bytesPlayed = 0;
            audioInputStream.close();
            logger.info("AudioPlayer", "Loaded file: " + file.getName() +
                    " (Format: " + format.getSampleRate() + "Hz, " + format.getChannels() + " channels)");
        } catch (UnsupportedAudioFileException e) {
            logger.error("AudioPlayer", "Unsupported audio format", e);
            throw new AudioFileException(AudioFileException.ErrorType.UNSUPPORTED_FORMAT, file.getAbsolutePath(), e);
        } catch (IOException e) {
            logger.error("AudioPlayer", "Error reading file", e);
            throw new AudioFileException(AudioFileException.ErrorType.CORRUPTED_FILE, file.getAbsolutePath(), e);
        }
    }

    public void addAudioDataListener(AudioDataListener listener) {
        listeners.add(listener);
    }

    public void play() {
        if (audioFile == null) return;

        if (paused) {
            synchronized (pauseLock) {
                paused = false;
                pauseLock.notifyAll();
            }
            logger.debug("AudioPlayer", "Resumed playback");
            return;
        }

        if (playing) return;

        playing = true;
        bytesPlayed = seekPosition;
        playbackThread = new Thread(this::playbackLoop, "AudioPlayback");
        playbackThread.start();
        logger.info("AudioPlayer", "Started playback: " + audioFile.getName());
    }

    private void playbackLoop() {
        AudioInputStream audioStream = null;
        SourceDataLine speaker = null;

        try {
            audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat fmt = audioStream.getFormat();

            // Skip to seek position
            if (seekPosition > 0) {
                long toSkip = seekPosition;
                while (toSkip > 0) {
                    long skipped = audioStream.skip(toSkip);
                    if (skipped <= 0) break;
                    toSkip -= skipped;
                }
            }

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(fmt);
            speaker.start();

            byte[] buffer = new byte[512];

            while (playing) {
                synchronized (pauseLock) {
                    while (paused && playing) {
                        try {
                            pauseLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            logger.warn("AudioPlayer", "Playback interrupted");
                            return;
                        }
                    }
                }

                if (!playing) break;

                int bytesRead = audioStream.read(buffer, 0, buffer.length);
                if (bytesRead == -1) break;

                bytesPlayed += bytesRead;
                speaker.write(buffer, 0, bytesRead);

                for (AudioDataListener listener : listeners) {
                    listener.onAudioData(buffer.clone(), bytesRead, fmt);
                }
            }

            speaker.drain();
            speaker.stop();
            speaker.close();
            logger.debug("AudioPlayer", "Playback completed");

        } catch (UnsupportedAudioFileException e) {
            logger.error("AudioPlayer", "Unsupported audio format during playback", e);
        } catch (IOException e) {
            logger.error("AudioPlayer", "I/O error during playback", e);
        } catch (LineUnavailableException e) {
            logger.error("AudioPlayer", "Audio line unavailable", e);
        } finally {
            playing = false;
            paused = false;

            if (speaker != null) {
                speaker.stop();
                speaker.close();
            }
            if (audioStream != null) {
                try {
                    audioStream.close();
                } catch (IOException e) {
                    logger.warn("AudioPlayer", "Error closing audio stream", e);
                }
            }
        }
    }

    public void pause() {
        if (playing && !paused) {
            paused = true;
            logger.debug("AudioPlayer", "Paused playback");
        }
    }

    public void stop() {
        playing = false;
        paused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        if (playbackThread != null) {
            try {
                playbackThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("AudioPlayer", "Interrupted while stopping playback");
            }
        }
        seekPosition = 0;
        logger.debug("AudioPlayer", "Stopped playback");
    }

    public void seekTo(double progress) {
        if (audioFile == null || totalBytes == 0) return;

        // Align to frame boundary
        long targetPosition = (long) (progress * totalBytes);
        int frameSize = format.getFrameSize();
        targetPosition = (targetPosition / frameSize) * frameSize;

        seekPosition = targetPosition;
        bytesPlayed = targetPosition;
        logger.debug("AudioPlayer", "Seeking to " + String.format("%.1f%%", progress * 100));

        // If currently playing, restart from new position
        if (playing || paused) {
            boolean wasPlaying = playing && !paused;
            playing = false;
            paused = false;
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
            if (playbackThread != null) {
                try {
                    playbackThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("AudioPlayer", "Interrupted while seeking");
                }
            }
            if (wasPlaying) {
                play();
            }
        }
    }

    public boolean isPlaying() {
        return playing && !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public double getProgress() {
        if (totalBytes == 0) return 0.0;
        return (double) bytesPlayed / totalBytes;
    }

    public double getCurrentTimeSeconds() {
        if (format == null) return 0.0;
        double bytesPerSecond = format.getSampleRate() * format.getFrameSize();
        return bytesPlayed / bytesPerSecond;
    }

    public double getTotalDurationSeconds() {
        if (format == null) return 0.0;
        double bytesPerSecond = format.getSampleRate() * format.getFrameSize();
        return totalBytes / bytesPerSecond;
    }

    public void close() {
        stop();
        logger.info("AudioPlayer", "Audio player closed");
    }
}
