package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.customAssertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.UserSystemInfo;
import com.symphony.bdk.gen.api.model.V2UserAttributes;
import com.symphony.bdk.gen.api.model.V2UserCreate;
import com.symphony.bdk.gen.api.model.V2UserDetail;
import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;

class UsersIntegrationTest extends IntegrationTest {

  @Test
  void createUser() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/user/create-user.swadl.yaml"));

    when(userService.create(any())).thenReturn(new V2UserDetail().userSystemInfo(new UserSystemInfo()));
    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail());

    engine.execute(workflow);
    engine.onEvent(messageReceived("/create-user"));

    ArgumentCaptor<V2UserCreate> userCreate = ArgumentCaptor.forClass(V2UserCreate.class);
    verify(userService, timeout(5000)).create(userCreate.capture());

    assertThat(userCreate.getValue()).satisfies(user -> {
      assertThat(user.getUserAttributes().getEmailAddress()).isEqualTo("john@mail.com");
      assertThat(user.getUserAttributes().getFirstName()).isEqualTo("John");
      assertThat(user.getUserAttributes().getLastName()).isEqualTo("Lee");
    });

    verify(userService, timeout(5000)).updateStatus(any(), any());
    verify(userService, timeout(5000)).updateFeatureEntitlements(any(), any());

    assertThat(workflow).isExecuted();
  }

  @Test
  void createSystemUser() throws IOException, ProcessingException {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/user/create-system-user.swadl.yaml"));

    when(userService.create(any())).thenReturn(new V2UserDetail().userSystemInfo(new UserSystemInfo()));
    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail());

    engine.execute(workflow);
    engine.onEvent(messageReceived("/create-user"));

    ArgumentCaptor<V2UserCreate> userCreate = ArgumentCaptor.forClass(V2UserCreate.class);
    verify(userService, timeout(5000)).create(userCreate.capture());

    assertThat(userCreate.getValue()).satisfies(user -> {
      assertThat(user.getUserAttributes().getCurrentKey().getAction()).isEqualTo("SAVE");
      assertThat(user.getUserAttributes().getCurrentKey().getKey()).isEqualTo("abc");
      assertThat(user.getUserAttributes().getCurrentKey().getExpirationDate()).isEqualTo(1629210917000L);
    });

    assertThat(workflow).isExecuted();
  }

  @Test
  void updateUser() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/user/update-user.swadl.yaml"));

    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail().userSystemInfo(new UserSystemInfo()));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/update-user"));

    ArgumentCaptor<V2UserAttributes> userUpdate = ArgumentCaptor.forClass(V2UserAttributes.class);
    verify(userService, timeout(5000)).update(any(), userUpdate.capture());

    assertThat(userUpdate.getValue()).satisfies(user -> {
      assertThat(user.getEmailAddress()).isEqualTo("john@mail.com");
      assertThat(user.getFirstName()).isEqualTo("John");
      assertThat(user.getLastName()).isEqualTo("Lee");
    });

    verify(userService, timeout(5000)).updateStatus(any(), any());
    verify(userService, timeout(5000)).updateFeatureEntitlements(any(), any());

    assertThat(workflow).isExecuted();
  }

  @Test
  void updateSystemUser() throws IOException, ProcessingException {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/user/update-system-user.swadl.yaml"));

    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail().userSystemInfo(new UserSystemInfo()));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/update-user"));

    ArgumentCaptor<V2UserAttributes> userUpdate = ArgumentCaptor.forClass(V2UserAttributes.class);
    verify(userService, timeout(5000)).update(any(), userUpdate.capture());

    assertThat(userUpdate.getValue()).satisfies(user -> {
      assertThat(user.getDisplayName()).isEqualTo("Changed");
    });

    assertExecuted(workflow);
  }

  @Test
  void updateUser_statusOnly() throws IOException, ProcessingException {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/user/update-user-status.swadl.yaml"));

    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail());

    engine.execute(workflow);
    engine.onEvent(messageReceived("/update-user"));

    verify(userService, timeout(5000)).updateStatus(any(), any());
    verify(userService, never()).update(any(), any());

    assertThat(workflow).isExecuted();
  }

  @Test
  void addUserRole() throws IOException, ProcessingException {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/user/add-user-role.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/update-user"));

    verify(userService, timeout(5000).times(2)).addRole(any(), any());
    assertThat(workflow).isExecuted();
  }

  @Test
  void removeUserRole() throws IOException, ProcessingException {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/user/remove-user-role.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/update-user"));

    verify(userService, timeout(5000).times(2)).removeRole(any(), any());
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUser() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/user/get-user.swadl.yaml"));

    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail());

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-user"));

    verify(userService, timeout(5000)).getUserDetail(123L);
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUsersIds() throws IOException, ProcessingException {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/user/get-users-ids.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-users"));

    verify(userService, timeout(5000)).listUsersByIds(List.of(123L, 456L), true, false);
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUsersEmails() throws IOException, ProcessingException {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/user/get-users-emails.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-users"));

    verify(userService, timeout(5000)).listUsersByEmails(List.of("bob@mail.com", "eve@mail.com"), true, false);
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUsersUsernames() throws IOException, ProcessingException {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/user/get-users-usernames.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-users"));

    verify(userService, timeout(5000)).listUsersByUsernames(List.of("bob", "eve"), false);
    assertThat(workflow).isExecuted();
  }
}
