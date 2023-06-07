package com.symphony.devsol.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class FilterConfig {
  @Bean
  public FilterRegistrationBean<OncePerRequestFilter> authFilterRegistration(AuthFilter filter) {
    FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns("/v1/*", "/gallery/*", "/symphony/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<OncePerRequestFilter> accessFilterRegistration(AccessFilter filter) {
    FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns("/v1/workflows", "/v1/workflows/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<OncePerRequestFilter> wdkFilterRegistration(WdkFilter filter) {
    FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns("/v1/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
    return registration;
  }
}
