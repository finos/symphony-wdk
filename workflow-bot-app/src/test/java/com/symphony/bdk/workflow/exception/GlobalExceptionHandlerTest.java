package com.symphony.bdk.workflow.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.api.v1.dto.ErrorResponse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  @Test
  void testUnauthorizedException() {
    ErrorResponse expectedErrorResponse = new ErrorResponse("Unauthorized exception's message");
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handle(new UnauthorizedException("Unauthorized exception's message"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isEqualTo(expectedErrorResponse);
  }

  @Test
  void testIllegalArgumentException() {
    ErrorResponse expectedErrorResponse = new ErrorResponse("Illegal argument exception's message");
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handle(new IllegalArgumentException("Illegal argument exception's message"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo(expectedErrorResponse);
  }

  @Test
  void testNotFoundException() {
    ErrorResponse expectedErrorResponse = new ErrorResponse("NotFound exception's message");
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handle(new NotFoundException("NotFound exception's message"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isEqualTo(expectedErrorResponse);
  }

  @Test
  void testDuplicateException() {
    ErrorResponse expectedErrorResponse = new ErrorResponse("Duplicate exception's message");
    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handle(new DuplicateException("Duplicate exception's message"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo(expectedErrorResponse);
  }

  @Test
  void testInternalServerException() {
    ErrorResponse expectedErrorResponse = new ErrorResponse("Throwable exception's message");
    Throwable throwable = new Throwable("Throwable exception's message");
    ResponseEntity<ErrorResponse> response = globalExceptionHandler.handle(throwable);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isEqualTo(expectedErrorResponse);
  }
}
