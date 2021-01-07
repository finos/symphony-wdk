package com.symphony.bdk.workflow;

import com.symphony.bdk.workflow.activities.Reply;
import com.symphony.bdk.workflow.activities.WhatTimeIsIt;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WorkflowDefinition {
    private RuntimeService runtimeService;

    @Autowired
    public WorkflowDefinition(RuntimeService runtimeService, RepositoryService repositoryService) {
        this.runtimeService = runtimeService;

        BpmnModelInstance hello = Bpmn.createExecutableProcess("hello")
                .startEvent().message("messageSent_hello")
                .serviceTask().camundaClass(Reply.class)
                .endEvent()
                .done();

        BpmnModelInstance time = Bpmn.createExecutableProcess("time")
                .startEvent().message("messageSent_time")
                .serviceTask().camundaClass(WhatTimeIsIt.class)
                .endEvent()
                .done();

        repositoryService.createDeployment()
                .addModelInstance("hello.bpmn", hello)
                .addModelInstance("time.bpmn", time)
                .deploy();
    }

}
