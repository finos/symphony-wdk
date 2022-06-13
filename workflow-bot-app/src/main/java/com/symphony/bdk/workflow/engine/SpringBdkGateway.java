package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.core.OboServices;
import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.auth.AuthenticatorFactory;
import com.symphony.bdk.core.auth.exception.AuthInitializationException;
import com.symphony.bdk.core.auth.exception.AuthUnauthorizedException;
import com.symphony.bdk.core.config.model.BdkConfig;
import com.symphony.bdk.core.service.connection.ConnectionService;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.core.service.user.UserService;
import com.symphony.bdk.ext.group.SymphonyGroupService;
import com.symphony.bdk.workflow.engine.executor.BdkGateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Slf4j
@Component
@CacheConfig(cacheNames = "oboAuthSesssions")
public class SpringBdkGateway implements BdkGateway {

  private final MessageService messageService;
  private final StreamService streamService;
  private final UserService userService;
  private final ConnectionService connectionService;
  private final SymphonyGroupService groupService;
  private final BdkConfig config;
  private final AuthenticatorFactory authenticatorFactory;

  @Autowired
  public SpringBdkGateway(@Nonnull BdkConfig config, @Nullable AuthenticatorFactory authenticatorFactory,
      MessageService messageService,
      StreamService streamService, UserService userService,
      ConnectionService connectionService, @Lazy SymphonyGroupService groupService) {
    this.messageService = messageService;
    this.streamService = streamService;
    this.userService = userService;
    this.connectionService = connectionService;
    this.groupService = groupService;
    this.config = config;
    this.authenticatorFactory = authenticatorFactory;
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
  public OboServices obo(AuthSession oboSession) {
    return new OboServices(this.config, oboSession);
  }

  @Override
  @Cacheable
  public AuthSession obo(String username) {
    if (config.isOboConfigured()) {
      try {
        return this.authenticatorFactory.getOboAuthenticator().authenticateByUsername(username);
      } catch (AuthInitializationException | AuthUnauthorizedException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException("At least OBO username or userid should be configured.");
    }
  }

  @Override
  @Cacheable
  public AuthSession obo(Long userId) {
    if (config.isOboConfigured()) {
      try {
        return this.authenticatorFactory.getOboAuthenticator().authenticateByUserId(userId);
      } catch (AuthInitializationException | AuthUnauthorizedException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException("At least OBO username or userid should be configured.");
    }
  }

  @Override
  public SymphonyGroupService groups() {
    return groupService;
  }
}
