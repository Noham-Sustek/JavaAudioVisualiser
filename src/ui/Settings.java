package ui;

import util.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Settings {

    private static final String SETTINGS_FILE = "visualizer_settings.properties";
    private static final String HISTORY_FILE = "song_history.txt";
    private static final int MAX_HISTORY_SIZE = 20;

    private static final Logger logger = Logger.getInstance();

    private ColorTheme.ThemeType theme = ColorTheme.ThemeType.DARK_BLUE;
    private boolean linearScale = false;
    private boolean mirrored = false;
    private List<String> songHistory = new ArrayList<>();

    private static Settings instance;

    private Settings() {
        load();
        loadSongHistory();
    }

    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    public ColorTheme.ThemeType getTheme() {
        return theme;
    }

    public void setTheme(ColorTheme.ThemeType theme) {
        this.theme = theme;
        save();
    }

    public boolean isLinearScale() {
        return linearScale;
    }

    public void setLinearScale(boolean linearScale) {
        this.linearScale = linearScale;
        save();
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
        save();
    }

    public ColorTheme getColorTheme() {
        return ColorTheme.getTheme(theme);
    }

    public List<String> getSongHistory() {
        return new ArrayList<>(songHistory);
    }

    public void addToSongHistory(String filePath) {
        // Remove if already exists (to move it to the top)
        songHistory.remove(filePath);
        // Add to the beginning
        songHistory.add(0, filePath);
        // Keep only the most recent songs
        while (songHistory.size() > MAX_HISTORY_SIZE) {
            songHistory.remove(songHistory.size() - 1);
        }
        saveSongHistory();
        logger.debug("Settings", "Added to song history: " + filePath);
    }

    private void load() {
        File file = getSettingsFile();
        if (!file.exists()) {
            logger.info("Settings", "No settings file found, using defaults");
            return;
        }

        Properties props = new Properties();
        try (InputStream is = new FileInputStream(file)) {
            props.load(is);

            String themeStr = props.getProperty("theme", "DARK_BLUE");
            try {
                theme = ColorTheme.ThemeType.valueOf(themeStr);
            } catch (IllegalArgumentException e) {
                theme = ColorTheme.ThemeType.DARK_BLUE;
                logger.warn("Settings", "Invalid theme in settings, using default: " + themeStr);
            }

            linearScale = Boolean.parseBoolean(props.getProperty("linearScale", "false"));
            mirrored = Boolean.parseBoolean(props.getProperty("mirrored", "false"));

            logger.info("Settings", "Settings loaded from file");

        } catch (IOException e) {
            logger.error("Settings", "Could not load settings", e);
        }
    }

    private void save() {
        Properties props = new Properties();
        props.setProperty("theme", theme.name());
        props.setProperty("linearScale", String.valueOf(linearScale));
        props.setProperty("mirrored", String.valueOf(mirrored));

        File file = getSettingsFile();
        try (OutputStream os = new FileOutputStream(file)) {
            props.store(os, "Java Audio Visualizer Settings");
            logger.debug("Settings", "Settings saved");
        } catch (IOException e) {
            logger.error("Settings", "Could not save settings", e);
        }
    }

    private void loadSongHistory() {
        File file = getHistoryFile();
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    songHistory.add(line);
                }
            }
            logger.info("Settings", "Song history loaded: " + songHistory.size() + " entries");
        } catch (IOException e) {
            logger.error("Settings", "Could not load song history", e);
        }
    }

    private void saveSongHistory() {
        File file = getHistoryFile();
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (String song : songHistory) {
                writer.println(song);
            }
            logger.debug("Settings", "Song history saved");
        } catch (IOException e) {
            logger.error("Settings", "Could not save song history", e);
        }
    }

    private File getSettingsFile() {
        return new File(getConfigDir(), SETTINGS_FILE);
    }

    private File getHistoryFile() {
        return new File(getConfigDir(), HISTORY_FILE);
    }

    private File getConfigDir() {
        String userHome = System.getProperty("user.home");
        File configDir = new File(userHome, ".java-audio-visualizer");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        return configDir;
    }
}
