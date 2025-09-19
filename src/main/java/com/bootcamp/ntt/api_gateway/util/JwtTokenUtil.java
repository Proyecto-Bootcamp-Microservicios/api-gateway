package com.bootcamp.ntt.api_gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenUtil {

  @Value("${JWT_SECRET:mySuperSecretKeyThatIsAtLeast512BitsLongForHS512AlgorithmAndMeetsSecurityRequirements123456789ABC}")
  private String secret;

  @Value("${JWT_EXPIRATION:864000000}")
  private Long expiration;

  private SecretKey getSigningKey() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token);
      return true;
    } catch (ExpiredJwtException e) {
      log.error("Token expired: {}", e.getMessage());
      return false;
    } catch (UnsupportedJwtException e) {
      log.error("Unsupported JWT token: {}", e.getMessage());
      return false;
    } catch (MalformedJwtException e) {
      log.error("Malformed JWT token: {}", e.getMessage());
      return false;
    } catch (SignatureException e) {
      log.error("Invalid JWT signature: {}", e.getMessage());
      return false;
    } catch (IllegalArgumentException e) {
      log.error("JWT token compact of handler are invalid: {}", e.getMessage());
      return false;
    }
  }

  public Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
      .setSigningKey(getSigningKey())
      .build()
      .parseClaimsJws(token)
      .getBody();
  }

  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  public String extractCustomerId(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }
    return extractAllClaims(token).get("customerId", String.class);
  }

  public String extractUserId(String token) {
    return extractAllClaims(token).get("userId", String.class);
  }

  public String extractRole(String token) {
    return extractAllClaims(token).get("role", String.class);
  }

  public String extractEmail(String token) {
    return extractAllClaims(token).get("email", String.class);
  }

  public boolean isTokenExpired(String token) {
    return extractAllClaims(token).getExpiration().before(new Date());
  }

  public Date extractExpiration(String token) {
    return extractAllClaims(token).getExpiration();
  }
}
