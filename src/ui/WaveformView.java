package ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class WaveformView extends Canvas {

    private double[] waveform;
    private double[] smoothedWaveform;
    private final Object lock = new Object();
    private static final int DISPLAY_POINTS = 8192;
    private static final double SMOOTHING = 0.9;
    private ColorTheme colorTheme;

    public WaveformView(double width, double height) {
        super(width, height);
        smoothedWaveform = new double[DISPLAY_POINTS];
        colorTheme = ColorTheme.getTheme(ColorTheme.ThemeType.DARK_BLUE);
        widthProperty().addListener(e -> draw());
        heightProperty().addListener(e -> draw());
        draw();
    }

    public void setWaveform(double[] waveform) {
        synchronized (lock) {
            this.waveform = waveform;
        }
    }

    public void setColorTheme(ColorTheme theme) {
        this.colorTheme = theme;
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();

        // Background
        gc.setFill(colorTheme.getCanvasBackground());
        gc.fillRect(0, 0, width, height);

        // Grid lines
        gc.setStroke(colorTheme.getGridLineColor());
        gc.setLineWidth(0.5);
        double midY = height / 2;
        gc.strokeLine(0, midY, width, midY);
        gc.strokeLine(0, midY - height / 4, width, midY - height / 4);
        gc.strokeLine(0, midY + height / 4, width, midY + height / 4);

        double[] samples;
        synchronized (lock) {
            samples = waveform;
        }

        if (samples == null || samples.length == 0) {
            return;
        }

        // Downsample to fixed number of points with RMS averaging
        double[] downsampled = new double[DISPLAY_POINTS];
        int samplesPerPoint = samples.length / DISPLAY_POINTS;

        for (int i = 0; i < DISPLAY_POINTS; i++) {
            int start = i * samplesPerPoint;
            int end = Math.min(start + samplesPerPoint, samples.length);

            // Use peak value for better visual representation
            double max = 0;
            double sum = 0;
            for (int j = start; j < end; j++) {
                sum += samples[j];
                if (Math.abs(samples[j]) > Math.abs(max)) {
                    max = samples[j];
                }
            }
            // Mix average and peak for smooth but responsive display
            double avg = sum / (end - start);
            downsampled[i] = avg * 0.3 + max * 0.7;
        }

        // Apply temporal smoothing
        for (int i = 0; i < DISPLAY_POINTS; i++) {
            smoothedWaveform[i] = SMOOTHING * smoothedWaveform[i] + (1 - SMOOTHING) * downsampled[i];
        }

        // Draw filled bars with hue based on amplitude
        double barWidth = width / DISPLAY_POINTS;
        for (int i = 0; i < DISPLAY_POINTS; i++) {
            double x = i * barWidth;
            double amplitude = Math.abs(smoothedWaveform[i]);
            double normalizedAmp = Math.min(amplitude, 1.0);

            Color fillColor = colorTheme.getAmplitudeColor(normalizedAmp, 0.8, 0.9, 0.4);

            gc.setFill(fillColor);
            double barHeight = amplitude * (height / 2 - 10);
            gc.fillRect(x, midY - barHeight, barWidth, barHeight * 2);
        }

        // Draw waveform line segments with hue based on amplitude
        gc.setLineWidth(2.0);

        for (int i = 0; i < DISPLAY_POINTS - 1; i++) {
            double x1 = (double) i / DISPLAY_POINTS * width;
            double y1 = midY - smoothedWaveform[i] * (height / 2 - 10);
            double x2 = (double) (i + 1) / DISPLAY_POINTS * width;
            double y2 = midY - smoothedWaveform[i + 1] * (height / 2 - 10);

            // Color based on amplitude at this point
            double amplitude = Math.abs(smoothedWaveform[i]);
            double normalizedAmp = Math.min(amplitude, 1.0);

            Color lineColor = colorTheme.getAmplitudeColor(normalizedAmp, 1.0, 1.0);

            gc.setStroke(lineColor);
            gc.strokeLine(x1, y1, x2, y2);
        }
    }

    public void reset() {
        smoothedWaveform = new double[DISPLAY_POINTS];
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }
}
