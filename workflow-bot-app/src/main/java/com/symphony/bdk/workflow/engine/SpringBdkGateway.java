package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.core.config.model.BdkConfig;
import com.symphony.bdk.core.service.connection.ConnectionService;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.core.service.user.UserService;
import com.symphony.bdk.http.api.ApiClient;
import com.symphony.bdk.workflow.client.ApiClientFactory;
import com.symphony.bdk.workflow.engine.executor.BdkGateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpringBdkGateway implements BdkGateway {

  private final MessageService messageService;
  private final StreamService streamService;
  private final UserService userService;
  private final ConnectionService connectionService;

  // not a bean
  private final ApiClientFactory apiClientFactory;

  @Autowired
  public SpringBdkGateway(MessageService messageService,
      StreamService streamService, UserService userService,
      ConnectionService connectionService, BdkConfig bdkConfig) {
    this.messageService = messageService;
    this.streamService = streamService;
    this.userService = userService;
    this.connectionService = connectionService;
    this.apiClientFactory = new ApiClientFactory(bdkConfig);
  }

  @Override
  public MessageService messages() {
    return messageService;
  }

  @Override
  public StreamService streams() {
    return streamService;
  }

  @Override
  public UserService users() {
    return userService;
  }

  @Override
  public ConnectionService connections() {
    return connectionService;
  }

  @Override
  public ApiClient apiClient(String basePath) {
    return this.apiClientFactory.getClient(basePath);
  }

}
