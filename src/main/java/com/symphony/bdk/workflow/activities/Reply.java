package com.symphony.bdk.workflow.activities;

import com.symphony.bdk.core.service.message.MessageService;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Reply implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(Reply.class);

    @Autowired
    private MessageService messageService;

    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("Replying to human");

        String streamId = (String) execution.getVariable("streamId");

        String messageML = (String) execution.getVariable("messageML");
        messageML = messageML.replace("PROCESS_ID", execution.getProcessInstanceId());
        for (Map.Entry<String, Object> entry : execution.getVariables().entrySet()) {
            // messageML is part of the variables and present the messageML content, skip it
            if (!entry.getKey().equals("messageML")) {
                messageML = messageML.replace(entry.getKey(), entry.getValue().toString());
            }
        }

        messageService.send(streamId, messageML);
    }

}
