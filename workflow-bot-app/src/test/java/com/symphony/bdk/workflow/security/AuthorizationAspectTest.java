package com.symphony.bdk.workflow.security;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.configuration.WorkflowBotConfiguration;
import com.symphony.bdk.workflow.exception.UnauthorizedException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;

@MockitoSettings(strictness = Strictness.LENIENT)
class AuthorizationAspectTest {

  private static final String UNAUTHORIZED_EXCEPTION_BAD_TOKEN_MESSAGE = "Request is not authorised";
  private static final String X_MONITORING_TOKEN_HEADER_KEY = "X-Monitoring-Token";
  private static final String X_MANAGEMENT_TOKEN_HEADER_KEY = "X-Management-Token";
  private static final String TOKEN_VALUE = "TOKEN_VALUE";

  @ParameterizedTest
  @ValueSource(strings = {X_MANAGEMENT_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_KEY})
  void test_authorization_monitoringTokenNotConfigured_headerSet(String header) {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn(null);
    when(workflowBotConfiguration.getManagementToken()).thenReturn(null);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(header, "BAD_MONITORING_TOKEN_VALUE");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance(header);

    assertThatExceptionOfType(UnauthorizedException.class)
        .as("No token will match when the monitoring token is not configured")
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(
            e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_BAD_TOKEN_MESSAGE));
  }

  @ParameterizedTest
  @ValueSource(strings = {X_MANAGEMENT_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_KEY})
  void test_authorization_monitoringTokenNotConfigured_headerSetWithEmptyString(String header) {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn("");
    when(workflowBotConfiguration.getManagementToken()).thenReturn("");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(header, "");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance(header);

    assertThatExceptionOfType(UnauthorizedException.class)
        .as("No token will match when the monitoring token is not configured, including empty string")
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(
            e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_BAD_TOKEN_MESSAGE));
  }

  @ParameterizedTest
  @ValueSource(strings = {X_MANAGEMENT_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_KEY})
  void test_authorization_monitoringTokenConfigured_headerTokenNotSet(String header) {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn(TOKEN_VALUE);
    when(workflowBotConfiguration.getManagementToken()).thenReturn(TOKEN_VALUE);

    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance(header);

    assertThatExceptionOfType(UnauthorizedException.class)
        .as("The header is not set in the request")
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_BAD_TOKEN_MESSAGE));
  }

  @ParameterizedTest
  @ValueSource(strings = {X_MANAGEMENT_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_KEY})
  void tst_authorization_headerToken_notMatching(String header) {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn(TOKEN_VALUE);
    when(workflowBotConfiguration.getManagementToken()).thenReturn(TOKEN_VALUE);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(header, "BAD_MONITORING_TOKEN_VALUE");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance(header);

    assertThatExceptionOfType(UnauthorizedException.class)
        .as("The provided token does not match the monitoring token")
        .isThrownBy(() -> authorizationAspect.authorizationCheck(authorizedInstance))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(UNAUTHORIZED_EXCEPTION_BAD_TOKEN_MESSAGE));
  }

  @ParameterizedTest
  @ValueSource(strings = {X_MANAGEMENT_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_KEY})
  void test_authorization_headerToken_matching(String header) {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn(TOKEN_VALUE);
    when(workflowBotConfiguration.getManagementToken()).thenReturn(TOKEN_VALUE);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(header, TOKEN_VALUE);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    AuthorizationAspect authorizationAspect = new AuthorizationAspect(workflowBotConfiguration);
    Authorized authorizedInstance = createAuthorizedInstance(header);

    try {
      authorizationAspect.authorizationCheck(authorizedInstance);
    } catch (Throwable e) {
      fail("No throwable should have been caught");
    }
  }

  private static Authorized createAuthorizedInstance(String header) {
    return new Authorized() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return null;
      }

      @Override
      public String headerTokenKey() {
        return header;
      }
    };
  }
}
