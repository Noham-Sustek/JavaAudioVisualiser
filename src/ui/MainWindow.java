package ui;

import audio.AudioAnalyzer;
import audio.AudioPlayer;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class MainWindow {

    private static final int FFT_SIZE = 8192;

    private final Stage stage;
    private final AudioPlayer audioPlayer;
    private final AudioAnalyzer audioAnalyzer;
    private final Settings settings;
    private WaveformView waveformView;
    private SpectrumView spectrumView;
    private Label fileLabel;
    private Button playPauseButton;
    private Button stopButton;
    private Button openButton;
    private MenuButton historyButton;
    private Slider progressSlider;
    private boolean isPlaying = false;
    private Label timeLabel;
    private Label waveformLabel;
    private Label spectrumLabel;
    private CheckBox linearScaleCheckBox;
    private CheckBox mirrorCheckBox;
    private ComboBox<ColorTheme.ThemeType> themeComboBox;
    private BorderPane root;
    private AnimationTimer animationTimer;
    private boolean sliderDragging = false;

    private volatile double[] currentWaveform;
    private volatile double[] currentSpectrum;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.audioPlayer = new AudioPlayer();
        this.audioAnalyzer = new AudioAnalyzer(FFT_SIZE);
        this.settings = Settings.getInstance();

        audioPlayer.addAudioDataListener(audioAnalyzer);
        audioAnalyzer.setAnalysisListener((waveform, spectrum) -> {
            currentWaveform = waveform;
            currentSpectrum = spectrum;
        });

        setupUI();
        loadSettings();
        setupAnimationTimer();
    }

    private void setupUI() {
        // Visualizations
        waveformView = new WaveformView(1500, 300);
        spectrumView = new SpectrumView(1500, 350);

        VBox visualizationBox = new VBox(10);
        visualizationBox.setPadding(new Insets(10));
        VBox.setVgrow(visualizationBox, Priority.ALWAYS);

        waveformLabel = new Label("Forme d'onde (Temps)");

        spectrumLabel = new Label("Spectre (Frequence)");

        linearScaleCheckBox = new CheckBox("Echelle lineaire");
        linearScaleCheckBox.setOnAction(e -> {
            boolean selected = linearScaleCheckBox.isSelected();
            spectrumView.setLinearScale(selected);
            settings.setLinearScale(selected);
        });

        mirrorCheckBox = new CheckBox("Miroir");
        mirrorCheckBox.setOnAction(e -> {
            boolean selected = mirrorCheckBox.isSelected();
            spectrumView.setMirrored(selected);
            settings.setMirrored(selected);
        });

        themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll(ColorTheme.ThemeType.values());
        themeComboBox.setOnAction(e -> {
            ColorTheme.ThemeType selectedTheme = themeComboBox.getValue();
            if (selectedTheme != null) {
                settings.setTheme(selectedTheme);
                applyTheme(ColorTheme.getTheme(selectedTheme));
            }
        });

        HBox spectrumHeader = new HBox(15, spectrumLabel, linearScaleCheckBox, mirrorCheckBox, themeComboBox);
        spectrumHeader.setAlignment(Pos.CENTER_LEFT);

        VBox waveformBox = new VBox(5, waveformLabel, waveformView);
        VBox spectrumBox = new VBox(5, spectrumHeader, spectrumView);
        VBox.setVgrow(waveformBox, Priority.ALWAYS);
        VBox.setVgrow(spectrumBox, Priority.ALWAYS);

        visualizationBox.getChildren().addAll(waveformBox, spectrumBox);

        // Controls
        openButton = new Button("Ouvrir");
        playPauseButton = new Button("\u25B6"); // Play icon
        stopButton = new Button("\u25A0"); // Stop icon
        historyButton = new MenuButton("Historique");

        playPauseButton.setDisable(true);
        stopButton.setDisable(true);

        fileLabel = new Label("Aucun fichier");

        progressSlider = new Slider(0, 1, 0);
        progressSlider.setPrefWidth(300);

        progressSlider.setOnMousePressed(e -> sliderDragging = true);
        progressSlider.setOnMouseReleased(e -> {
            sliderDragging = false;
            audioPlayer.seekTo(progressSlider.getValue());
        });

        timeLabel = new Label("0:00 / 0:00");

        openButton.setOnAction(e -> openFile());
        playPauseButton.setOnAction(e -> togglePlayPause());
        stopButton.setOnAction(e -> stop());

        updateHistoryMenu();

        HBox controlsBox = new HBox(10);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        controlsBox.setPadding(new Insets(10));
        controlsBox.getChildren().addAll(openButton, playPauseButton, stopButton, fileLabel, progressSlider, timeLabel, historyButton);

        // Main layout
        root = new BorderPane();
        root.setCenter(visualizationBox);
        root.setBottom(controlsBox);

        Scene scene = new Scene(root, 950, 750);
        stage.setTitle("Java Audio Visualiser");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setOnCloseRequest(e -> cleanup());
    }

    private void loadSettings() {
        // Load and apply saved settings
        linearScaleCheckBox.setSelected(settings.isLinearScale());
        spectrumView.setLinearScale(settings.isLinearScale());

        mirrorCheckBox.setSelected(settings.isMirrored());
        spectrumView.setMirrored(settings.isMirrored());

        themeComboBox.setValue(settings.getTheme());
        applyTheme(settings.getColorTheme());
    }

    private void applyTheme(ColorTheme theme) {
        // Apply theme to views
        waveformView.setColorTheme(theme);
        spectrumView.setColorTheme(theme);

        // Apply theme to UI elements
        String bgHex = ColorTheme.toHex(theme.getBackgroundColor());
        String btnBgHex = ColorTheme.toHex(theme.getButtonBackground());
        String textHex = ColorTheme.toHex(theme.getTextColor());
        String secondaryTextHex = ColorTheme.toHex(theme.getSecondaryTextColor());

        root.setStyle("-fx-background-color: " + bgHex + ";");

        String labelStyle = "-fx-text-fill: " + textHex + "; -fx-font-size: 12px;";
        waveformLabel.setStyle(labelStyle);
        spectrumLabel.setStyle(labelStyle);
        linearScaleCheckBox.setStyle(labelStyle);
        mirrorCheckBox.setStyle(labelStyle);

        String buttonStyle = "-fx-background-color: " + btnBgHex + "; -fx-text-fill: white; -fx-padding: 8 16; -fx-font-size: 14px;";
        openButton.setStyle(buttonStyle);
        playPauseButton.setStyle(buttonStyle);
        stopButton.setStyle(buttonStyle);
        historyButton.setStyle(buttonStyle);

        fileLabel.setStyle("-fx-text-fill: " + secondaryTextHex + ";");
        timeLabel.setStyle("-fx-text-fill: " + secondaryTextHex + "; -fx-font-size: 11px;");
        progressSlider.setStyle("-fx-control-inner-background: " + btnBgHex + ";");

        // Style the combo box
        themeComboBox.setStyle("-fx-background-color: " + btnBgHex + "; -fx-text-fill: white;");

        // Force redraw
        waveformView.draw();
        spectrumView.draw();
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
            loadFile(file);
        }
    }

    private void loadFile(File file) {
        try {
            audioPlayer.loadFile(file);
            fileLabel.setText(file.getName());
            playPauseButton.setDisable(false);
            stopButton.setDisable(false);
            // Add to history
            settings.addToSongHistory(file.getAbsolutePath());
            updateHistoryMenu();
        } catch (Exception e) {
            fileLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateHistoryMenu() {
        historyButton.getItems().clear();
        List<String> history = settings.getSongHistory();

        if (history.isEmpty()) {
            MenuItem emptyItem = new MenuItem("(Aucun historique)");
            emptyItem.setDisable(true);
            historyButton.getItems().add(emptyItem);
        } else {
            for (String filePath : history) {
                File file = new File(filePath);
                MenuItem item = new MenuItem(file.getName());
                item.setOnAction(e -> {
                    if (file.exists()) {
                        loadFile(file);
                    } else {
                        fileLabel.setText("Fichier introuvable");
                    }
                });
                historyButton.getItems().add(item);
            }
        }
    }

    private void togglePlayPause() {
        if (isPlaying) {
            audioPlayer.pause();
            playPauseButton.setText("\u25B6"); // Play
            isPlaying = false;
        } else {
            audioPlayer.play();
            playPauseButton.setText("\u23F8"); // Pause
            isPlaying = true;
        }
    }

    private void stop() {
        audioPlayer.stop();
        playPauseButton.setText("\u25B6"); // Play
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
