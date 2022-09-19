package com.symphony.bdk.workflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.fail;

import com.symphony.bdk.workflow.exception.ApiDisabledException;
import com.symphony.bdk.workflow.exception.UnauthorizedException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;

class AuthorizationAspectTest {

  private static final String UNAUTHORIZED_EXCEPTION_MESSAGE = "Request token is not valid";
  private static final String API_DISABLED_EXCEPTION_MESSAGE = "The endpoint %s is disabled and cannot be called";
  private static final String X_MONITORING_TOKEN_HEADER_KEY = "X-Monitoring-Token";

  @Test
  void authorization_noMonitoringTokenTest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/mock/path");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect(null);
    Authorized authorizedInstance = createAuthorizedInstance();

    assertThatExceptionOfType(ApiDisabledException.class)
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(
            e -> assertThat(e.getMessage()).isEqualTo(String.format(API_DISABLED_EXCEPTION_MESSAGE, "/mock/path")));
  }

  @Test
  void authorization_noHeaderTokenTest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect("MONITORING_TOKEN_VALUE");
    Authorized authorizedInstance = createAuthorizedInstance();

    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_MESSAGE));
  }

  @Test
  void authorization_headerToken_notMatchingTest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(X_MONITORING_TOKEN_HEADER_KEY, "BAD_MONITORING_TOKEN_VALUE");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect("MONITORING_TOKEN_VALUE");
    Authorized authorizedInstance = createAuthorizedInstance();

    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_MESSAGE));
  }

  @Test
  void authorization_headerToken_matchingTest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(X_MONITORING_TOKEN_HEADER_KEY, "MONITORING_TOKEN_VALUE");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect("MONITORING_TOKEN_VALUE");
    Authorized authorizedInstance = createAuthorizedInstance();

    try {
      authorizationAspect.authorizationCheck(authorizedInstance);
    } catch (Throwable e) {
      fail("No throwable should have been caught");
    }
  }

  private static Authorized createAuthorizedInstance() {
    return new Authorized() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return null;
      }

      @Override
      public String headerTokenKey() {
        return X_MONITORING_TOKEN_HEADER_KEY;
      }
    };
  }
}
