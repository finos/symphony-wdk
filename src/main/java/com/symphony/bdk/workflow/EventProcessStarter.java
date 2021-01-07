package com.symphony.bdk.workflow;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class EventProcessStarter {

    @Autowired
    private RuntimeService runtimeService;

    @EventListener
    public void onMessageSent(RealTimeEvent<V4MessageSent> event) throws PresentationMLParserException {
        // TODO try to serialize as json
        String content = PresentationMLParser.getTextContent(event.getSource().getMessage().getMessage());
        runtimeService.startProcessInstanceByMessage("messageSent_" + content,
                Collections.singletonMap("streamId", event.getSource().getMessage().getStream().getStreamId()));
    }
}
