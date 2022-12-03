package com.atsumeru.web;

import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.util.WorkspaceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;

import java.util.Arrays;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class, JacksonAutoConfiguration.class })
public class AtsumeruApplication implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(AtsumeruApplication.class.getSimpleName());
    private static final String DB_PATH = WorkspaceUtils.DATABASES_DIR + "library.db";

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        setOrmLiteLogLevel();
        WorkspaceUtils.configureWorkspace();
        BooksDatabaseRepository.connect(DB_PATH);

        context = SpringApplication.run(AtsumeruApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doAfterStart() {
        // Do nothing
    }

    static void setOrmLiteLogLevel() {
        System.setProperty("com.j256.ormlite.logger.type", "LOCAL");
        System.setProperty("com.j256.ormlite.logger.level", "FATAL");
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Application started with command-line arguments: {}", Arrays.toString(args.getSourceArgs()));
    }

    public static void restart() {
        ApplicationArguments args = context.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            context.close();
            context = SpringApplication.run(AtsumeruApplication.class, args.getSourceArgs());
        });

        thread.setDaemon(false);
        thread.start();
    }
}