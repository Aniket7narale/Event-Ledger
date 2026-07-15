package com.example.eventgateway;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class EventGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventGatewayApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public OpenAPI eventGatewayOpenAPI() {
        return new OpenAPI().info(new Info().title("Event Gateway API").version("1.0").description("Gateway endpoints for submitting and retrieving financial events"));
    }
}
