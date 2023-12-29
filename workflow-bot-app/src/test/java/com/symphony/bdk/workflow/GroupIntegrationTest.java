package com.symphony.bdk.workflow;

import com.symphony.bdk.ext.group.gen.api.model.GroupList;
import com.symphony.bdk.ext.group.gen.api.model.ReadGroup;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class GroupIntegrationTest extends IntegrationTest {

  @Test
  @DisplayName("Groups (SDL) basic operations")
  void groupBasicOperations() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/group/group.swadl.yaml"));

    when(groupService.insertGroup(any())).thenReturn(new ReadGroup().id("group-id-123").eTag("etag-123"));
    when(groupService.updateGroup(eq("etag-123"), any(),
        argThat(group -> "etag-123".equals(group.geteTag()) && "group-id-123".equals(group.getId()))))
        .thenReturn(new ReadGroup().id("group-id-123"));
    when(groupService.updateAvatar(any(), any())).thenReturn(new ReadGroup().id("group-id-123"));
    when(groupService.getGroup("group-id-123")).thenReturn(new ReadGroup().id("group-id-123"));
    when(groupService.listGroups(any(), any(), any(), any(), any())).thenReturn(
        new GroupList().addDataItem(new ReadGroup().id("group-id-123")));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/start-group"));

    assertThat(workflow).isExecuted();
  }

}
