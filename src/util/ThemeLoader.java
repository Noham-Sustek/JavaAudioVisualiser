package util;

import ui.ColorTheme;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that uses reflection to inspect and load color themes.
 * Demonstrates the use of Java Reflection API.
 */
public class ThemeLoader {

    private static final Logger logger = Logger.getInstance();

    /**
     * Get all available theme types using reflection.
     * This inspects the ThemeType enum at runtime.
     *
     * @return list of theme type names
     */
    public static List<String> getAvailableThemeNames() {
        List<String> themeNames = new ArrayList<>();

        try {
            // Use reflection to get the ThemeType enum class
            Class<?> themeTypeClass = Class.forName("ui.ColorTheme$ThemeType");

            // Check if it's an enum
            if (themeTypeClass.isEnum()) {
                // Get all enum constants using reflection
                Object[] enumConstants = themeTypeClass.getEnumConstants();

                // Get the getDisplayName method
                Method getDisplayNameMethod = themeTypeClass.getMethod("getDisplayName");

                for (Object constant : enumConstants) {
                    // Invoke getDisplayName() on each enum constant
                    String displayName = (String) getDisplayNameMethod.invoke(constant);
                    themeNames.add(displayName);
                    logger.debug("ThemeLoader", "Found theme: " + displayName);
                }
            }
        } catch (ClassNotFoundException e) {
            logger.error("ThemeLoader", "ThemeType class not found", e);
        } catch (NoSuchMethodException e) {
            logger.error("ThemeLoader", "getDisplayName method not found", e);
        } catch (Exception e) {
            logger.error("ThemeLoader", "Error loading themes via reflection", e);
        }

        return themeNames;
    }

    /**
     * Load a theme by its display name using reflection.
     *
     * @param displayName the display name of the theme
     * @return the ColorTheme, or default theme if not found
     */
    public static ColorTheme loadThemeByName(String displayName) {
        try {
            Class<?> themeTypeClass = Class.forName("ui.ColorTheme$ThemeType");

            if (themeTypeClass.isEnum()) {
                Object[] enumConstants = themeTypeClass.getEnumConstants();
                Method getDisplayNameMethod = themeTypeClass.getMethod("getDisplayName");

                for (Object constant : enumConstants) {
                    String name = (String) getDisplayNameMethod.invoke(constant);
                    if (name.equals(displayName)) {
                        // Found the matching theme, now get the ColorTheme
                        logger.info("ThemeLoader", "Loading theme: " + displayName);
                        return ColorTheme.getTheme((ColorTheme.ThemeType) constant);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("ThemeLoader", "Error loading theme by name: " + displayName, e);
        }

        logger.warn("ThemeLoader", "Theme not found, using default: " + displayName);
        return ColorTheme.getTheme(ColorTheme.ThemeType.DARK_BLUE);
    }

    /**
     * Get information about the ColorTheme class using reflection.
     *
     * @return formatted string with class information
     */
    public static String getThemeClassInfo() {
        StringBuilder info = new StringBuilder();

        try {
            Class<?> colorThemeClass = Class.forName("ui.ColorTheme");

            info.append("Class: ").append(colorThemeClass.getName()).append("\n");
            info.append("Package: ").append(colorThemeClass.getPackage().getName()).append("\n");

            // Get declared methods
            info.append("Methods:\n");
            Method[] methods = colorThemeClass.getDeclaredMethods();
            for (Method method : methods) {
                info.append("  - ").append(method.getName())
                    .append("(").append(getParameterTypes(method)).append(")")
                    .append(" : ").append(method.getReturnType().getSimpleName())
                    .append("\n");
            }

            logger.debug("ThemeLoader", "Retrieved ColorTheme class info");

        } catch (ClassNotFoundException e) {
            logger.error("ThemeLoader", "ColorTheme class not found", e);
            return "Error: ColorTheme class not found";
        }

        return info.toString();
    }

    /**
     * Get parameter types of a method as a formatted string.
     */
    private static String getParameterTypes(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramTypes[i].getSimpleName());
        }
        return sb.toString();
    }

    /**
     * Check if a theme exists by name using reflection.
     *
     * @param themeName the theme name to check
     * @return true if the theme exists
     */
    public static boolean themeExists(String themeName) {
        try {
            Class<?> themeTypeClass = Class.forName("ui.ColorTheme$ThemeType");
            Method valueOfMethod = themeTypeClass.getMethod("valueOf", String.class);

            // Try to get the enum constant
            valueOfMethod.invoke(null, themeName);
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
