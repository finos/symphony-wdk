package com.symphony.bdk.workflow.monitoring.service;

import com.symphony.bdk.workflow.engine.UnauthorizedException;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Component
@Aspect
public class PermissionAspect {

  private final String monitoringToken;

  public PermissionAspect(@Value("${wdk.properties.monitoring-token}") String monitoringToken) {
    this.monitoringToken = monitoringToken;
  }

  @Before("@within(org.springframework.web.bind.annotation.RequestMapping) && @annotation(authorized)")
  public void permissionCheck(Authorized authorized) {
    HttpServletRequest httpServletRequest = getHttpServletRequest();
    String headerKey = authorized.headerTokenKey();

    if (monitoringToken == null) {
      throw new UnauthorizedException("Monitoring token environment variable should be set.");
    }

    if (headerKey == null || !monitoringToken.equals(httpServletRequest.getHeader(headerKey))) {
      throw new UnauthorizedException("Incorrect token");
    }
  }

  private HttpServletRequest getHttpServletRequest() {
    return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
  }
}
