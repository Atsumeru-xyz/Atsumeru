package com.atsumeru.web.util.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeExecutionLogger {
    private static final Logger logger = LoggerFactory.getLogger(CodeExecutionLogger.class.getSimpleName());
    long mStartTime;

    private CodeExecutionLogger() {
        mStartTime = System.currentTimeMillis();
    }

    public static CodeExecutionLogger start() {
        return new CodeExecutionLogger();
    }

    public void stop(String logMessage) {
        logger.info(logMessage + " executing time: " + (System.currentTimeMillis() - mStartTime) + "ms");
    }
}