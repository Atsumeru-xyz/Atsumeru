package xyz.atsumeru.web;

import lombok.Getter;
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
import org.springframework.core.env.Environment;
import xyz.atsumeru.web.util.StringUtils;

import java.util.Arrays;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class, JacksonAutoConfiguration.class })
public class AtsumeruApplication implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(AtsumeruApplication.class.getSimpleName());

    @Getter
    private static ConfigurableApplicationContext context;

    @Getter
    private static boolean isInDevMode;

    public static void main(String[] args) {
        context = SpringApplication.run(AtsumeruApplication.class, args);
    }

    public AtsumeruApplication(Environment environment) {
        isInDevMode = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> StringUtils.equalsIgnoreCase(profile, "dev"));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doAfterStart() {
        // Do nothing
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