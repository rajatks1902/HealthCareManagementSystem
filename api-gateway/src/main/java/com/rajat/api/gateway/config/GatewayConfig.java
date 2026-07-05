package com.rajat.api.gateway.config;

import com.rajat.api.gateway.filter.AuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

@Configuration // Marks this class as a configuration class for Spring
public class GatewayConfig {

    private static final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);

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
        logger.info("Configuring API Gateway routes");

        return builder.routes()

                // Route configuration for doctor-service
                .route("doctor-info", r -> r.path("/api/v1/doctor/**")
                        .filters(f -> securedFilters(f, "doctorCircuitBreaker", "forward:/fallback/doctor"))
                        .uri("http://doctor-info:8080")) // Correct base URI for doctor-service

                .route("patient-info", r -> r.path("/api/v1/patient/**")
                        .filters(f -> securedFilters(f, "patientCircuitBreaker", "forward:/fallback/patient"))
                        .uri("http://patient-info:8080")) // Correct base URI for doctor-service


                // Route configuration for appointment-service
                .route("appointment-services", r -> r.path("/api/v1/appointments/**")
                        .filters(f -> securedFilters(f, "appointmentServiceCircuitBreaker", "forward:/fallback/appointment"))
                        .uri("http://appointment-services:8080")) // Correct base URI for appointment-service

                // Route configuration for auth-service
                .route("auth-security", r -> r.path("/api/auth/**")
                        .filters(f -> securedFilters(f, "authCircuitBreaker", "forward:/fallback/auth"))
                        .uri("http://auth-security:8080")) // Correct base URI for auth-service

                .build();
    }

    private GatewayFilterSpec securedFilters(GatewayFilterSpec filters, String circuitBreakerName, String fallbackUri) {
        return filters
                .filter(filter)
                .retry(retry -> retry
                        .setRetries(2)
                        .setMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
                        .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT))
                .circuitBreaker(c -> c.setName(circuitBreakerName).setFallbackUri(fallbackUri))
                .filter(new RemoveDuplicateHeadersFilter())
                .filter((exchange, chain) -> {
                    if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                        exchange.getResponse().setStatusCode(HttpStatus.OK);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }
}
