package com.bootcamp.ntt.api_gateway.filter;

import com.bootcamp.ntt.api_gateway.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

  private final JwtTokenUtil jwtTokenUtil;

  @Override
  public GatewayFilter apply(Object config) {
    return (exchange, chain) -> {
      ServerHttpRequest request = exchange.getRequest();

      // Extraer el token del header Authorization
      String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        log.warn("Missing or invalid Authorization header");
        return handleUnauthorized(exchange);
      }

      String token = authHeader.substring(7);

      try {
        // Validar el token
        if (!jwtTokenUtil.validateToken(token)) {
          log.warn("Invalid JWT token");
          return handleUnauthorized(exchange);
        }

        // Extraer claims y agregar headers para los microservicios
        String username = jwtTokenUtil.extractUsername(token);
        String userId = jwtTokenUtil.extractUserId(token);
        String customerId = jwtTokenUtil.extractCustomerId(token);
        String role = jwtTokenUtil.extractRole(token);
        String email = jwtTokenUtil.extractEmail(token);

        // Crear request mutado con headers adicionales
        ServerHttpRequest mutatedRequest = request.mutate()
          .header("X-User-Username", username)
          .header("X-User-Id", userId)
          .header("X-Customer-Id", customerId)
          .header("X-User-Role", role)
          .header("X-User-Email", email)
          .header("X-Auth-Token", token)
          .build();

        log.debug("JWT authentication successful for user: {}", username);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());

      } catch (Exception e) {
        log.error("JWT authentication failed: {}", e.getMessage());
        return handleUnauthorized(exchange);
      }
    };
  }

  private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().add("Content-Type", "application/json");

    String body = String.format(
      "{\n" +
        "  \"error\": \"Unauthorized\",\n" +
        "  \"message\": \"Invalid or missing authentication token\",\n" +
        "  \"timestamp\": \"%s\",\n" +
        "  \"path\": \"%s\"\n" +
        "}",
      OffsetDateTime.now(),
      exchange.getRequest().getPath().value()
    );

    DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
    return response.writeWith(Mono.just(buffer));
  }
}
