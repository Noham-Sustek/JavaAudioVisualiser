package audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioPlayer {

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

    public void loadFile(File file) throws UnsupportedAudioFileException, IOException {
        stop();
        this.audioFile = file;
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        format = audioInputStream.getFormat();
        audioInputStream.close();
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
            return;
        }

        if (playing) return;

        playing = true;
        playbackThread = new Thread(this::playbackLoop, "AudioPlayback");
        playbackThread.start();
    }

    private void playbackLoop() {
        AudioInputStream audioStream = null;
        SourceDataLine speaker = null;

        try {
            audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat fmt = audioStream.getFormat();

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(fmt);
            speaker.start();

            byte[] buffer = new byte[4096];

            while (playing) {
                synchronized (pauseLock) {
                    while (paused && playing) {
                        try {
                            pauseLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }

                if (!playing) break;

                int bytesRead = audioStream.read(buffer, 0, buffer.length);
                if (bytesRead == -1) break;

                speaker.write(buffer, 0, bytesRead);

                for (AudioDataListener listener : listeners) {
                    listener.onAudioData(buffer.clone(), bytesRead, fmt);
                }
            }

            speaker.drain();
            speaker.stop();
            speaker.close();

        } catch (Exception e) {
            e.printStackTrace();
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
                    e.printStackTrace();
                }
            }
        }
    }

    public void pause() {
        if (playing && !paused) {
            paused = true;
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

    public void close() {
        stop();
    }
}
