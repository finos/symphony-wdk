package com.symphony.devsol.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WdkFilter extends OncePerRequestFilter {
  @Value("${wdk.properties.monitoring-token}")
  private String monitoringToken;
  @Value("${wdk.properties.management-token}")
  private String managementToken;

  private HttpServletRequestWrapper wdkTokenWrapper(HttpServletRequest request) {
    return new HttpServletRequestWrapper(request) {
      @Override
      public String getHeader(String name) {
        if (name.equals("X-Monitoring-Token")) {
          return monitoringToken;
        }
        if (name.equals("X-Management-Token")) {
          return managementToken;
        }
        return super.getHeader(name);
      }
    };
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain
  ) throws ServletException, IOException {
    chain.doFilter(wdkTokenWrapper(request), response);
  }
}
