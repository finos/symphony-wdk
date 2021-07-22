package com.symphony.bdk.workflow.event;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4AttachmentInfo;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.lang.WorkflowBuilder;
import com.symphony.bdk.workflow.lang.exception.YamlNotValidException;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.validator.YamlValidator;
import com.symphony.bdk.workflow.util.AttachmentsUtils;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

// TODO this could be meta! a workflow to submit other workflows
@Slf4j
@Generated // for debugging only
@Component
public class WorkflowBotController {

  private final WorkflowEngine workflowEngine;
  private final MessageService messageService;

  public WorkflowBotController(WorkflowEngine workflowEngine, MessageService messageService) {
    this.workflowEngine = workflowEngine;
    this.messageService = messageService;
  }

  @EventListener
  public void onMessageSent(RealTimeEvent<V4MessageSent> event)
      throws PresentationMLParserException, IOException, ProcessingException {
    V4Message message = event.getSource().getMessage();
    String messageId = message.getMessageId();
    String text = PresentationMLParser.getTextContent(message.getMessage());
    String streamId = message.getStream().getStreamId();

    if (text.startsWith(YamlValidator.YAML_VALIDATION_COMMAND)) {
      String attachmentId = getFirstAttachmentIdFrom(AttachmentsUtils.getAttachmentsFrom(event));
      try {
        Workflow workflow = this.buildWorkflow(streamId, messageId, attachmentId);
        this.workflowEngine.execute(workflow);
        messageService.send(streamId,
            String.format("<messageML>Ok, validated <b>%s</b></messageML>", workflow.getName()));
      } catch (YamlNotValidException yamlNotValidException) {
        log.info(yamlNotValidException.getMessage());
        messageService.send(streamId, "<messageML>YAML file is not valid</messageML>");
      }
    }
  }

  private Workflow buildWorkflow(String streamId, String messageId, String attachmentsId)
      throws IOException, ProcessingException {
    byte[] attachment = getDecodedAttachments(streamId, messageId, attachmentsId);
    try (ByteArrayInputStream yaml = new ByteArrayInputStream(attachment)) {
      return WorkflowBuilder.fromYaml(yaml);
    }
  }

  private byte[] getDecodedAttachments(String streamId, String messageId, String attachmentsId) {
    byte[] attachment = messageService.getAttachment(streamId,
        messageId, attachmentsId);

    return Base64.getDecoder().decode(attachment);
  }

  private String getFirstAttachmentIdFrom(List<V4AttachmentInfo> attachments) {
    Optional<V4AttachmentInfo> firstAttachment = attachments.stream().findFirst();
    return firstAttachment.map(V4AttachmentInfo::getId).orElse(null);
  }
}
