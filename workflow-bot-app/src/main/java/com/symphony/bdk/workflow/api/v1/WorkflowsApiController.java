package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.api.v1.dto.ErrorResponse;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowExecutionRequest;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.engine.UnauthorizedException;
import com.symphony.bdk.workflow.engine.WorkflowEngine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/workflows")
@Slf4j
public class WorkflowsApiController {

  @Autowired
  private WorkflowEngine workflowEngine;

  /**
   * Triggers the execution of a workflow given by its id. This is an asynchronous operation.
   *
   * @param token     Workflow's token to authenticate the request
   * @param id        Workflow's id that is provided in SWADL
   * @param arguments Pass arguments to the event triggering the workflow
   */
  @PostMapping("/{id}/execute")
  public ResponseEntity<Object> executeWorkflowById(@RequestHeader(name = "X-Workflow-Token") String token,
      @PathVariable String id, @RequestBody WorkflowExecutionRequest arguments) {

    try {
      log.info("Executing workflow {}", id);
      workflowEngine.execute(id, new ExecutionParameters(arguments.getArgs(), token));

    } catch (IllegalArgumentException illegalArgumentException) {
      log.warn("The workflow id {} provided in the request does not exist", id);
      return new ResponseEntity<>(new ErrorResponse(illegalArgumentException.getMessage()), HttpStatus.NOT_FOUND);

    } catch (UnauthorizedException unauthorizedException) {
      log.warn("The token provided in the request is not valid for this workflow");
      return new ResponseEntity<>(new ErrorResponse(unauthorizedException.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    return ResponseEntity.noContent().build();
  }

}
