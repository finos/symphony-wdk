package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.workflow.activities.Reply;
import com.symphony.bdk.workflow.swadl.Activity;
import com.symphony.bdk.workflow.swadl.Workflow;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WorkflowBuilder {

    private final RepositoryService repositoryService;

    // run a single workflow at anytime
    private Deployment deploy;

    @Autowired
    public WorkflowBuilder(RepositoryService repositoryService) throws IOException {
        this.repositoryService = repositoryService;

//        ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
//                .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true));
//        Workflow workflow = mapper.readValue(getClass().getResourceAsStream("/calculator.yaml"), Workflow.class);
//        addWorkflow(workflow);
    }

    public void addWorkflow(Workflow workflow) {
        BpmnModelInstance instance = workflowToBpmn(workflow);

        if (deploy != null) {
            repositoryService.deleteDeployment(deploy.getId());
        }
        deploy = repositoryService.createDeployment()
                .addModelInstance(workflow.getName() + ".bpmn", instance)
                .deploy();
    }

    private BpmnModelInstance workflowToBpmn(Workflow workflow) {
        ProcessBuilder process = Bpmn.createExecutableProcess(workflow.getName());
        String commandToStart = workflow.getActivities().get(0).getOn().getMessage();
        AbstractFlowNodeBuilder eventBuilder = process.startEvent().message("message_" + commandToStart);

        for (Activity activity : workflow.getActivities()) {
            if (activity.getOn() != null && activity.getOn().getFormReply() != null) {
                eventBuilder = eventBuilder.receiveTask().message("formReply");
            }

            if (activity.getReply() != null) {
                eventBuilder = eventBuilder.serviceTask()
                        .camundaClass(Reply.class)
                        .camundaInputParameter("messageML", activity.getReply());

            } else if (activity.getScript() != null) {
                eventBuilder = eventBuilder.scriptTask()
                        .scriptFormat("groovy")
                        .scriptText(activity.getScript());
            }
        }

        return eventBuilder.endEvent().done();
    }

}
