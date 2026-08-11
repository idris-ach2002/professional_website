package sorbonne.professional_website.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI portfolioOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Professional Portfolio API")
                .version("22.0")
                .description("Contract for the public portfolio and authenticated administration APIs."));
    }
}
