package com.symphony.bdk.workflow.activities;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.model.Message;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Reply implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(Reply.class);

    @Autowired
    private MessageService messageService;

    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("Replying to human");
        String streamId = (String) execution.getVariable("streamId");
        messageService.send(streamId, Message.builder().content("Hello human!").build());
    }
}
