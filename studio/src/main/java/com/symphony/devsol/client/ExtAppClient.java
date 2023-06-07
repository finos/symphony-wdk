package com.symphony.devsol.client;

import com.symphony.bdk.core.auth.ExtensionAppAuthenticator;
import com.symphony.bdk.core.auth.exception.AuthInitializationException;
import com.symphony.bdk.core.auth.jwt.UserClaim;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExtAppClient {
  private final ExtensionAppAuthenticator extAppAuth;

  @Cacheable("jwt")
  public UserClaim validate(String jwt) throws AuthInitializationException {
    return extAppAuth.validateJwt(jwt);
  }
}
