package ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SpectrumView extends Canvas {

    private double[] spectrum;
    private double[] smoothedBars;
    private final Object lock = new Object();
    private static final int NUM_BARS = 64;
    private static final double SMOOTHING = 0.8;

    public SpectrumView(double width, double height) {
        super(width, height);
        smoothedBars = new double[NUM_BARS];
        widthProperty().addListener(e -> draw());
        heightProperty().addListener(e -> draw());
        draw();
    }

    public void setSpectrum(double[] spectrum) {
        synchronized (lock) {
            this.spectrum = spectrum;
        }
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();

        // Background
        gc.setFill(Color.rgb(20, 20, 30));
        gc.fillRect(0, 0, width, height);

        // Grid lines
        gc.setStroke(Color.rgb(50, 50, 70));
        gc.setLineWidth(0.5);
        for (int i = 1; i <= 4; i++) {
            double y = height - (i * height / 5);
            gc.strokeLine(0, y, width, y);
        }

        double[] data;
        synchronized (lock) {
            data = spectrum;
        }

        if (data == null || data.length == 0) {
            return;
        }

        // Logarithmic frequency binning
        double[] newBars = new double[NUM_BARS];
        double minFreq = 1;
        double maxFreq = data.length;
        double logMin = Math.log10(minFreq);
        double logMax = Math.log10(maxFreq);

        for (int i = 0; i < NUM_BARS; i++) {
            double logStart = logMin + (logMax - logMin) * i / NUM_BARS;
            double logEnd = logMin + (logMax - logMin) * (i + 1) / NUM_BARS;
            int startIdx = (int) Math.pow(10, logStart);
            int endIdx = (int) Math.pow(10, logEnd);

            startIdx = Math.max(0, Math.min(startIdx, data.length - 1));
            endIdx = Math.max(startIdx + 1, Math.min(endIdx, data.length));

            double sum = 0;
            for (int j = startIdx; j < endIdx; j++) {
                sum += data[j];
            }
            newBars[i] = sum / (endIdx - startIdx);
        }

        // Apply smoothing
        for (int i = 0; i < NUM_BARS; i++) {
            smoothedBars[i] = SMOOTHING * smoothedBars[i] + (1 - SMOOTHING) * newBars[i];
        }

        // Draw bars
        double barWidth = width / NUM_BARS;
        double gap = 2;

        for (int i = 0; i < NUM_BARS; i++) {
            // Magnitudes are already log-scaled, normalize to 0-1 range
            double normalizedHeight = Math.min(smoothedBars[i] / 2.5, 1.0);
            double barHeight = normalizedHeight * (height - 10);
            double x = i * barWidth + gap / 2;
            double y = height - barHeight;

            // Dynamic hue based on magnitude (green → yellow → red)
            double hue = 0.33 - normalizedHeight * 0.33;
            Color barColor = Color.hsb(hue * 360, 1.0, 1.0);

            gc.setFill(barColor);
            gc.fillRect(x, y, barWidth - gap, barHeight);

            // Top highlight
            gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.3));
            gc.fillRect(x, y, barWidth - gap, 2);
        }
    }

    public void reset() {
        smoothedBars = new double[NUM_BARS];
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
