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

  private static final String UNAUTHORIZED_EXCEPTION_BAD_TOKEN_MESSAGE = "Request is not authorised";
  private static final String X_MONITORING_TOKEN_HEADER_KEY = "X-Monitoring-Token";
  private static final String MONITORING_TOKEN_VALUE = "MONITORING_TOKEN_VALUE";

  @Test
  void test_authorization_monitoringTokenNotConfigured_headerSet() {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn(null);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(X_MONITORING_TOKEN_HEADER_KEY, "BAD_MONITORING_TOKEN_VALUE");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance();

    assertThatExceptionOfType(UnauthorizedException.class)
        .as("No token will match when the monitoring token is not configured")
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(
            e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_BAD_TOKEN_MESSAGE));
  }

  @Test
  void test_authorization_monitoringTokenNotConfigured_headerSetWithEmptyString() {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn("");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(X_MONITORING_TOKEN_HEADER_KEY, "");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance();

    assertThatExceptionOfType(UnauthorizedException.class)
        .as("No token will match when the monitoring token is not configured, including empty string")
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(
            e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_BAD_TOKEN_MESSAGE));
  }

  @Test
  void test_authorization_monitoringTokenConfigured_headerTokenNotSet() {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn(MONITORING_TOKEN_VALUE);

    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance();

    assertThatExceptionOfType(UnauthorizedException.class)
        .as("The header is not set in the request")
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_BAD_TOKEN_MESSAGE));
  }

  @Test
  void tst_authorization_headerToken_notMatching() {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn(MONITORING_TOKEN_VALUE);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(X_MONITORING_TOKEN_HEADER_KEY, "BAD_MONITORING_TOKEN_VALUE");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance();

    assertThatExceptionOfType(UnauthorizedException.class)
        .as("The provided token does not match the monitoring token")
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_BAD_TOKEN_MESSAGE));
  }

  @Test
  void test_authorization_headerToken_matching() {
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
