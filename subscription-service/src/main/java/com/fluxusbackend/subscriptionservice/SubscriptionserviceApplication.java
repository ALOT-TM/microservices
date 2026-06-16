package com.fluxusbackend.subscriptionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.fluxusbackend")
@EnableJpaAuditing
@EntityScan(basePackages = "com.fluxusbackend")
@EnableJpaRepositories(basePackages = "com.fluxusbackend")
@EnableFeignClients(basePackages = "com.fluxusbackend")
public class SubscriptionserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubscriptionserviceApplication.class, args);
    }
}
