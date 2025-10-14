package com.ruipeng.cloudstorage.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cloud Storage API Documentation")
                        .version("1.0")
                        .description("This is an API documentation of a cloud storage web application")
                        .contact(new Contact()
                                .name("Rui Peng")
                                .email("rui.peng.contact@gmail.com")));
    }
}
