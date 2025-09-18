package com.bootcamp.ntt.api_gateway.config;

import io.netty.handler.timeout.TimeoutException;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.core.io.buffer.DataBuffer;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;

import io.netty.channel.ConnectTimeoutException;

@Configuration
@Slf4j
public class GatewayConfig {

  @Value("${rate-limit.replenish-rate:10}")
  private int replenishRate;

  @Value("${rate-limit.burst-capacity:20}")
  private int burstCapacity;

  /**
   * Rate Limiter basado en Redis
   */
  @Bean
  public RedisRateLimiter redisRateLimiter() {
    return new RedisRateLimiter(replenishRate, burstCapacity, 1);
  }

  /**
   * Key Resolver basado en IP para Rate Limiting
   */
  @Bean
  @Primary
  public KeyResolver ipKeyResolver() {
    return exchange -> {
      String clientIp = getClientIP(exchange.getRequest());
      log.debug("Rate limiting key: {}", clientIp);
      return Mono.just(clientIp);
    };
  }

  /**
   * Key Resolver basado en usuario autenticado
   */
  @Bean
  public KeyResolver userKeyResolver() {
    return exchange -> {
      String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        try {
          // Extraer username del token si está presente
          String token = authHeader.substring(7);
          // Aquí podrías usar JwtTokenUtil para extraer el username
          return Mono.just("user-" + token.hashCode());
        } catch (Exception e) {
          log.warn("Failed to extract user from token, falling back to IP");
        }
      }
      return Mono.just(getClientIP(exchange.getRequest()));
    };
  }

  /**
   * Configuración de Route Locator para rutas programáticas (opcional)
   */
  @Bean
  public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
      // Puedes agregar rutas programáticas aquí si necesitas lógica compleja
      .route("health-check", r -> r.path("/health")
        .uri("http://localhost:8080/actuator/health"))
      .build();
  }

  /**
   * Global Filter para logging de requests
   */
  @Bean
  public GlobalFilter loggingGlobalFilter() {
    return (exchange, chain) -> {
      org.springframework.http.server.reactive.ServerHttpRequest request = exchange.getRequest();
      String path = request.getPath().value();
      String method = request.getMethod().name();
      String clientIp = getClientIP(request);

      log.info("Gateway Request: {} {} from IP: {}", method, path, clientIp);

      return chain.filter(exchange)
        .doOnSuccess(aVoid -> log.debug("Gateway Response: {} {} completed successfully", method, path))
        .doOnError(throwable -> log.error("Gateway Response: {} {} failed with error: {}",
          method, path, throwable.getMessage()));
    };
  }

  /**
   * Global Filter para agregar headers de respuesta
   */
  @Bean
  public GlobalFilter responseHeadersFilter() {
    return (exchange, chain) -> {
      return chain.filter(exchange).then(Mono.fromRunnable(() -> {
        exchange.getResponse().getHeaders().add("X-Gateway-Response", "API-Gateway");
        exchange.getResponse().getHeaders().add("X-Response-Time", String.valueOf(System.currentTimeMillis()));
      }));
    };
  }

  /**
   * Configuración para WebClient con timeout personalizado
   */
  @Bean
  public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
      .responseTimeout(Duration.ofSeconds(5))
      .doOnConnected(conn ->
        conn.addHandlerLast(new ReadTimeoutHandler(5))
          .addHandlerLast(new WriteTimeoutHandler(5)));

    return WebClient.builder()
      .clientConnector(new ReactorClientHttpConnector(httpClient))
      .build();
  }

  /**
   * Extrae la IP real del cliente considerando proxies y load balancers
   */
  private String getClientIP(org.springframework.http.server.reactive.ServerHttpRequest request) {
    String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
    String xRealIp = request.getHeaders().getFirst("X-Real-IP");
    String xClientIp = request.getHeaders().getFirst("X-Client-IP");

    if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
      // X-Forwarded-For puede contener múltiples IPs, tomamos la primera
      return xForwardedFor.split(",")[0].trim();
    }

    if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
      return xRealIp;
    }

    if (xClientIp != null && !xClientIp.isEmpty() && !"unknown".equalsIgnoreCase(xClientIp)) {
      return xClientIp;
    }

    // Fallback a la IP remota del request
    return request.getRemoteAddress() != null ?
      request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
  }

  /**
   * Bean para manejar excepciones globales en el gateway
   */
  @Bean
  @Order(-1)
  public WebExceptionHandler globalExceptionHandler() {
    return (exchange, throwable) -> {
      ServerHttpResponse response = exchange.getResponse();

      if (throwable instanceof ConnectTimeoutException) {
        response.setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
        log.error("Gateway timeout: {}", throwable.getMessage());
      } else if (throwable instanceof TimeoutException) {
        response.setStatusCode(HttpStatus.REQUEST_TIMEOUT);
        log.error("Request timeout: {}", throwable.getMessage());
      } else if (throwable instanceof ConnectException) {
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        log.error("Service unavailable: {}", throwable.getMessage());
      } else {
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        log.error("Gateway error: {}", throwable.getMessage(), throwable);
      }

      response.getHeaders().add("Content-Type", "application/json");

      String errorBody = String.format(
        "{\n" +
          "  \"error\": \"%s\",\n" +
          "  \"message\": \"%s\",\n" +
          "  \"timestamp\": \"%s\",\n" +
          "  \"path\": \"%s\"\n" +
          "}",
        response.getStatusCode().getReasonPhrase(),
        throwable.getMessage(),
        OffsetDateTime.now().toString(),
        exchange.getRequest().getPath().value()
      );

      DataBuffer buffer = response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8));
      return response.writeWith(Mono.just(buffer));
    };
  }
}
