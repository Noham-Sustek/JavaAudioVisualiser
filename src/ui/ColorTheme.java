package ui;

import javafx.scene.paint.Color;

public class ColorTheme {

    public enum ThemeType {
        DARK_BLUE("Bleu sombre"),
        OCEAN("Ocean"),
        SUNSET("Coucher de soleil"),
        FOREST("Foret"),
        PURPLE("Violet"),
        MONOCHROME("Monochrome");

        private final String displayName;

        ThemeType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final ThemeType type;
    private final Color backgroundColor;
    private final Color canvasBackground;
    private final Color gridLineColor;
    private final Color centerLineColor;
    private final Color buttonBackground;
    private final Color textColor;
    private final Color secondaryTextColor;
    private final double hueStart;    // Starting hue for amplitude coloring (0-1)
    private final double hueEnd;      // Ending hue for amplitude coloring (0-1)

    private ColorTheme(ThemeType type, Color backgroundColor, Color canvasBackground,
                       Color gridLineColor, Color centerLineColor, Color buttonBackground,
                       Color textColor, Color secondaryTextColor, double hueStart, double hueEnd) {
        this.type = type;
        this.backgroundColor = backgroundColor;
        this.canvasBackground = canvasBackground;
        this.gridLineColor = gridLineColor;
        this.centerLineColor = centerLineColor;
        this.buttonBackground = buttonBackground;
        this.textColor = textColor;
        this.secondaryTextColor = secondaryTextColor;
        this.hueStart = hueStart;
        this.hueEnd = hueEnd;
    }

    public static ColorTheme getTheme(ThemeType type) {
        switch (type) {
            case OCEAN:
                return new ColorTheme(
                        type,
                        Color.rgb(10, 25, 40),      // Dark navy background
                        Color.rgb(15, 30, 50),      // Canvas background
                        Color.rgb(40, 70, 100),     // Grid lines
                        Color.rgb(60, 100, 140),    // Center line
                        Color.rgb(30, 60, 90),      // Button background
                        Color.rgb(150, 200, 255),   // Text color
                        Color.rgb(100, 150, 200),   // Secondary text
                        0.55,                        // Cyan (low amplitude)
                        0.95                         // Magenta (high amplitude) - opposite spectrum
                );

            case SUNSET:
                return new ColorTheme(
                        type,
                        Color.rgb(35, 20, 25),      // Dark red-brown background
                        Color.rgb(40, 25, 30),      // Canvas background
                        Color.rgb(80, 50, 60),      // Grid lines
                        Color.rgb(120, 70, 80),     // Center line
                        Color.rgb(70, 40, 50),      // Button background
                        Color.rgb(255, 200, 180),   // Text color
                        Color.rgb(200, 150, 130),   // Secondary text
                        0.15,                        // Yellow-orange (low amplitude)
                        0.85                         // Purple (high amplitude) - opposite spectrum
                );

            case FOREST:
                return new ColorTheme(
                        type,
                        Color.rgb(15, 30, 20),      // Dark green background
                        Color.rgb(20, 35, 25),      // Canvas background
                        Color.rgb(50, 80, 60),      // Grid lines
                        Color.rgb(70, 110, 80),     // Center line
                        Color.rgb(40, 70, 50),      // Button background
                        Color.rgb(180, 230, 190),   // Text color
                        Color.rgb(130, 180, 140),   // Secondary text
                        0.33,                        // Green (low amplitude)
                        0.83                         // Magenta-pink (high amplitude) - opposite spectrum
                );

            case PURPLE:
                return new ColorTheme(
                        type,
                        Color.rgb(25, 15, 35),      // Dark purple background
                        Color.rgb(30, 20, 40),      // Canvas background
                        Color.rgb(60, 45, 80),      // Grid lines
                        Color.rgb(90, 70, 120),     // Center line
                        Color.rgb(50, 35, 70),      // Button background
                        Color.rgb(220, 190, 255),   // Text color
                        Color.rgb(170, 140, 200),   // Secondary text
                        0.75,                        // Purple (low amplitude)
                        0.25                         // Yellow-green (high amplitude) - opposite spectrum
                );

            case MONOCHROME:
                return new ColorTheme(
                        type,
                        Color.rgb(20, 20, 20),      // Dark gray background
                        Color.rgb(25, 25, 25),      // Canvas background
                        Color.rgb(60, 60, 60),      // Grid lines
                        Color.rgb(90, 90, 90),      // Center line
                        Color.rgb(50, 50, 50),      // Button background
                        Color.rgb(200, 200, 200),   // Text color
                        Color.rgb(140, 140, 140),   // Secondary text
                        0.0,                         // Not used (grayscale)
                        0.0                          // Not used (grayscale)
                );

            case DARK_BLUE:
            default:
                return new ColorTheme(
                        type,
                        Color.rgb(26, 26, 46),      // #1a1a2e
                        Color.rgb(20, 20, 30),      // Canvas background
                        Color.rgb(50, 50, 70),      // Grid lines
                        Color.rgb(80, 80, 100),     // Center line
                        Color.rgb(64, 64, 80),      // #404050
                        Color.rgb(170, 170, 170),   // #AAAAAA
                        Color.rgb(136, 136, 136),   // #888888
                        0.33,                        // Green (low amplitude)
                        0.0                          // Red (high amplitude)
                );
        }
    }

    public ThemeType getType() {
        return type;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getCanvasBackground() {
        return canvasBackground;
    }

    public Color getGridLineColor() {
        return gridLineColor;
    }

    public Color getCenterLineColor() {
        return centerLineColor;
    }

    public Color getButtonBackground() {
        return buttonBackground;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getSecondaryTextColor() {
        return secondaryTextColor;
    }

    /**
     * Get color based on normalized amplitude (0-1).
     * For most themes, interpolates hue from hueStart to hueEnd.
     * For monochrome, returns grayscale based on brightness.
     */
    public Color getAmplitudeColor(double normalizedAmplitude, double saturation, double brightness, double alpha) {
        if (type == ThemeType.MONOCHROME) {
            double gray = 0.3 + normalizedAmplitude * 0.7;
            return Color.gray(gray, alpha);
        }

        double hue = hueStart - normalizedAmplitude * (hueStart - hueEnd);
        return Color.hsb(hue * 360, saturation, brightness, alpha);
    }

    public Color getAmplitudeColor(double normalizedAmplitude, double saturation, double brightness) {
        return getAmplitudeColor(normalizedAmplitude, saturation, brightness, 1.0);
    }

    // Helper to convert Color to CSS hex string
    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
