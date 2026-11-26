package controller;

import java.io.IOException;
import java.util.logging.*;

public class LoggerServiceController {
    private static final Logger logger = Logger.getLogger(LoggerServiceController.class.getName());
    static {
        try {
            // Log rotation: 1MB per file, max 10 files
            FileHandler fileHandler = new FileHandler("system_logs.%g.log", 1024 * 1024, 10, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize logger", e);
        }
    }

    public static void logAdminActivity(String message) {
        logger.info("ADMIN: " + message);
    }

    public static void logException(String context, Exception e) {
        logger.log(Level.SEVERE, context, e);
    }

    public static void logActivity(String message) {
        logger.info(message);
    }
}
