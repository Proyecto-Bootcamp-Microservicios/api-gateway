package com.bootcamp.ntt.api_gateway.config;

import com.bootcamp.ntt.api_gateway.filter.JwtAuthenticationGatewayFilterFactory;
import com.bootcamp.ntt.api_gateway.filter.RoleBasedAuthGatewayFilterFactory;
import com.bootcamp.ntt.api_gateway.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * Configuración para registrar automáticamente los filtros personalizados
 */
@Configuration
@Slf4j
public class FilterConfiguration {

  /**
   * Bean para hacer disponible el filtro JWT en el YAML
   */
  @Bean
  public JwtAuthenticationGatewayFilterFactory jwtAuthenticationGatewayFilter(JwtTokenUtil jwtTokenUtil) {
    return new JwtAuthenticationGatewayFilterFactory(jwtTokenUtil);
  }

  /**
   * Bean para hacer disponible el filtro de roles en el YAML
   */
  @Bean
  public RoleBasedAuthGatewayFilterFactory roleBasedAuthGatewayFilter(JwtTokenUtil jwtTokenUtil) {
    return new RoleBasedAuthGatewayFilterFactory(jwtTokenUtil);
  }
}
