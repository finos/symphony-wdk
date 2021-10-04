package com.symphony.bdk.workflow.client;

import com.symphony.bdk.core.config.model.BdkConfig;
import com.symphony.bdk.http.api.ApiClient;
import com.symphony.bdk.http.api.ApiClientBuilder;
import com.symphony.bdk.http.jersey2.ApiClientBuilderProviderJersey2;

import org.apache.commons.lang3.StringUtils;

public class ApiClientFactory {

  private final BdkConfig bdkConfig;

  private ApiClientBuilder apiClientBuilder;

  public ApiClientFactory(BdkConfig bdkConfig) {
    this.bdkConfig = bdkConfig;
    final ApiClientBuilderProviderJersey2 apiClientBuilderProvider = new ApiClientBuilderProviderJersey2();

    this.apiClientBuilder = apiClientBuilderProvider.newInstance();

    if (this.bdkConfig.getProxy() != null) {
      this.apiClientBuilder =
          this.apiClientBuilder.withProxy(this.bdkConfig.getProxy().getHost(), this.bdkConfig.getProxy().getPort());

      if (!StringUtils.isEmpty(this.bdkConfig.getProxy().getUsername())) {
        this.apiClientBuilder = this.apiClientBuilder.withProxyCredentials(this.bdkConfig.getProxy().getUsername(),
            this.bdkConfig.getProxy().getPassword());
      }
    }
  }

  public ApiClient getClient(String basePath) {
    return this.apiClientBuilder.withBasePath(basePath).build();
  }

}
