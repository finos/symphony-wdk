package com.symphony.bdk.workflow;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Test;

import java.io.File;

class WorkflowBuilderTest {

    @Test
    void name() {
        BpmnModelInstance hello = Bpmn.createExecutableProcess("hello")
                .startEvent().message("messageSent_hello")
                .serviceTask("test1")
                .intermediateCatchEvent().message("messageReply_hello")
                .serviceTask("second_step")
                .endEvent()
                .done();

        System.out.println(Bpmn.convertToString(hello));
//        File file = new File("/Users/yassine.amounane/Downloads/test.bpmn");
//        Bpmn.writeModelToFile(file, hello);
    }
}
