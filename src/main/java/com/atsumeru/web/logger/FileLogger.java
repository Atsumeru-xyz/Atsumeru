package com.atsumeru.web.logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class FileLogger {

    public static Logger createLogger(String loggerName, String filePath) {
        Logger logger = Logger.getLogger(loggerName);

        try {
            // This block configure the logger with handler and formatter
            FileHandler fh = new FileHandler(filePath, true);
            logger.addHandler(fh);
            Formatter formatter = new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.valueOf(record.getLevel()) + ':' + record.getMessage() + '\n';
                }
            };
            fh.setFormatter(formatter);

            logger.setUseParentHandlers(false);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

        return logger;
    }
}
