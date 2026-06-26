package com.fluxusbackend.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.fluxusbackend")
@EnableDiscoveryClient
public class NotificationserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationserviceApplication.class, args);
    }
}
