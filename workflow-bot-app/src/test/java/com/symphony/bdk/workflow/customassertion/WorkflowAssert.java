package com.symphony.bdk.workflow.customassertion;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import com.symphony.bdk.workflow.IntegrationTest;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.assertj.core.api.AbstractAssert;
import org.camunda.bpm.engine.history.HistoricDetail;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;

import java.util.List;
import java.util.Optional;

public class WorkflowAssert extends AbstractAssert<WorkflowAssert, Workflow> {
  public WorkflowAssert(Workflow workflow) {
    super(workflow, WorkflowAssert.class);
  }

  public static WorkflowAssert assertThat(Workflow actual) {
    return new WorkflowAssert(actual);
  }

  public WorkflowAssert isExecuted() {
    isNotNull();
    IntegrationTest.assertExecuted(actual);
    return this;
  }

  public WorkflowAssert isExecutedWithProcessAndActivities(Optional<String> process, List<String> activities) {
    isNotNull();
    IntegrationTest.assertExecuted(process, activities);
    return this;
  }

  public WorkflowAssert hasOutput(String key, Object value) {
    isNotNull();
    this.assertOutputs(key, value);
    return this;
  }

  private void assertOutputs(String key, Object value) {
    String process = IntegrationTest.lastProcess().orElseThrow();
    await().atMost(5, SECONDS).until(() -> IntegrationTest.processIsCompleted(process));

    final List<HistoricDetail> historicalDetails =
        IntegrationTest.historyService.createHistoricDetailQuery().processInstanceId(process).list();

    Optional<HistoricDetail> historicalDetailOptional = historicalDetails.stream()
        .filter(x -> ((HistoricDetailVariableInstanceUpdateEntity) x).getVariableName().equals(key))
        .findFirst();

    if (historicalDetailOptional.isEmpty()) {
      fail("No historical details found for the process.");
    } else {
      HistoricDetail historicalDetail = historicalDetailOptional.get();
      String actualVariableName = ((HistoricDetailVariableInstanceUpdateEntity) historicalDetail).getVariableName();
      Object actualVariableValue = ((HistoricDetailVariableInstanceUpdateEntity) historicalDetail).getValue();

      if (!actualVariableName.equals(key)) {
        failWithMessage("Expected variable key to be %s but was %s", key, actualVariableName);
      }

      if ((actualVariableValue == null || value == null) && actualVariableValue != value) {
        fail("Expected variable value to be %s but was %s", value, actualVariableValue);
      } else if (actualVariableValue != null && !actualVariableValue.equals(value)) {
        failWithMessage("Actual variable value was different to the expected one");
      }
    }
  }
}
