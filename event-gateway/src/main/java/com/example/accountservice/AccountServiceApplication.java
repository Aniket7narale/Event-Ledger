package com.example.accountservice;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AccountServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    @Bean
    public OpenAPI accountServiceOpenAPI() {
        return new OpenAPI().info(new Info().title("Account Service API").version("1.0").description("Operations for managing account balances and transactions"));
    }
}
