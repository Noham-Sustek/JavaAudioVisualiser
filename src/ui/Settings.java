package ui;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Settings {

    private static final String SETTINGS_FILE = "visualizer_settings.properties";
    private static final String HISTORY_FILE = "song_history.txt";
    private static final int MAX_HISTORY_SIZE = 20;

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
    }

    private void load() {
        File file = getSettingsFile();
        if (!file.exists()) {
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
            }

            linearScale = Boolean.parseBoolean(props.getProperty("linearScale", "false"));
            mirrored = Boolean.parseBoolean(props.getProperty("mirrored", "false"));

        } catch (IOException e) {
            System.err.println("Could not load settings: " + e.getMessage());
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
        } catch (IOException e) {
            System.err.println("Could not save settings: " + e.getMessage());
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
        } catch (IOException e) {
            System.err.println("Could not load song history: " + e.getMessage());
        }
    }

    private void saveSongHistory() {
        File file = getHistoryFile();
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (String song : songHistory) {
                writer.println(song);
            }
        } catch (IOException e) {
            System.err.println("Could not save song history: " + e.getMessage());
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
