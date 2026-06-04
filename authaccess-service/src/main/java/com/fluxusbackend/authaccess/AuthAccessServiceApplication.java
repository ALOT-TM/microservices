package com.fluxusbackend.authaccess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients(basePackages = "com.fluxusbackend.authaccess.infrastructure.clients")
public class AuthAccessServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthAccessServiceApplication.class, args);
    }
}
