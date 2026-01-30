package ui;

import audio.AudioAnalyzer;
import audio.AudioPlayer;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainWindow {

    private static final int FFT_SIZE = 8192;

    private final Stage stage;
    private final AudioPlayer audioPlayer;
    private final AudioAnalyzer audioAnalyzer;
    private WaveformView waveformView;
    private SpectrumView spectrumView;
    private Label fileLabel;
    private Button playPauseButton;
    private Button stopButton;
    private Slider progressSlider;
    private boolean isPlaying = false;
    private Label timeLabel;
    private AnimationTimer animationTimer;
    private boolean sliderDragging = false;

    private volatile double[] currentWaveform;
    private volatile double[] currentSpectrum;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.audioPlayer = new AudioPlayer();
        this.audioAnalyzer = new AudioAnalyzer(FFT_SIZE);

        audioPlayer.addAudioDataListener(audioAnalyzer);
        audioAnalyzer.setAnalysisListener((waveform, spectrum) -> {
            currentWaveform = waveform;
            currentSpectrum = spectrum;
        });

        setupUI();
        setupAnimationTimer();
    }

    private void setupUI() {
        // Visualizations
        waveformView = new WaveformView(1500, 300);
        spectrumView = new SpectrumView(1500, 350);

        VBox visualizationBox = new VBox(10);
        visualizationBox.setPadding(new Insets(10));
        VBox.setVgrow(visualizationBox, Priority.ALWAYS);

        Label waveformLabel = new Label("Forme d'onde (Temps)");
        waveformLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12px;");

        Label spectrumLabel = new Label("Spectre (Frequence)");
        spectrumLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12px;");

        CheckBox linearScaleCheckBox = new CheckBox("Echelle lineaire");
        linearScaleCheckBox.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12px;");
        linearScaleCheckBox.setOnAction(e -> spectrumView.setLinearScale(linearScaleCheckBox.isSelected()));

        CheckBox mirrorCheckBox = new CheckBox("Miroir");
        mirrorCheckBox.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12px;");
        mirrorCheckBox.setOnAction(e -> spectrumView.setMirrored(mirrorCheckBox.isSelected()));

        HBox spectrumHeader = new HBox(15, spectrumLabel, linearScaleCheckBox, mirrorCheckBox);
        spectrumHeader.setAlignment(Pos.CENTER_LEFT);

        VBox waveformBox = new VBox(5, waveformLabel, waveformView);
        VBox spectrumBox = new VBox(5, spectrumHeader, spectrumView);
        VBox.setVgrow(waveformBox, Priority.ALWAYS);
        VBox.setVgrow(spectrumBox, Priority.ALWAYS);

        visualizationBox.getChildren().addAll(waveformBox, spectrumBox);

        // Controls
        Button openButton = new Button("Ouvrir");
        playPauseButton = new Button("\u25B6"); // ▶ play icon
        stopButton = new Button("\u25A0"); // ■ stop icon

        playPauseButton.setDisable(true);
        stopButton.setDisable(true);

        fileLabel = new Label("Aucun fichier");
        fileLabel.setStyle("-fx-text-fill: #888888;");

        progressSlider = new Slider(0, 1, 0);
        progressSlider.setPrefWidth(300);
        progressSlider.setStyle("-fx-control-inner-background: #404050;");

        progressSlider.setOnMousePressed(e -> sliderDragging = true);
        progressSlider.setOnMouseReleased(e -> {
            sliderDragging = false;
            audioPlayer.seekTo(progressSlider.getValue());
        });

        timeLabel = new Label("0:00 / 0:00");
        timeLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

        openButton.setOnAction(e -> openFile());
        playPauseButton.setOnAction(e -> togglePlayPause());
        stopButton.setOnAction(e -> stop());

        HBox controlsBox = new HBox(10);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        controlsBox.setPadding(new Insets(10));
        controlsBox.getChildren().addAll(openButton, playPauseButton, stopButton, fileLabel, progressSlider, timeLabel);

        String buttonStyle = "-fx-background-color: #404050; -fx-text-fill: white; -fx-padding: 8 16; -fx-font-size: 14px;";
        openButton.setStyle(buttonStyle);
        playPauseButton.setStyle(buttonStyle);
        stopButton.setStyle(buttonStyle);

        // Main layout
        BorderPane root = new BorderPane();
        root.setCenter(visualizationBox);
        root.setBottom(controlsBox);
        root.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(root, 950, 750);
        stage.setTitle("Java Audio Visualiser");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setOnCloseRequest(e -> cleanup());
    }

    private void setupAnimationTimer() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (currentWaveform != null) {
                    waveformView.setWaveform(currentWaveform);
                    waveformView.draw();
                }
                if (currentSpectrum != null) {
                    spectrumView.setSpectrum(currentSpectrum);
                    spectrumView.draw();
                }
                updateProgress();
            }
        };
        animationTimer.start();
    }

    private void updateProgress() {
        if (!sliderDragging) {
            progressSlider.setValue(audioPlayer.getProgress());
        }
        double current = audioPlayer.getCurrentTimeSeconds();
        double total = audioPlayer.getTotalDurationSeconds();
        timeLabel.setText(formatTime(current) + " / " + formatTime(total));
    }

    private String formatTime(double seconds) {
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%d:%02d", mins, secs);
    }

    private void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ouvrir un fichier WAV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers WAV", "*.wav")
        );

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                audioPlayer.loadFile(file);
                fileLabel.setText(file.getName());
                playPauseButton.setDisable(false);
                stopButton.setDisable(false);
            } catch (Exception e) {
                fileLabel.setText("Erreur: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void togglePlayPause() {
        if (isPlaying) {
            audioPlayer.pause();
            playPauseButton.setText("\u25B6"); // ▶
            isPlaying = false;
        } else {
            audioPlayer.play();
            playPauseButton.setText("\u23F8"); // ⏸
            isPlaying = true;
        }
    }

    private void stop() {
        audioPlayer.stop();
        playPauseButton.setText("\u25B6"); // ▶
        isPlaying = false;
        currentWaveform = null;
        currentSpectrum = null;
        waveformView.setWaveform(null);
        waveformView.reset();
        spectrumView.setSpectrum(null);
        spectrumView.reset();
        waveformView.draw();
        spectrumView.draw();
        progressSlider.setValue(0);
    }

    private void cleanup() {
        animationTimer.stop();
        audioPlayer.close();
    }

    public void show() {
        stage.show();
    }
}
