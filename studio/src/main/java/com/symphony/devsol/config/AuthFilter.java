package com.symphony.devsol.config;

import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import com.symphony.bdk.core.auth.jwt.UserClaim;
import com.symphony.devsol.client.ExtAppClient;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {
  private final ExtAppClient extAppClient;
  private final Pattern execPattern = Pattern.compile("/v1/workflows/[\\w\\-]+/execute");
  @Value("${wdk.properties.management-token}")
  private String managementToken;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain
  ) throws ServletException, IOException {
    if (!request.getMethod().equals("OPTIONS")) {
      String authHeader = request.getHeader("Authorization");
      String managementTokenHeader = request.getHeader("X-Management-Token");
      UserClaim userClaim = new UserClaim();

      if (managementToken.equals(managementTokenHeader)) {
        userClaim.setUsername("mgmt-token");
      } else if (execPattern.matcher(request.getRequestURI()).matches()) {
        userClaim.setUsername("webhook");
      } else {
        try {
          String jwt = authHeader.substring(7);
          userClaim = extAppClient.validate(jwt);
        } catch (Exception e) {
          response.sendError(SC_UNAUTHORIZED, "Invalid Credentials");
          return;
        }
      }
      if (userClaim.getUsername() != null) {
        request.setAttribute("user", userClaim);
      }
    }
    chain.doFilter(request, response);
  }
}
