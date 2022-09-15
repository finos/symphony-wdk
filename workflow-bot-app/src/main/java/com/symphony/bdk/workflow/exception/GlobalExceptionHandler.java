package com.symphony.bdk.workflow.exception;

import com.symphony.bdk.workflow.api.v1.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@Component
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handle(Throwable exception) {
    log.error("Internal server error: [{}]", exception.getMessage());
    return handle(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }


  @ExceptionHandler(RuntimeException.class)
  public void handle(RuntimeException exception) {
    log.error("Runtime exception: [{}]", exception.getMessage());
    throw exception;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handle(IllegalArgumentException exception) {
    log.error("Illegal argument exception: [{}]", exception.getMessage());
    return handle(exception.getMessage(), HttpStatus.NOT_FOUND);
  }

  private ResponseEntity<ErrorResponse> handle(String errorMessage, HttpStatus status) {
    return ResponseEntity.status(status).body(new ErrorResponse(errorMessage));
  }
}
