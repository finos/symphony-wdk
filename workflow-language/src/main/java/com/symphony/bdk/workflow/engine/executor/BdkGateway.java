package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.core.service.connection.ConnectionService;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.core.service.user.UserService;

/**
 * A gateway to access all the BDK services as the SymphonyBDK object is not exposed in the Spring Boot BDK starter.
 */
public interface BdkGateway {
  /**
   * @return BDK service to send and manipulate messages.
   */
  MessageService messages();

  /**
   * @return BDK service to manage streams (aka rooms).
   */
  StreamService streams();

  /**
   * @return BDK service to manage users.
   */
  UserService users();

  /**
   * @return BDK service to manage connections.
   */
  ConnectionService connections();
}
