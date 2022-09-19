package com.symphony.bdk.workflow.security;

import com.symphony.bdk.workflow.exception.UnauthorizedException;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Component
@Aspect
public class AuthorizationAspect {

  private final String monitoringToken;

  private static final String UNAUTHORIZED_EXCEPTION_MESSAGE = "Request token is not valid";

  public AuthorizationAspect(@Value("${wdk.properties.monitoring-token}") String monitoringToken) {
    this.monitoringToken = monitoringToken;
  }

  @Before("@within(org.springframework.web.bind.annotation.RequestMapping) && @annotation(authorized)")
  public void authorizationCheck(Authorized authorized) {
    HttpServletRequest httpServletRequest = getHttpServletRequest();

    if (monitoringToken == null || monitoringToken.isEmpty()) {
      throw new UnauthorizedException(
          String.format("The endpoint %s is disabled and cannot be called", httpServletRequest.getRequestURI()));
    }

    String headerKey = authorized.headerTokenKey();

    if (headerKey == null || !monitoringToken.equals(
        httpServletRequest.getHeader(headerKey))) {
      throw new UnauthorizedException(UNAUTHORIZED_EXCEPTION_MESSAGE);
    }
  }

  private HttpServletRequest getHttpServletRequest() {
    return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
  }
}
