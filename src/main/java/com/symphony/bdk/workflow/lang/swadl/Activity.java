package com.symphony.bdk.workflow.lang.swadl;

import com.symphony.bdk.workflow.lang.swadl.activity.BaseActivity;
import com.symphony.bdk.workflow.lang.swadl.activity.CreateRoom;
import com.symphony.bdk.workflow.lang.swadl.activity.ExecuteScript;
import com.symphony.bdk.workflow.lang.swadl.activity.SendMessage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Optional;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity {

  private CreateRoom createRoom;

  private SendMessage sendMessage;

  private ExecuteScript executeScript;

  public Optional<BaseActivity<?>> getActivity() {
    if (createRoom != null) {
      return Optional.of(createRoom);

    } else if (sendMessage != null) {
      return Optional.of(sendMessage);

    } else if (executeScript != null) {
      return Optional.of(executeScript);
    }

    return Optional.empty();
  }

  public Optional<Event> getEvent() {
    return getActivity().map(BaseActivity::getOn);
  }
}

