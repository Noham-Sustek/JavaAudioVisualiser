package ui;

import audio.AudioAnalyzer;
import audio.AudioPlayer;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainWindow {

    private static final int FFT_SIZE = 1024;

    private final Stage stage;
    private final AudioPlayer audioPlayer;
    private final AudioAnalyzer audioAnalyzer;
    private WaveformView waveformView;
    private SpectrumView spectrumView;
    private Label fileLabel;
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private AnimationTimer animationTimer;

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
        waveformView = new WaveformView(800, 200);
        spectrumView = new SpectrumView(800, 250);

        VBox visualizationBox = new VBox(10);
        visualizationBox.setPadding(new Insets(10));

        Label waveformLabel = new Label("Forme d'onde (Temps)");
        waveformLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12px;");

        Label spectrumLabel = new Label("Spectre (Frequence)");
        spectrumLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12px;");

        VBox waveformBox = new VBox(5, waveformLabel, waveformView);
        VBox spectrumBox = new VBox(5, spectrumLabel, spectrumView);

        visualizationBox.getChildren().addAll(waveformBox, spectrumBox);

        // Controls
        Button openButton = new Button("Ouvrir");
        playButton = new Button("Play");
        pauseButton = new Button("Pause");
        stopButton = new Button("Stop");

        playButton.setDisable(true);
        pauseButton.setDisable(true);
        stopButton.setDisable(true);

        fileLabel = new Label("Aucun fichier");
        fileLabel.setStyle("-fx-text-fill: #888888;");

        openButton.setOnAction(e -> openFile());
        playButton.setOnAction(e -> play());
        pauseButton.setOnAction(e -> pause());
        stopButton.setOnAction(e -> stop());

        HBox controlsBox = new HBox(10);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        controlsBox.setPadding(new Insets(10));
        controlsBox.getChildren().addAll(openButton, playButton, pauseButton, stopButton, fileLabel);

        String buttonStyle = "-fx-background-color: #404050; -fx-text-fill: white; -fx-padding: 8 16;";
        openButton.setStyle(buttonStyle);
        playButton.setStyle(buttonStyle);
        pauseButton.setStyle(buttonStyle);
        stopButton.setStyle(buttonStyle);

        // Main layout
        BorderPane root = new BorderPane();
        root.setCenter(visualizationBox);
        root.setBottom(controlsBox);
        root.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(root, 850, 550);
        stage.setTitle("Java Audio Visualiser");
        stage.setScene(scene);
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
            }
        };
        animationTimer.start();
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
                playButton.setDisable(false);
                stopButton.setDisable(false);
            } catch (Exception e) {
                fileLabel.setText("Erreur: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void play() {
        audioPlayer.play();
        playButton.setDisable(true);
        pauseButton.setDisable(false);
    }

    private void pause() {
        audioPlayer.pause();
        playButton.setDisable(false);
        pauseButton.setDisable(true);
    }

    private void stop() {
        audioPlayer.stop();
        playButton.setDisable(false);
        pauseButton.setDisable(true);
        currentWaveform = null;
        currentSpectrum = null;
        waveformView.setWaveform(null);
        waveformView.reset();
        spectrumView.setSpectrum(null);
        spectrumView.reset();
        waveformView.draw();
        spectrumView.draw();
    }

    private void cleanup() {
        animationTimer.stop();
        audioPlayer.close();
    }

    public void show() {
        stage.show();
    }
}
