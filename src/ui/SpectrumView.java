package ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SpectrumView extends Canvas {

    private double[] spectrum;
    private double[] smoothedBars;
    private final Object lock = new Object();
    private static final int NUM_BARS = 256;
    private static final double SMOOTHING = 0.8;
    private boolean linearScale = false;
    private boolean mirrored = false;

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

    public void setLinearScale(boolean linear) {
        this.linearScale = linear;
    }

    public boolean isLinearScale() {
        return linearScale;
    }

    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    public boolean isMirrored() {
        return mirrored;
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
        if (mirrored) {
            double centerY = height / 2;
            for (int i = 1; i <= 2; i++) {
                double offset = i * height / 5;
                gc.strokeLine(0, centerY - offset, width, centerY - offset);
                gc.strokeLine(0, centerY + offset, width, centerY + offset);
            }
            gc.setStroke(Color.rgb(80, 80, 100));
            gc.setLineWidth(1);
            gc.strokeLine(0, centerY, width, centerY);
        } else {
            for (int i = 1; i <= 4; i++) {
                double y = height - (i * height / 5);
                gc.strokeLine(0, y, width, y);
            }
        }

        double[] data;
        synchronized (lock) {
            data = spectrum;
        }

        if (data == null || data.length == 0) {
            return;
        }

        // Frequency binning
        double[] newBars = new double[NUM_BARS];
        double maxFreq = data.length;

        for (int i = 0; i < NUM_BARS; i++) {
            int startIdx, endIdx;

            if (linearScale) {
                // Linear scale: equal frequency width per bar
                startIdx = (int) ((double) i / NUM_BARS * maxFreq);
                endIdx = (int) ((double) (i + 1) / NUM_BARS * maxFreq);
            } else {
                // Logarithmic scale: more resolution for low frequencies
                double t0 = (double) i / NUM_BARS;
                double t1 = (double) (i + 1) / NUM_BARS;
                double power = 2.5;
                startIdx = (int) (Math.pow(t0, power) * maxFreq);
                endIdx = (int) (Math.pow(t1, power) * maxFreq);
            }

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
        double gap = 1;
        double centerY = height / 2;

        for (int i = 0; i < NUM_BARS; i++) {
            // Magnitudes are already log-scaled, normalize to 0-1 range
            double normalizedHeight = Math.min(smoothedBars[i] / 2.5, 1.0);
            double x = i * barWidth + gap / 2;

            // Dynamic hue based on magnitude (green → yellow → red)
            double hue = 0.33 - normalizedHeight * 0.33;
            Color barColor = Color.hsb(hue * 360, 1.0, 1.0);

            if (mirrored) {
                double barHeight = normalizedHeight * (height / 2 - 5);

                gc.setFill(barColor);
                gc.fillRect(x, centerY - barHeight, barWidth - gap, barHeight);
                gc.fillRect(x, centerY, barWidth - gap, barHeight);

                gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.3));
                gc.fillRect(x, centerY - barHeight, barWidth - gap, 2);
                gc.fillRect(x, centerY + barHeight - 2, barWidth - gap, 2);
            } else {
                double barHeight = normalizedHeight * (height - 10);
                double y = height - barHeight;

                gc.setFill(barColor);
                gc.fillRect(x, y, barWidth - gap, barHeight);

                gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.3));
                gc.fillRect(x, y, barWidth - gap, 2);
            }
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
