package com.symphony.bdk.workflow.lang.swadl;

import com.symphony.bdk.workflow.lang.swadl.activity.CreateRoom;
import com.symphony.bdk.workflow.lang.swadl.activity.SendMessage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Optional;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity {

  @JsonProperty("create-room")
  private CreateRoom createRoom;

  @JsonProperty("send-message")
  private SendMessage sendMessage;

  private String reply;

  public Optional<Event> getEvent() {
    if (createRoom != null) {
      return Optional.of(createRoom.getOn());
    } else if (sendMessage != null) {
      return Optional.of(sendMessage.getOn());
    }
    return Optional.empty();
  }
}

