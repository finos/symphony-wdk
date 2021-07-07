package com.symphony.bdk.workflow.admin;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.Workflow;
import com.symphony.bdk.workflow.validators.YamlValidator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class BotAdminController {

  private final MessageService messageService;
  private final WorkflowBuilder workflowBuilder;
  private static final Logger logger = LoggerFactory.getLogger(BotAdminController.class);

  public BotAdminController(MessageService messageService, WorkflowBuilder workflowBuilder) {
    this.messageService = messageService;
    this.workflowBuilder = workflowBuilder;
  }

  @EventListener
  public void onMessageSent(RealTimeEvent<V4MessageSent> event)
      throws IOException, ProcessingException, PresentationMLParserException {
    // consider a message with an attachment as a workflow to run
    V4Message message = event.getSource().getMessage();
    String text = PresentationMLParser.getTextContent(message.getMessage());
    String streamId = message.getStream().getStreamId();
    String messageId = message.getMessageId();
    String attachmentId;

    if (message.getAttachments() != null) {
      attachmentId = message.getAttachments().get(0).getId();
    } else {
      return; // nothing to process if no attachment is found
    }

    if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
      byte[] attachment = messageService.getAttachment(streamId, messageId, attachmentId);
      byte[] decodedAttachment = Base64.getDecoder().decode(attachment);

      if (text.startsWith(YamlValidator.YAML_VALIDATION_COMMAND)) {
        YamlValidator.validateYamlString(new String(decodedAttachment, StandardCharsets.UTF_8));
        Workflow workflow = deserializeWorkflow(decodedAttachment);
        workflowBuilder.generateBpmnOutputFile(workflow);
      } else {
        Workflow workflow = deserializeWorkflow(decodedAttachment);
        workflowBuilder.addWorkflow(workflow);

        messageService.send(streamId,
            "<messageML>Ok, running workflow <b>" + workflow.getName() + "</b></messageML>");
      }
    }
  }

  private Workflow deserializeWorkflow(byte[] workflow) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true));
    return mapper.readValue(workflow, Workflow.class);
  }
}
