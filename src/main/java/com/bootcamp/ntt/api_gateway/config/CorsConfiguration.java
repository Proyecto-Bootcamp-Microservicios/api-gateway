package com.bootcamp.ntt.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
@Slf4j
public class CorsConfiguration {

  @Value("${cors.allowed-origins:*}")
  private String allowedOrigins;

  @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
  private String allowedMethods;

  @Value("${cors.allowed-headers:*}")
  private String allowedHeaders;

  @Value("${cors.allow-credentials:true}")
  private boolean allowCredentials;

  @Bean
  public CorsWebFilter corsFilter() {
    log.info("Configuring CORS with origins: {}", allowedOrigins);

    org.springframework.web.cors.CorsConfiguration corsConfig = new org.springframework.web.cors.CorsConfiguration();

    // Configurar orígenes permitidos
    if ("*".equals(allowedOrigins)) {
      corsConfig.addAllowedOriginPattern("*");
    } else {
      Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .forEach(corsConfig::addAllowedOrigin);
    }

    // Configurar métodos permitidos
    Arrays.stream(allowedMethods.split(","))
      .map(String::trim)
      .forEach(corsConfig::addAllowedMethod);

    // Configurar headers permitidos
    if ("*".equals(allowedHeaders)) {
      corsConfig.addAllowedHeader("*");
    } else {
      Arrays.stream(allowedHeaders.split(","))
        .map(String::trim)
        .forEach(corsConfig::addAllowedHeader);
    }

    corsConfig.setAllowCredentials(allowCredentials);
    corsConfig.setMaxAge(3600L); // 1 hora

    UrlBasedCorsConfigurationSource source =
      new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);

    return new CorsWebFilter(source);
  }

  @Bean
  public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
    return http
      .csrf(csrf -> csrf.disable())
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .authorizeExchange(exchanges -> exchanges
        .pathMatchers("/actuator/**", "/fallback/**").permitAll()
        .anyExchange().authenticated()
      )
      .build();
  }

  @Bean
  public org.springframework.web.cors.reactive.CorsConfigurationSource corsConfigurationSource() {
    org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

    if ("*".equals(allowedOrigins)) {
      configuration.addAllowedOriginPattern("*");
    } else {
      Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .forEach(configuration::addAllowedOrigin);
    }

    Arrays.stream(allowedMethods.split(","))
      .map(String::trim)
      .forEach(configuration::addAllowedMethod);

    if ("*".equals(allowedHeaders)) {
      configuration.addAllowedHeader("*");
    } else {
      Arrays.stream(allowedHeaders.split(","))
        .map(String::trim)
        .forEach(configuration::addAllowedHeader);
    }

    configuration.setAllowCredentials(allowCredentials);

    org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource source =
      new org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}
