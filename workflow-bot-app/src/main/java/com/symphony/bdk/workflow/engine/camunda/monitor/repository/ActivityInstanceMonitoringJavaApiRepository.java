package com.symphony.bdk.workflow.engine.camunda.monitor.repository;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ActivityInstanceMonitoringJavaApiRepository
    implements ActivityInstanceMonitoringRepository<HistoricActivityInstance> {
  private final HistoryService historyService;

  @Override
  public List<HistoricActivityInstance> listInstanceActivities(String instanceId) {
    /*
       Instance activities are the sum of executed activities from the historyService (with status COMPLETED)
       and the activities not started yet from the workflow mapping object (with status PENDING)
       and eventually the one activity being executed (with status ONGOING).

       In order to have an ongoing activity, I tried with an execute-script having a sleep,
       as long as the activity (the sleep) is not completed,
       historyService and runtimeService are not updated with new rows.

       --> Maybe Camunda is synchronous and needs to be completely free (no ongoing work/activity) to update database.

       Let's try with a new activity who does a lot of work (a big for loop..): Same behavior
     */

    //TODO: handle variables
    return historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(instanceId)
        .orderByHistoricActivityInstanceEndTime()
        .asc()
        .list()
        .stream()
        .filter(a -> a.getActivityType().equals("serviceTask") || a.getActivityType().equals("scriptTask"))
        .collect(Collectors.toList());
  }

}
