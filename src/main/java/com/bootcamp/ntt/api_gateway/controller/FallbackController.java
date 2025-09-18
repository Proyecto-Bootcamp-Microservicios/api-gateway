package com.bootcamp.ntt.api_gateway.controller;
import com.bootcamp.ntt.api_gateway.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

  @GetMapping("/customer")
  public Mono<ResponseEntity<ErrorResponse>> customerFallback(ServerWebExchange exchange) {
    log.warn("Customer service fallback triggered for path: {}", exchange.getRequest().getPath());
    return createFallbackResponse("Customer service is temporarily unavailable", "CUSTOMER_SERVICE_DOWN");
  }

  @GetMapping("/account")
  public Mono<ResponseEntity<ErrorResponse>> accountFallback(ServerWebExchange exchange) {
    log.warn("Account service fallback triggered for path: {}", exchange.getRequest().getPath());
    return createFallbackResponse("Account service is temporarily unavailable", "ACCOUNT_SERVICE_DOWN");
  }

  @GetMapping("/card")
  public Mono<ResponseEntity<ErrorResponse>> cardFallback(ServerWebExchange exchange) {
    log.warn("Card service fallback triggered for path: {}", exchange.getRequest().getPath());
    return createFallbackResponse("Card service is temporarily unavailable", "CARD_SERVICE_DOWN");
  }

  @GetMapping("/credit")
  public Mono<ResponseEntity<ErrorResponse>> creditFallback(ServerWebExchange exchange) {
    log.warn("Credit service fallback triggered for path: {}", exchange.getRequest().getPath());
    return createFallbackResponse("Credit service is temporarily unavailable", "CREDIT_SERVICE_DOWN");
  }

  @GetMapping("/transaction")
  public Mono<ResponseEntity<ErrorResponse>> transactionFallback(ServerWebExchange exchange) {
    log.warn("Transaction service fallback triggered for path: {}", exchange.getRequest().getPath());
    return createFallbackResponse("Transaction service is temporarily unavailable", "TRANSACTION_SERVICE_DOWN");
  }

  @GetMapping("/reports")
  public Mono<ResponseEntity<ErrorResponse>> reportsFallback(ServerWebExchange exchange) {
    log.warn("Reports service fallback triggered for path: {}", exchange.getRequest().getPath());
    return createFallbackResponse("Reports service is temporarily unavailable", "REPORTS_SERVICE_DOWN");
  }

  private Mono<ResponseEntity<ErrorResponse>> createFallbackResponse(String message, String errorCode) {
    ErrorResponse errorResponse = ErrorResponse.builder()
      .error("Service Unavailable")
      .message(message)
      .errorCode(errorCode)
      .timestamp(OffsetDateTime.now())
      .build();

    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
  }

}
