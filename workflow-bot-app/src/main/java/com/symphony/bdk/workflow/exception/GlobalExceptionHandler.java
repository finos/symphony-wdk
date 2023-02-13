
package com.symphony.bdk.workflow.exception;

import com.symphony.bdk.workflow.api.v1.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.persistence.OptimisticLockException;


@Component
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handle(Throwable exception) {
    log.error("Internal server error: [{}]", exception.getMessage());
    log.debug("", exception);
    return handle("Internal server error, something went wrong.", HttpStatus.INTERNAL_SERVER_ERROR);
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

  @ExceptionHandler(UnsupportedOperationException.class)
  public ResponseEntity<ErrorResponse> handle(UnsupportedOperationException exception) {
    log.error("Unsupported operation exception: [{}]", exception.getMessage());
    log.debug("", exception);
    return handle(exception.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(OptimisticLockException.class)
  public ResponseEntity<ErrorResponse> handle(OptimisticLockException exception) {
    log.error("Optimistic locking exception: [{}]", exception.getMessage());
    return handle("Workflow being updated is outdated, please refresh then update again.", HttpStatus.CONFLICT);
  }

  private ResponseEntity<ErrorResponse> handle(String errorMessage, HttpStatus status) {
    return ResponseEntity.status(status).body(new ErrorResponse(errorMessage));
  }
}

