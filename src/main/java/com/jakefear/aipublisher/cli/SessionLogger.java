package com.jakefear.aipublisher.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Detailed session logger that writes to session.log for diagnostic purposes.
 * All method calls, state changes, and exceptions are logged with timestamps.
 */
public class SessionLogger {
    private static final Logger log = LoggerFactory.getLogger(SessionLogger.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final PrintWriter logWriter;
    private final Path logPath;
    private final String sessionId;

    public SessionLogger(String sessionId) {
        this.sessionId = sessionId;
        this.logPath = Path.of("session.log");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(logPath.toFile(), true), true);
            writer.println();
            writer.println("=" .repeat(80));
            writer.printf("SESSION STARTED: %s (ID: %s)%n", timestamp(), sessionId);
            writer.println("=" .repeat(80));
            writer.println();
        } catch (IOException e) {
            log.warn("Failed to create session log file: {}", e.getMessage());
        }
        this.logWriter = writer;
    }

    public void info(String message) {
        write("INFO", message);
    }

    public void info(String format, Object... args) {
        write("INFO", String.format(format, args));
    }

    public void debug(String message) {
        write("DEBUG", message);
    }

    public void debug(String format, Object... args) {
        write("DEBUG", String.format(format, args));
    }

    public void warn(String message) {
        write("WARN", message);
    }

    public void warn(String format, Object... args) {
        write("WARN", String.format(format, args));
    }

    public void error(String message) {
        write("ERROR", message);
    }

    public void error(String format, Object... args) {
        write("ERROR", String.format(format, args));
    }

    public void error(String message, Throwable throwable) {
        write("ERROR", message);
        writeException(throwable);
    }

    public void phase(String phaseName, int current, int total) {
        if (logWriter == null) return;
        logWriter.println();
        logWriter.println("-".repeat(60));
        logWriter.printf("[%s] PHASE %d/%d: %s%n", timestamp(), current, total, phaseName);
        logWriter.println("-".repeat(60));
    }

    public void userInput(String prompt, String value) {
        write("INPUT", String.format("'%s' => '%s'", prompt, value));
    }

    public void action(String action, String details) {
        write("ACTION", String.format("%s: %s", action, details));
    }

    public void state(String description) {
        write("STATE", description);
    }

    public void apiCall(String service, String operation) {
        write("API", String.format("%s.%s()", service, operation));
    }

    public void apiResponse(String service, String summary) {
        write("API-RESP", String.format("%s: %s", service, summary));
    }

    public void exception(Throwable t) {
        write("EXCEPTION", t.getClass().getName() + ": " + t.getMessage());
        writeException(t);
    }

    public void sessionEnd(boolean success, String message) {
        if (logWriter == null) return;
        logWriter.println();
        logWriter.println("=".repeat(80));
        logWriter.printf("SESSION %s: %s (ID: %s)%n",
                success ? "COMPLETED" : "FAILED", timestamp(), sessionId);
        logWriter.printf("Result: %s%n", message);
        logWriter.println("=".repeat(80));
        logWriter.println();
        logWriter.flush();
    }

    public void close() {
        if (logWriter != null) {
            logWriter.close();
        }
    }

    private void write(String level, String message) {
        if (logWriter == null) return;
        logWriter.printf("[%s] [%-8s] %s%n", timestamp(), level, message);
        logWriter.flush();
    }

    private void writeException(Throwable t) {
        if (logWriter == null) return;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        logWriter.println("--- Stack Trace ---");
        logWriter.println(sw.toString());
        logWriter.println("--- End Stack Trace ---");
        logWriter.flush();
    }

    private String timestamp() {
        return TIMESTAMP_FORMAT.format(Instant.now());
    }

    public Path getLogPath() {
        return logPath;
    }
}
