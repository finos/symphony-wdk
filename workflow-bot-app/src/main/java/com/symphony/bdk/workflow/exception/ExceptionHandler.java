package com.symphony.bdk.workflow.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Component
@ControllerAdvice
@Slf4j
public class ExceptionHandler extends GlobalExceptionHandler {}
