package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.gen.api.model.RoomSystemInfo;
import com.symphony.bdk.gen.api.model.RoomTag;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V2RoomSearchCriteria;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.gen.api.model.V3RoomSearchResults;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.exception.SwadlNotValidException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class RoomIntegrationTest extends IntegrationTest {

  @Test
  @DisplayName(
      "Given create-room activity with only user ids fields, when the message is received, "
          + "then an MIM is created with the given users")
  void createRoomWithUids() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/create-mim-with-uids.swadl.yaml"));
    final List<Long> uids = Arrays.asList(666L, 777L, 999L);
    final Stream stream = new Stream();
    stream.setId("0000");
    when(streamService.create(uids)).thenReturn(stream);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/create-mim"));
    verify(streamService, timeout(5000)).create(uids);
  }

  @Test
  @DisplayName(
      "Given create-room activity with only required room details such as name and description, "
          + "when the message is received, then a room with the given details is created")
  void createRoomWithDetails() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/create-room-with-details.swadl.yaml"));
    final String roomName = "The best room ever";
    final String roomDescription = "this is room description";
    final boolean isRoomPublic = true;
    final boolean viewHistory = true;
    final boolean discoverable = false;
    final boolean readOnly = true;
    final boolean crossPod = true;
    final boolean copyProtected = true;
    final boolean multilateralRoom = false;
    final boolean memberCanInvite = true;
    final String subType = "EMAIL";
    final List<RoomTag> keywords =
        Arrays.asList(new RoomTag().key("A").value("AA"), new RoomTag().key("B").value("BB"));

    final V3RoomDetail v3RoomDetail =
        this.buildRoomDetail("1234", roomName, roomDescription, isRoomPublic, viewHistory, discoverable, readOnly,
            crossPod, copyProtected, multilateralRoom, memberCanInvite, subType, keywords);
    when(streamService.create(any(V3RoomAttributes.class))).thenReturn(v3RoomDetail);

    engine.deploy(workflow);
    engine.onEvent(IntegrationTest.messageReceived("/create-room"));

    ArgumentCaptor<V3RoomAttributes> argumentCaptor = ArgumentCaptor.forClass(V3RoomAttributes.class);
    verify(streamService, timeout(5000)).create(argumentCaptor.capture());

    final V3RoomAttributes captorValue = argumentCaptor.getValue();

    final V3RoomAttributes expectedRoomAttributes =
        this.buildRoomAttributes(roomName, roomDescription, isRoomPublic, viewHistory, discoverable, readOnly, crossPod,
            copyProtected, multilateralRoom, memberCanInvite, subType, keywords);
    assertRoomAttributes(expectedRoomAttributes, captorValue);
  }

  @Test
  @DisplayName("Given create-room activity with uids and required room details, when the message is received,"
      + "then a room with given details and members is created")
  void createRoomWithDetailsAndMembers() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/create-room-with-details-members.swadl.yaml"));
    final String roomName = "The best room ever";
    final String roomDescription = "this is room description";
    final boolean isRoomPublic = false;
    final List<Long> uids = Arrays.asList(666L, 777L, 999L);

    final V3RoomDetail v3RoomDetail =
        this.buildRoomDetail("1234", roomName, roomDescription, isRoomPublic, null, null, null, null, null, null, null,
            null, null);
    when(streamService.create(any(V3RoomAttributes.class))).thenReturn(v3RoomDetail);

    engine.deploy(workflow);
    engine.onEvent(IntegrationTest.messageReceived("/create-room-members"));

    ArgumentCaptor<V3RoomAttributes> argumentCaptor = ArgumentCaptor.forClass(V3RoomAttributes.class);
    verify(streamService, timeout(5000)).create(argumentCaptor.capture());

    uids.forEach(uid -> verify(streamService, times(1)).addMemberToRoom(eq(uid), anyString()));

    final V3RoomAttributes captorValue = argumentCaptor.getValue();

    final V3RoomAttributes expectedRoomAttributes =
        this.buildRoomAttributes(roomName, roomDescription, isRoomPublic, null, null, null, true, null, null,
            null, null, null);
    assertRoomAttributes(expectedRoomAttributes, captorValue);
  }

  @Test()
  @DisplayName("Given an invalid create-room activity, when the message is received, then an error is thrown")
  void createRoomInvalidWorkflow() {
    assertThatThrownBy(() -> SwadlParser.fromYaml(
        getClass().getResourceAsStream("/room/create-room-invalid-workflow.swadl.yaml"))).isInstanceOf(
        SwadlNotValidException.class);
  }

  private void assertRoomAttributes(V3RoomAttributes expected, V3RoomAttributes actual) {
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    assertThat(actual.getPublic()).isEqualTo(expected.getPublic());
    assertThat(actual.getCopyProtected()).isEqualTo(expected.getCopyProtected());
    assertThat(actual.getDiscoverable()).isEqualTo(expected.getDiscoverable());
    assertThat(actual.getCrossPod()).isEqualTo(expected.getCrossPod());
    assertThat(actual.getKeywords()).isEqualTo(expected.getKeywords());
    assertThat(actual.getMembersCanInvite()).isEqualTo(expected.getMembersCanInvite());
    assertThat(actual.getReadOnly()).isEqualTo(expected.getReadOnly());
    assertThat(actual.getViewHistory()).isEqualTo(expected.getViewHistory());
    assertThat(actual.getSubType()).isEqualTo(expected.getSubType());
  }

  private V3RoomDetail buildRoomDetail(String id, String name, String description, boolean isPublic,
      Boolean viewHistory, Boolean discoverable, Boolean readOnly, Boolean crossPod, Boolean copyProtected,
      Boolean multilateralRoom, Boolean memberCanInvite, String subType, List<RoomTag> keywords) {
    V3RoomDetail v3RoomDetail = new V3RoomDetail();
    RoomSystemInfo roomSystemInfo = new RoomSystemInfo();
    roomSystemInfo.setId(id);
    V3RoomAttributes v3RoomAttributes = new V3RoomAttributes();
    v3RoomAttributes.name(name)
        .description(description)
        ._public(isPublic)
        .viewHistory(viewHistory)
        .discoverable(discoverable)
        .readOnly(readOnly)
        .crossPod(crossPod)
        .copyProtected(copyProtected)
        .multiLateralRoom(multilateralRoom)
        .membersCanInvite(memberCanInvite)
        .subType(subType)
        .keywords(keywords);
    v3RoomDetail.setRoomSystemInfo(roomSystemInfo);
    v3RoomDetail.setRoomAttributes(v3RoomAttributes);

    return v3RoomDetail;
  }

  private V3RoomAttributes buildRoomAttributes(String name, String description, Boolean isPublic, Boolean viewHistory,
      Boolean discoverable, Boolean readOnly, Boolean crossPod, Boolean copyProtected, Boolean multilateralRoom,
      Boolean memberCanInvite, String subType, List<RoomTag> keywords) {
    V3RoomAttributes expectedRoomAttributes = new V3RoomAttributes();
    return expectedRoomAttributes.name(name)
        .description(description)
        ._public(isPublic)
        .viewHistory(viewHistory)
        .discoverable(discoverable)
        .readOnly(readOnly)
        .crossPod(crossPod)
        .multiLateralRoom(multilateralRoom)
        .copyProtected(copyProtected)
        .subType(subType)
        .membersCanInvite(memberCanInvite)
        .keywords(keywords);
  }

  @Test
  void updateRoom() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/room/update-room.swadl.yaml"));

    V3RoomDetail roomDetail = new V3RoomDetail();
    RoomSystemInfo info = new RoomSystemInfo();
    info.setId("abc");
    roomDetail.setRoomSystemInfo(info);
    when(streamService.getRoomInfo("abc")).thenReturn(roomDetail);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-room"));

    ArgumentCaptor<V3RoomAttributes> attributes = ArgumentCaptor.forClass(V3RoomAttributes.class);
    verify(streamService, timeout(5000)).updateRoom(eq("abc"), attributes.capture());

    assertThat(attributes.getValue()).satisfies(a -> {
      assertThat(a.getName()).isNull();
      assertThat(a.getDescription()).isNotEmpty();
      assertThat(a.getDiscoverable()).isTrue();
      assertThat(a.getCopyProtected()).isNull();
    });
  }

  @Test
  void updateRoom_activate() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/update-room-activate.swadl.yaml"));

    V3RoomDetail roomDetail = new V3RoomDetail();
    RoomSystemInfo info = new RoomSystemInfo();
    info.setId("abc");
    roomDetail.setRoomSystemInfo(info);
    when(streamService.getRoomInfo("abc")).thenReturn(roomDetail);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-room"));

    verify(streamService, timeout(5000)).setRoomActive("abc", true);
  }

  @Test
  void addRoomMember() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/add-room-member.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/add-room-member"));

    verify(streamService, timeout(5000)).addMemberToRoom(123L, "abc");
    verify(streamService, timeout(5000)).addMemberToRoom(456L, "abc");
  }

  @Test
  void removeRoomMember() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/remove-room-member.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/remove-room-member"));

    verify(streamService, timeout(5000)).removeMemberFromRoom(123L, "abc");
    verify(streamService, timeout(5000)).removeMemberFromRoom(456L, "abc");
  }

  @Test
  void promoteRoomMember() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/promote-room-owner.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/promote-room-owner"));

    verify(streamService, timeout(5000)).promoteUserToRoomOwner(123L, "abc");
    verify(streamService, timeout(5000)).promoteUserToRoomOwner(456L, "abc");
  }

  @Test
  void demoteRoomMember() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/demote-room-owner.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/demote-room-owner"));

    verify(streamService, timeout(5000)).demoteUserToRoomParticipant(123L, "abc");
    verify(streamService, timeout(5000)).demoteUserToRoomParticipant(456L, "abc");
  }

  @Test
  void getRoom() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/room/get-room.swadl.yaml"));
    when(streamService.getRoomInfo("abc")).thenReturn(new V3RoomDetail());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-room"));

    verify(streamService, timeout(5000)).getRoomInfo("abc");
    assertThat(workflow).isExecuted();
  }

  @Test
  void getRoomMembers() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/get-room-members.swadl.yaml"));
    when(streamService.listRoomMembers("abc")).thenReturn(Collections.emptyList());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-room-members"));

    verify(streamService, timeout(5000)).listRoomMembers("abc");
    assertThat(workflow).isExecuted();
  }

  @SuppressWarnings("ConstantConditions") // for null pagination attribute with refEq
  @Test
  void getRooms() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/room/get-rooms.swadl.yaml"));

    V2RoomSearchCriteria query = new V2RoomSearchCriteria()
        .query("test")
        .labels(List.of("test", "test1"))
        .active(true)
        .sortOrder(V2RoomSearchCriteria.SortOrderEnum.BASIC);
    when(streamService.searchRooms(refEq(query))).thenReturn(new V3RoomSearchResults());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-rooms"));

    verify(streamService, timeout(5000)).searchRooms(refEq(query));
    assertThat(workflow).isExecuted();
  }

  @SuppressWarnings("ConstantConditions") // for null pagination attribute with refEq
  @Test
  void getRoomsPagination() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/get-rooms-pagination.swadl.yaml"));

    V2RoomSearchCriteria query = new V2RoomSearchCriteria()
        .query("test")
        .labels(List.of("test", "test1"))
        .active(true);
    when(streamService.searchRooms(refEq(query), refEq(new PaginationAttribute(10, 10))))
        .thenReturn(new V3RoomSearchResults());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-rooms-pagination"));

    verify(streamService, timeout(5000)).searchRooms(refEq(query), refEq(new PaginationAttribute(10, 10)));
    assertThat(workflow).isExecuted();
  }
}
