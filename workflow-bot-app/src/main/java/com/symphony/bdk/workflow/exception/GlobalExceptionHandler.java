
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
    log.debug("", exception);
    return handle(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handle(UnauthorizedException exception) {
    log.error("Unauthorized exception: [{}]", exception.getMessage());
    log.trace("", exception);
    return handle(exception.getMessage(), HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(DuplicateException.class)
  public ResponseEntity<ErrorResponse> handle(DuplicateException exception) {
    log.error("Duplicated exception: [{}]", exception.getMessage());
    log.trace("", exception);
    return handle(exception.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handle(NotFoundException exception) {
    log.error("NotFound exception: [{}]", exception.getMessage());
    log.trace("", exception);
    return handle(exception.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handle(IllegalArgumentException exception) {
    log.error("Illegal argument exception: [{}]", exception.getMessage());
    log.debug("", exception);
    return handle(exception.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(UnprocessableEntityException.class)
  public ResponseEntity<ErrorResponse> handle(UnprocessableEntityException exception) {
    log.error("Unprocessable entity exception: [{}]", exception.getMessage());
    log.debug("", exception);
    return handle(exception.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  private ResponseEntity<ErrorResponse> handle(String errorMessage, HttpStatus status) {
    return ResponseEntity.status(status).body(new ErrorResponse(errorMessage));
  }
}

