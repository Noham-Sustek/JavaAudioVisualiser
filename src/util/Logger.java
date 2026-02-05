package util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple file-based logger for the application.
 * Logs messages to both console and a log file.
 */
public class Logger {

    private static final String LOG_FILE = "visualizer.log";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static Logger instance;
    private final File logFile;
    private final boolean consoleOutput;

    /**
     * Log levels.
     */
    public enum Level {
        DEBUG("[DEBUG]"),
        INFO("[INFO]"),
        WARN("[WARN]"),
        ERROR("[ERROR]");

        private final String prefix;

        Level(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    private Logger() {
        String userHome = System.getProperty("user.home");
        File configDir = new File(userHome, ".java-audio-visualizer");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.logFile = new File(configDir, LOG_FILE);
        this.consoleOutput = true;

        // Log startup
        info("Logger", "Application started");
    }

    /**
     * Get the singleton logger instance.
     */
    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    /**
     * Log a message at the specified level.
     *
     * @param level   the log level
     * @param source  the source class or component
     * @param message the message to log
     */
    public void log(Level level, String source, String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logLine = String.format("%s %s [%s] %s", timestamp, level.getPrefix(), source, message);

        // Write to console
        if (consoleOutput) {
            if (level == Level.ERROR || level == Level.WARN) {
                System.err.println(logLine);
            } else {
                System.out.println(logLine);
            }
        }

        // Write to file
        writeToFile(logLine);
    }

    /**
     * Log a message with an exception.
     *
     * @param level     the log level
     * @param source    the source class or component
     * @param message   the message to log
     * @param throwable the exception to log
     */
    public void log(Level level, String source, String message, Throwable throwable) {
        log(level, source, message + " - " + throwable.getMessage());

        // Write stack trace to file
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            writeToFile(sw.toString());
        } catch (IOException e) {
            System.err.println("Failed to write stack trace to log: " + e.getMessage());
        }
    }

    /**
     * Log a debug message.
     */
    public void debug(String source, String message) {
        log(Level.DEBUG, source, message);
    }

    /**
     * Log an info message.
     */
    public void info(String source, String message) {
        log(Level.INFO, source, message);
    }

    /**
     * Log a warning message.
     */
    public void warn(String source, String message) {
        log(Level.WARN, source, message);
    }

    /**
     * Log a warning message with an exception.
     */
    public void warn(String source, String message, Throwable throwable) {
        log(Level.WARN, source, message, throwable);
    }

    /**
     * Log an error message.
     */
    public void error(String source, String message) {
        log(Level.ERROR, source, message);
    }

    /**
     * Log an error message with an exception.
     */
    public void error(String source, String message, Throwable throwable) {
        log(Level.ERROR, source, message, throwable);
    }

    private synchronized void writeToFile(String line) {
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(line);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    /**
     * Get the path to the log file.
     */
    public String getLogFilePath() {
        return logFile.getAbsolutePath();
    }
}
