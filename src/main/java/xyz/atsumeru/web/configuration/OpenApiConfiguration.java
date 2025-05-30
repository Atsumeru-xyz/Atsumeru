package xyz.atsumeru.web.configuration;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Atsumeru API",
                version = "1.0",
                description = """
                    API documentation for Atsumeru self-hosted mangas/comics/light novels media server
                    
                    Implementations:
                    [Kotlin](https://github.com/Atsumeru-xyz/Atsumeru-API)
                    """,
                contact = @Contact(
                        name = "Wiki",
                        url = "https://atsumeru.xyz"
                ),
                license = @License(
                        name = "MIT",
                        url = "https://mit-license.org/"
                )
        ),
        externalDocs = @ExternalDocumentation(
                description = "Source Code",
                url = "https://github.com/Atsumeru-xyz/Atsumeru"
        )

)
public class OpenApiConfiguration {
}
