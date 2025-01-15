package xyz.atsumeru.web.logger;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class FileLogger {

    public static Logger createLogger(String loggerName, File file) {
        Logger logger = Logger.getLogger(loggerName);

        try {
            // This block configure the logger with handler and formatter
            FileHandler fileHandler = new FileHandler(file.getAbsolutePath(), true);
            logger.addHandler(fileHandler);
            Formatter formatter = new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.valueOf(record.getLevel()) + ':' + record.getMessage() + '\n';
                }
            };
            fileHandler.setFormatter(formatter);

            logger.setUseParentHandlers(false);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

        return logger;
    }
}
