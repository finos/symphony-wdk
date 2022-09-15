package com.symphony.bdk.workflow.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphony.bdk.workflow.api.v1.dto.ActivityDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.ActivityInstanceView;
import com.symphony.bdk.workflow.api.v1.dto.EventDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.api.v1.dto.TaskDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.TaskTypeEnum;
import com.symphony.bdk.workflow.api.v1.dto.VariableView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowActivitiesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.exception.UnauthorizedException;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@WebMvcTest
class WorkflowsApiControllerTest {

  private static final String WORKFLOW_EXECUTE_PATH = "/v1/workflows/wfId/execute";
  private static final String LIST_WORKFLOWS_PATH = "/v1/workflows/";
  private static final String LIST_WORKFLOW_INSTANCES_PATH = "/v1/workflows/%s/instances";
  private static final String LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH =
      "/v1/workflows/%s/instances/%s/activities";
  private static final String LIST_WORKFLOW_DEFINITIONS_PATH = "/v1/workflows/%s/definitions";

  @Autowired
  protected MockMvc mockMvc;

  @MockBean
  protected MonitoringService monitoringService;

  @MockBean
  protected WorkflowEngine workflowEngine;

  @Test
  void executeWorkflowById_validRequestTest() throws Exception {
    doNothing().when(workflowEngine)
        .execute(eq("wfId"), any(ExecutionParameters.class));

    MvcResult mvcResult = mockMvc.perform(request(HttpMethod.POST, WORKFLOW_EXECUTE_PATH)
            .header("X-Workflow-Token", "myToken")
            .contentType("application/json")
            .content("{\"arguments\": {\"content\":\"hello\"}}"))
        .andExpect(status().isNoContent())
        .andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  void executeWorkflowById_noTokenProvidedTest() throws Exception {
    mockMvc.perform(request(HttpMethod.POST, WORKFLOW_EXECUTE_PATH)
            .contentType("application/json")
            .content("{\"args\": {\"content\":\"hello\"}}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void executeWorkflowById_runTimeExceptionTest() throws Exception {
    doThrow(new RuntimeException("Error parsing presentationML")).when(workflowEngine)
        .execute(eq("wfId"), any(ExecutionParameters.class));

    mockMvc.perform(request(HttpMethod.POST, WORKFLOW_EXECUTE_PATH)
            .header("X-Workflow-Token", "myToken")
            .contentType("application/json")
            .content("{\"args\": {\"content\":\"hello\"}}"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("Error parsing presentationML"));
  }

  @Test
  void executeWorkflowById_illegalArgumentExceptionTest() throws Exception {
    doThrow(new IllegalArgumentException("No workflow found with id wfId")).when(workflowEngine)
        .execute(eq("wfId"), any(ExecutionParameters.class));

    mockMvc.perform(request(HttpMethod.POST, WORKFLOW_EXECUTE_PATH)
            .header("X-Workflow-Token", "myToken")
            .contentType("application/json")
            .content("{\"args\": {\"content\":\"hello\"}}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("No workflow found with id wfId"));
  }

  @Test
  void executeWorkflowById_unauthorizedExceptionTest() throws Exception {
    doThrow(new UnauthorizedException("Token is not valid")).when(workflowEngine)
        .execute(eq("wfId"), any(ExecutionParameters.class));

    mockMvc.perform(request(HttpMethod.POST, WORKFLOW_EXECUTE_PATH)
            .header("X-Workflow-Token", "myToken")
            .contentType("application/json")
            .content("{\"args\": {\"content\":\"hello\"}}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Token is not valid"));
  }

  @Test
  void listAllWorkflows() throws Exception {
    WorkflowView workflowView1 = WorkflowView.builder().id("id1").version("1").build();
    WorkflowView workflowView2 = WorkflowView.builder().id("id2").version("2").build();

    when(monitoringService.listAllWorkflows()).thenReturn(Arrays.asList(workflowView1, workflowView2));

    mockMvc.perform(request(HttpMethod.GET, LIST_WORKFLOWS_PATH))
        .andExpect(status().isOk())

        .andExpect(jsonPath("[0].id").value("id1"))
        .andExpect(jsonPath("[0].version").value("1"))

        .andExpect(jsonPath("[1].id").value("id2"))
        .andExpect(jsonPath("[1].version").value("2"));
  }

  @Test
  void listWorkflowInstances() throws Exception {
    WorkflowInstView instanceView1 =
        workflowInstView("testWorkflowId", "instance1", 222L, 666L, 1, StatusEnum.COMPLETED);
    WorkflowInstView instanceView2 = workflowInstView("testWorkflowId", "instance2", 333L, 777L, 2, StatusEnum.PENDING);

    when(monitoringService.listWorkflowInstances("testWorkflowId")).thenReturn(
        Arrays.asList(instanceView1, instanceView2));

    mockMvc.perform(request(HttpMethod.GET, String.format(LIST_WORKFLOW_INSTANCES_PATH, "testWorkflowId")))
        .andExpect(status().isOk())

        .andExpect(jsonPath("[0].id").value("testWorkflowId"))
        .andExpect(jsonPath("[0].version").value(1))
        .andExpect(jsonPath("[0].instanceId").value("instance1"))
        .andExpect(jsonPath("[0].status").value("COMPLETED"))
        .andExpect(jsonPath("[0].startDate").isNotEmpty())
        .andExpect(jsonPath("[0].endDate").isNotEmpty())

        .andExpect(jsonPath("[1].id").value("testWorkflowId"))
        .andExpect(jsonPath("[1].version").value(2))
        .andExpect(jsonPath("[1].instanceId").value("instance2"))
        .andExpect(jsonPath("[1].status").value("PENDING"))
        .andExpect(jsonPath("[1].startDate").isNotEmpty())
        .andExpect(jsonPath("[1].endDate").isNotEmpty());
  }

  @Test
  void listWorkflowInstanceActivities() throws Exception {
    final String workflowId = "testWorkflowId";
    final String instanceId = "testInstanceId";

    VariablesDomain activityOutputs = new VariablesDomain();
    activityOutputs.setOutputs(Map.of("a", "b", "c", "d"));
    activityOutputs.setRevision(0);
    activityOutputs.setUpdateTime(Instant.ofEpochMilli(333L));

    VariablesDomain globalVariables = new VariablesDomain();
    globalVariables.setOutputs(Map.of("globalOne", "valueOne", "globalTwo", "valueTwo"));
    globalVariables.setRevision(0);
    globalVariables.setUpdateTime(Instant.ofEpochMilli(333L));

    ActivityInstanceView activityInstanceView1 =
        activityInstanceView(workflowId, "activity0", instanceId, TaskTypeEnum.SEND_MESSAGE_ACTIVITY,
            new VariableView(activityOutputs), 222L, 666L);

    ActivityInstanceView activityInstanceView2 =
        activityInstanceView(workflowId, "activity1", instanceId, TaskTypeEnum.CREATE_ROOM_ACTIVITY,
            new VariableView(activityOutputs), 333L, 777L);

    WorkflowActivitiesView workflowActivitiesView = new WorkflowActivitiesView();
    workflowActivitiesView.setActivities(Arrays.asList(activityInstanceView1, activityInstanceView2));
    workflowActivitiesView.setGlobalVariables(new VariableView(globalVariables));

    when(monitoringService.listWorkflowInstanceActivities(workflowId, instanceId)).thenReturn(workflowActivitiesView);

    mockMvc.perform(
            request(HttpMethod.GET, String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH, workflowId, instanceId)))
        .andExpect(status().isOk())

        .andExpect(jsonPath("globalVariables.outputs[\"globalOne\"]").value("valueOne"))
        .andExpect(jsonPath("globalVariables.outputs[\"globalTwo\"]").value("valueTwo"))
        .andExpect(jsonPath("globalVariables.revision").value(0))

        .andExpect(jsonPath("activities[0].workflowId").value(workflowId))
        .andExpect(jsonPath("activities[0].instanceId").value(instanceId))
        .andExpect(jsonPath("activities[0].activityId").value("activity0"))
        .andExpect(jsonPath("activities[0].type").value("SEND_MESSAGE_ACTIVITY"))
        .andExpect(jsonPath("activities[0].startDate").isNotEmpty())
        .andExpect(jsonPath("activities[0].endDate").isNotEmpty())
        .andExpect(jsonPath("activities[0].outputs[\"a\"]").value("b"))
        .andExpect(jsonPath("activities[0].outputs[\"c\"]").value("d"))

        .andExpect(jsonPath("activities[1].workflowId").value(workflowId))
        .andExpect(jsonPath("activities[1].instanceId").value(instanceId))
        .andExpect(jsonPath("activities[1].activityId").value("activity1"))
        .andExpect(jsonPath("activities[1].type").value("CREATE_ROOM_ACTIVITY"))
        .andExpect(jsonPath("activities[1].startDate").isNotEmpty())
        .andExpect(jsonPath("activities[1].endDate").isNotEmpty())

        .andExpect(jsonPath("activities[0].outputs[\"a\"]").value("b"))
        .andExpect(jsonPath("activities[0].outputs[\"c\"]").value("d"));
  }

  @Test
  void listWorkflowInstanceActivities_illegalArgument() throws Exception {
    final String illegalWorkflowId = "testWorkflowId";
    final String illegalInstanceId = "testInstanceId";
    final String errorMsg =
        String.format("Either no workflow deployed with id '%s' is found or the instance id '%s' is not correct",
            illegalWorkflowId, illegalInstanceId);

    when(monitoringService.listWorkflowInstanceActivities(illegalInstanceId, illegalInstanceId)).thenThrow(
        new IllegalArgumentException(errorMsg));

    mockMvc.perform(request(HttpMethod.GET,
            String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH, illegalInstanceId, illegalInstanceId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("message").value(errorMsg));
  }

  @Test
  void listWorkflowActivities() throws Exception {
    final String workflowId = "testWorkflowId";

    TaskDefinitionView activity0 = activityDefinitionView("activity0", Collections.emptyList(),
        Collections.singletonList("event"), TaskTypeEnum.SEND_MESSAGE_ACTIVITY);

    TaskDefinitionView event = eventDefinitionView(Collections.singletonList("activity0"),
        Collections.singletonList("activity1"), TaskTypeEnum.ROOM_UPDATED_EVENT);

    TaskDefinitionView activity1 = activityDefinitionView("activity1", Collections.singletonList("event"),
        Collections.emptyList(), TaskTypeEnum.SEND_MESSAGE_ACTIVITY);

    WorkflowDefinitionView workflowDefinitionView = WorkflowDefinitionView.builder()
        .workflowId(workflowId)
        .variables(Collections.emptyList())
        .flowNodes(Arrays.asList(activity0, event, activity1))
        .build();

    when(monitoringService.getWorkflowDefinition(workflowId)).thenReturn(workflowDefinitionView);

    mockMvc.perform(request(HttpMethod.GET, String.format(LIST_WORKFLOW_DEFINITIONS_PATH, workflowId)))
        .andExpect(status().isOk())

        .andExpect(jsonPath("$.workflowId").value(workflowId))
        .andExpect(jsonPath("$.variables").isEmpty())

        .andExpect(jsonPath("$.flowNodes[0].type").value("SEND_MESSAGE_ACTIVITY"))
        .andExpect(jsonPath("$.flowNodes[0].parents").isEmpty())
        .andExpect(jsonPath("$.flowNodes[0].children[0]").value("event"))

        .andExpect(jsonPath("$.flowNodes[1].type").value("ROOM_UPDATED_EVENT"))
        .andExpect(jsonPath("$.flowNodes[1].parents[0]").value("activity0"))
        .andExpect(jsonPath("$.flowNodes[1].children[0]").value("activity1"))

        .andExpect(jsonPath("$.flowNodes[2].type").value("SEND_MESSAGE_ACTIVITY"))
        .andExpect(jsonPath("$.flowNodes[2].parents[0]").value("event"))
        .andExpect(jsonPath("$.flowNodes[2].children").isEmpty());
  }

  @Test
  void listWorkflowActivities_illegalArgument() throws Exception {
    final String illegalWorkflowId = "testWorkflowId";
    final String errorMsg = String.format("No workflow deployed with id '%s' is found", illegalWorkflowId);

    when(monitoringService.getWorkflowDefinition(illegalWorkflowId)).thenThrow(new IllegalArgumentException(errorMsg));

    mockMvc.perform(request(HttpMethod.GET, String.format(LIST_WORKFLOW_DEFINITIONS_PATH, illegalWorkflowId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("message").value(errorMsg));
  }

  private WorkflowInstView workflowInstView(String workflowId, String instanceId, long start, long end, Integer version,
      StatusEnum status) {

    return WorkflowInstView.builder()
        .id(workflowId)
        .version(version)
        .instanceId(instanceId)
        .status(status)
        .startDate(Instant.ofEpochMilli(start))
        .endDate(Instant.ofEpochMilli(end))
        .build();
  }

  private ActivityInstanceView activityInstanceView(String workflowId, String activityId, String instanceId,
      TaskTypeEnum type, VariableView variables, Long start, Long end) {
    return ActivityInstanceView.builder()
        .workflowId(workflowId)
        .instanceId(instanceId)
        .activityId(activityId)
        .type(type)
        .startDate(Instant.ofEpochMilli(start))
        .endDate(Instant.ofEpochMilli(end))
        .outputs(variables.getOutputs())
        .build();
  }

  private ActivityDefinitionView activityDefinitionView(String activityId, List<String> parents, List<String> children,
      TaskTypeEnum type) {
    TaskDefinitionView taskDefinitionView = this.taskDefinitionView(parents, children, type);
    return new ActivityDefinitionView(taskDefinitionView, activityId);
  }

  private EventDefinitionView eventDefinitionView(List<String> parents, List<String> children, TaskTypeEnum type) {
    TaskDefinitionView taskDefinitionView = this.taskDefinitionView(parents, children, type);
    return new EventDefinitionView(taskDefinitionView);
  }

  private TaskDefinitionView taskDefinitionView(List<String> parents, List<String> children, TaskTypeEnum type) {
    return TaskDefinitionView.builder()
        .parents(parents)
        .children(children)
        .type(type)
        .build();
  }

}

