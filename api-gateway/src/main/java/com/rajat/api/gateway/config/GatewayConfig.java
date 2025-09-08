package com.rajat.api.gateway.config;

import com.rajat.api.gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

@Configuration // Marks this class as a configuration class for Spring
public class GatewayConfig {

    // Injects the custom authentication filter for validating requests
    private final AuthenticationFilter filter;

    // Explicit constructor for dependency injection
    public GatewayConfig(AuthenticationFilter filter) {
        this.filter = filter;
    }

    /**
     * Configures routes for the API Gateway.
     * Defines how incoming requests should be routed to specific microservices.
     *
     * @param builder RouteLocatorBuilder to define routes and attach filters.
     * @return a RouteLocator with defined routes.
     */
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        System.out.println("inside routes");

        return builder.routes()

                // Route configuration for doctor-service
                .route("doctor-info", r -> r.path("/api/v1/doctor/**")
                        .filters(f -> f
                                .filter(filter)
                                .circuitBreaker(c -> c.setName("doctorCircuitBreaker").setFallbackUri("forward:/fallback/doctor"))
                                .filter(new RemoveDuplicateHeadersFilter()) // Optional: Remove duplicate headers
                                .filter((exchange, chain) -> {
                                    // Handle preflight OPTIONS requests
                                    if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                                        exchange.getResponse().setStatusCode(HttpStatus.OK);
                                        return exchange.getResponse().setComplete();
                                    }
                                    return chain.filter(exchange);
                                })
                        )
                        .uri("http://doctor-info:8080")) // Correct base URI for doctor-service

                .route("patient-info", r -> r.path("/api/v1/patient/**")
                        .filters(f -> f
                                .filter(filter)
                                .circuitBreaker(c -> c.setName("doctorCircuitBreaker").setFallbackUri("forward:/fallback/doctor"))
                                .filter(new RemoveDuplicateHeadersFilter()) // Optional: Remove duplicate headers
                                .filter((exchange, chain) -> {
                                    // Handle preflight OPTIONS requests
                                    if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                                        exchange.getResponse().setStatusCode(HttpStatus.OK);
                                        return exchange.getResponse().setComplete();
                                    }
                                    return chain.filter(exchange);
                                })
                        )
                        .uri("http://patient-info:8080")) // Correct base URI for doctor-service


                // Route configuration for appointment-service
                .route("appointment-service", r -> r.path("/api/v1/appointments/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c.setName("appointmentServiceCircuitBreaker").setFallbackUri("forward:/fallback/appointment"))
                                .filter(new RemoveDuplicateHeadersFilter()) // Optional: Remove duplicate headers
                                .filter((exchange, chain) -> {
                                    // Handle preflight OPTIONS requests
                                    if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                                        exchange.getResponse().setStatusCode(HttpStatus.OK);
                                        return exchange.getResponse().setComplete();
                                    }
                                    return chain.filter(exchange);
                                })
                        )
                        .uri("http://appointment-service:8080")) // Correct base URI for appointment-service

                // Route configuration for auth-service
                .route("auth-security", r -> r.path("/api/auth/**")
                        .filters(f -> f
                                .filter(filter)
                                .circuitBreaker(c -> c.setName("authCircuitBreaker").setFallbackUri("forward:/fallback/auth"))
                                .filter(new RemoveDuplicateHeadersFilter()) // Optional: Remove duplicate headers
                                .filter((exchange, chain) -> {
                                    // Handle preflight OPTIONS requests
                                    if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                                        exchange.getResponse().setStatusCode(HttpStatus.OK);
                                        return exchange.getResponse().setComplete();
                                    }
                                    return chain.filter(exchange);
                                })
                        )
                        .uri("http://auth-security:8080")) // Correct base URI for auth-service

                .build();
    }
}
