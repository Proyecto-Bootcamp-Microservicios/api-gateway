package com.bootcamp.ntt.api_gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ErrorResponse {
  private String error;
  private String message;
  private String errorCode;
  private OffsetDateTime timestamp;
}
