package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.validators.YAMLValidator;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class DatafeedEventToMessage {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @EventListener
    public void onMessageSent(RealTimeEvent<V4MessageSent> event) throws PresentationMLParserException {
        String content = PresentationMLParser.getTextContent(event.getSource().getMessage().getMessage());
        if (!content.startsWith(YAMLValidator.YAML_VALIDATION_COMMAND)) {
            // content being the command to start a workflow
            runtimeService.startProcessInstanceByMessage("message_" + content,
                Collections.singletonMap("streamId",
                    event.getSource().getMessage().getStream().getStreamId()));
        }
    }

    @EventListener
    public void onSymphonyElementsAction(RealTimeEvent<V4SymphonyElementsAction> event) {
        Map<String, Object> formReplies = (Map<String, Object>) event.getSource().getFormValues();
        runtimeService.createMessageCorrelation("formReply")
                .processInstanceId(event.getSource().getFormId())
                .setVariables(formReplies)
                .correlate();
    }

}
