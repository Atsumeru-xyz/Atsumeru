package xyz.atsumeru.web.configuration;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
@ComponentScan
public class MultiPartConfiguration {
    private static final DataSize DATA_SIZE_256_MB = DataSize.ofMegabytes(256);

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DATA_SIZE_256_MB);
        factory.setMaxRequestSize(DATA_SIZE_256_MB);
        return factory.createMultipartConfig();
    }
}
