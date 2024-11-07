package com.github.mortenpa.turtle.configuration;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.List;

@Configuration
public class SwaggerDocConfig {

    @Value("${turtleman.dev-url}")
    private String devURL;

    @Value("${turtleman.prod-url}")
    private String prodURL;

    @Bean
    public OpenAPI turtleManOpenApi() {

        String title = "TurtleMan Customer Management API";
        String version = "1.0";
        String description = "This API provides endpoints to manage customers";

        Server devServer = new Server();
        devServer.setUrl(devURL);
        devServer.setDescription("Development Server");

        Server prodServer = new Server();
        prodServer.setUrl(prodURL);
        prodServer.setDescription("Production Server");

        return new OpenAPI().info(
                new Info().
                title(title).
                version(version).
                description(description)
        ).servers(List.of(devServer, prodServer));

    }

}
