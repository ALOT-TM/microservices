package com.fluxusbackend.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

import java.util.Arrays;
import java.util.Collections;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> customRouteLocator() {
        return route("authaccess-service").route(path("/api/auth/**"), http()).filter(lb("authaccess-service")).build()
            .and(route("companies-service").route(path("/api/retail-companies/**", "/api/addresses/**", "/api/countries/**", "/api/retail-company-headquarters/**", "/api/retail/dashboard/**"), http()).filter(lb("companies-service")).build())
            .and(route("beneficiaries-service").route(path("/api/beneficiary-institutions/**", "/api/beneficiary-institution-headquarters/**", "/api/institution-types/**", "/api/beneficiary-addresses/**", "/api/beneficiary-countries/**"), http()).filter(lb("beneficiaries-service")).build())
            .and(route("shrinkage-service").route(path("/api/shrinkages/**", "/api/reports/**", "/api/audit/**"), http()).filter(lb("shrinkage-service")).build())
            .and(route("donations-logistics-service").route(path("/api/donations/**", "/api/requests/**"), http()).filter(lb("donations-logistics-service")).build())
            .and(route("subscription-service").route(path("/api/plans/**", "/api/subscriptions/**"), http()).filter(lb("subscription-service")).build());
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Collections.singletonList("http://localhost:5173"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
