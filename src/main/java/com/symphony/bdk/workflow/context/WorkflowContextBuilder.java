package com.symphony.bdk.workflow.context;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.gen.api.model.V4AttachmentInfo;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.validator.YamlValidator;
import com.symphony.bdk.workflow.util.AttachmentsUtils;
import com.symphony.bdk.workflow.util.MessageUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class WorkflowContextBuilder {

  private RealTimeEvent<V4MessageSent> event;
  @Autowired
  private MessageService messageService;

  public WorkflowContextBuilder(){
  }

  public WorkflowContextBuilder fromEvent(RealTimeEvent<V4MessageSent> event){
    this.event = event;
    return this;
  }

  public WorkflowContext build() throws Exception {
    String attachmentsId = getFirstAttachmentIdFrom(AttachmentsUtils.getAttachmentsFrom(event));
    String messageId = MessageUtils.getMessageIdFrom(event);
    String streamId = MessageUtils.getStreamIdFrom(event);
    String content = MessageUtils.getMessageTextContent(event);

    return new WorkflowContext(streamId, messageId, attachmentsId, content,
        buildWorkflow(streamId, messageId, attachmentsId));
  }

  private Workflow buildWorkflow(String streamId, String messageId, String attachmentsId)
      throws IOException, ProcessingException {

    byte[] attachment = getDecodedAttachments(streamId, messageId, attachmentsId);
    YamlValidator.validateYamlString(new String(attachment, StandardCharsets.UTF_8));

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true));

    return mapper.readValue(attachment, Workflow.class);
  }

  private byte[] getDecodedAttachments(String streamId, String messageId, String attachmentsId){
    byte[] attachment = messageService.getAttachment(streamId,
        messageId, attachmentsId);

    return Base64.getDecoder().decode(attachment);
  }

  private String getFirstAttachmentIdFrom(List<V4AttachmentInfo> attachments) {
    Optional<V4AttachmentInfo> firstAttachment = attachments.stream().findFirst();

    return firstAttachment.isPresent() ? firstAttachment.get().getId() : null;
  }

}
