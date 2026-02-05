package ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Visualization view for audio waveform (time domain).
 * Extends the abstract BaseView class.
 */
public class WaveformView extends BaseView {

    private double[] waveform;
    private double[] smoothedWaveform;
    private static final int DISPLAY_POINTS = 8192;
    private static final double SMOOTHING = 0.9;

    public WaveformView(double width, double height) {
        super(width, height);
        smoothedWaveform = new double[DISPLAY_POINTS];
        draw();
    }

    public void setWaveform(double[] waveform) {
        synchronized (lock) {
            this.waveform = waveform;
        }
    }

    @Override
    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();

        // Clear with theme background
        clearCanvas();

        // Draw grid
        drawGrid(gc);

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

        double midY = height / 2;

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

    @Override
    protected void drawGrid(GraphicsContext gc) {
        double width = getWidth();
        double height = getHeight();
        double midY = height / 2;

        gc.setStroke(colorTheme.getGridLineColor());
        gc.setLineWidth(0.5);
        gc.strokeLine(0, midY, width, midY);
        gc.strokeLine(0, midY - height / 4, width, midY - height / 4);
        gc.strokeLine(0, midY + height / 4, width, midY + height / 4);
    }

    @Override
    public void reset() {
        smoothedWaveform = new double[DISPLAY_POINTS];
    }
}
