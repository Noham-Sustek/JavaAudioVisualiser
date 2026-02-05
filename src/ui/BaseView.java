package ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

/**
 * Abstract base class for visualization views.
 * Provides common functionality for waveform and spectrum displays.
 */
public abstract class BaseView extends Canvas {

    protected ColorTheme colorTheme;
    protected final Object lock = new Object();

    public BaseView(double width, double height) {
        super(width, height);
        this.colorTheme = ColorTheme.getTheme(ColorTheme.ThemeType.DARK_BLUE);

        // Common listener setup
        widthProperty().addListener(e -> draw());
        heightProperty().addListener(e -> draw());
    }

    /**
     * Set the color theme for this view.
     */
    public void setColorTheme(ColorTheme theme) {
        this.colorTheme = theme;
    }

    /**
     * Get the current color theme.
     */
    public ColorTheme getColorTheme() {
        return colorTheme;
    }

    /**
     * Draw the visualization. Must be implemented by subclasses.
     */
    public abstract void draw();

    /**
     * Reset the view state. Must be implemented by subclasses.
     */
    public abstract void reset();

    /**
     * Clear the canvas with the current theme's background color.
     */
    protected void clearCanvas() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(colorTheme.getCanvasBackground());
        gc.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Draw grid lines on the canvas.
     */
    protected abstract void drawGrid(GraphicsContext gc);

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
