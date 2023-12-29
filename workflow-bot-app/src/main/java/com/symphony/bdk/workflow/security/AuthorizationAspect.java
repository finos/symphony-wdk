package com.symphony.bdk.workflow.security;

import com.symphony.bdk.workflow.api.v1.WorkflowsApi;
import com.symphony.bdk.workflow.api.v1.WorkflowsMgtApi;
import com.symphony.bdk.workflow.configuration.WorkflowBotConfiguration;
import com.symphony.bdk.workflow.exception.UnauthorizedException;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
@Aspect
public class AuthorizationAspect {

  private static final String UNAUTHORIZED_EXCEPTION_INVALID_TOKEN_MESSAGE = "Request is not authorised";

  private final WorkflowBotConfiguration workflowBotConfiguration;

  public AuthorizationAspect(WorkflowBotConfiguration workflowBotConfiguration) {
    this.workflowBotConfiguration = workflowBotConfiguration;
  }

  @Before("@within(org.springframework.web.bind.annotation.RequestMapping) && @annotation(authorized)")
  public void authorizationCheck(Authorized authorized) {
    String headerKey = authorized.headerTokenKey();
    HttpServletRequest httpServletRequest = getHttpServletRequest();

    if (WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY.equals(headerKey)) {
      String managementToken = workflowBotConfiguration.getManagementToken();
      validateToken(httpServletRequest.getHeader(headerKey), managementToken);
    } else if (WorkflowsApi.X_MONITORING_TOKEN_KEY.equals(headerKey)) {
      String monitoringToken = workflowBotConfiguration.getMonitoringToken();
      validateToken(httpServletRequest.getHeader(headerKey), monitoringToken);
    } else {
      throw new UnauthorizedException(UNAUTHORIZED_EXCEPTION_INVALID_TOKEN_MESSAGE);
    }
  }

  private static void validateToken(String receivedToken, String managementToken) {
    if (StringUtils.isBlank(managementToken) || !managementToken.equals(receivedToken)) {
      throw new UnauthorizedException(UNAUTHORIZED_EXCEPTION_INVALID_TOKEN_MESSAGE);
    }
  }

  private HttpServletRequest getHttpServletRequest() {
    return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
  }
}
