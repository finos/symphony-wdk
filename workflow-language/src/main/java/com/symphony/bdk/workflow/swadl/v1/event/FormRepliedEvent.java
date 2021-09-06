package com.symphony.bdk.workflow.swadl.v1.event;

import lombok.Data;

/**
 * See https://perzoinc.atlassian.net/wiki/spaces/DevX/pages/2136016534/PLAT-10313+Workflow+language+form+elements
 */
@Data
public class FormRepliedEvent {
  private String formId;
}
