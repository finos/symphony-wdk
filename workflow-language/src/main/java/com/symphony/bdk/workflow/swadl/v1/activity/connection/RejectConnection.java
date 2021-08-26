package com.symphony.bdk.workflow.swadl.v1.activity.connection;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#reject-connection">Reject Connection API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RejectConnection extends Connection {}
