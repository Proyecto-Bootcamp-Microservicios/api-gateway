package com.bootcamp.ntt.api_gateway.filter;

import com.bootcamp.ntt.api_gateway.util.JwtTokenUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class RoleBasedAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<RoleBasedAuthGatewayFilterFactory.Config> {

  private final JwtTokenUtil jwtTokenUtil;

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      ServerHttpRequest request = exchange.getRequest();

      // El JWT ya fue validado por JwtAuthenticationFilter
      String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return handleForbidden(exchange, "Missing authentication token");
      }

      String token = authHeader.substring(7);

      try {
        String userRole = jwtTokenUtil.extractRole(token);
        String requiredRole = config.getRequiredRole();

        log.debug("User role: {}, Required role: {}", userRole, requiredRole);

        // Verificar si el usuario tiene el rol requerido
        if (!hasRequiredRole(userRole, requiredRole)) {
          log.warn("Access denied. User role '{}' does not meet required role '{}'", userRole, requiredRole);
          return handleForbidden(exchange, "Insufficient privileges");
        }

        log.debug("Role-based authorization successful for role: {}", userRole);
        return chain.filter(exchange);

      } catch (Exception e) {
        log.error("Role-based authorization failed: {}", e.getMessage());
        return handleForbidden(exchange, "Authorization failed");
      }
    };
  }

  private boolean hasRequiredRole(String userRole, String requiredRole) {
    if (userRole == null || requiredRole == null) {
      return false;
    }

    // Lógica de roles jerárquica
    switch (requiredRole.toUpperCase()) {
      case "ADMIN":
        return "ROLE_ADMIN".equals(userRole);
      case "USER":
        return "ROLE_USER".equals(userRole) || "ROLE_ADMIN".equals(userRole);
      case "EMPRESA":
        return "ROLE_EMPRESA".equals(userRole) || "ROLE_ADMIN".equals(userRole);
      default:
        return userRole.equals("ROLE_" + requiredRole.toUpperCase());
    }
  }

  private Mono<Void> handleForbidden(ServerWebExchange exchange, String message) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.FORBIDDEN);
    response.getHeaders().add("Content-Type", "application/json");

    String body = String.format(
      "{\n" +
        "  \"error\": \"Forbidden\",\n" +
        "  \"message\": \"%s\",\n" +
        "  \"timestamp\": \"%s\",\n" +
        "  \"path\": \"%s\"\n" +
        "}",
      message,
      OffsetDateTime.now().toString(),
      exchange.getRequest().getPath().value()
    );

    DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
    return response.writeWith(Mono.just(buffer));
  }

  @Override
  public Class<Config> getConfigClass() {
    return Config.class;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Config {
    private String requiredRole;
  }
}
