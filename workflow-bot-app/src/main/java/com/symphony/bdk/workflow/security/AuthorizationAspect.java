package com.symphony.bdk.workflow.security;

import com.symphony.bdk.workflow.configuration.WorkflowBotConfiguration;
import com.symphony.bdk.workflow.exception.UnauthorizedException;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Component
@Aspect
public class AuthorizationAspect {

  private static final String UNAUTHORIZED_EXCEPTION_INVALID_TOKEN_MESSAGE = "Request token is not valid";
  private static final String UNAUTHORIZED_EXCEPTION_MISSING_TOKEN_MESSAGE = "Request header %s is missing";

  private final WorkflowBotConfiguration workflowBotConfiguration;

  public AuthorizationAspect(WorkflowBotConfiguration workflowBotConfiguration) {
    this.workflowBotConfiguration = workflowBotConfiguration;
  }

  @Before("@within(org.springframework.web.bind.annotation.RequestMapping) && @annotation(authorized)")
  public void authorizationCheck(Authorized authorized) {
    String monitoringToken = workflowBotConfiguration.getMonitoringToken();
    String headerKey = authorized.headerTokenKey();
    HttpServletRequest httpServletRequest = getHttpServletRequest();

    if (headerKey != null && httpServletRequest.getHeader(headerKey) == null) {
      throw new UnauthorizedException(String.format(UNAUTHORIZED_EXCEPTION_MISSING_TOKEN_MESSAGE, headerKey));
    }

    if (headerKey == null || monitoringToken == null || monitoringToken.isEmpty() || !monitoringToken.equals(
        httpServletRequest.getHeader(headerKey))) {
      throw new UnauthorizedException(UNAUTHORIZED_EXCEPTION_INVALID_TOKEN_MESSAGE);
    }
  }

  private HttpServletRequest getHttpServletRequest() {
    return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
  }
}
