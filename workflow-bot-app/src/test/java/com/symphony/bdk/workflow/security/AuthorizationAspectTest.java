package com.symphony.bdk.workflow.security;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.configuration.WorkflowBotConfiguration;
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
  private static final String MONITORING_TOKEN_VALUE = "MONITORING_TOKEN_VALUE";

  @Test
  void authorization_noMonitoringTokenTest() {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn(null);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/mock/path");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance();

    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(
            e -> assertThat(e.getMessage()).isEqualTo(String.format(API_DISABLED_EXCEPTION_MESSAGE, "/mock/path")));
  }

  @Test
  void authorization_noHeaderTokenTest() {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn(MONITORING_TOKEN_VALUE);

    MockHttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance();

    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_MESSAGE));
  }

  @Test
  void authorization_headerToken_notMatchingTest() {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn(MONITORING_TOKEN_VALUE);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(X_MONITORING_TOKEN_HEADER_KEY, "BAD_MONITORING_TOKEN_VALUE");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance();

    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_MESSAGE));
  }

  @Test
  void authorization_headerToken_matchingTest() {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn(MONITORING_TOKEN_VALUE);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(X_MONITORING_TOKEN_HEADER_KEY, MONITORING_TOKEN_VALUE);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
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
