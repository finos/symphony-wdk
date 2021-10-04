package com.symphony.bdk.workflow.client;

import com.symphony.bdk.core.util.ServiceLookup;
import com.symphony.bdk.http.api.ApiClient;
import com.symphony.bdk.http.api.ApiClientBuilder;
import com.symphony.bdk.http.api.ApiClientBuilderProvider;

public class ApiClientFactory {

  private final ApiClientBuilder apiClientBuilder;

  public ApiClientFactory() {
    final ApiClientBuilderProvider apiClientBuilderProvider =
        ServiceLookup.lookupSingleService(ApiClientBuilderProvider.class);

    this.apiClientBuilder = apiClientBuilderProvider.newInstance();
  }

  public ApiClient getClient(String basePath) {
    return this.apiClientBuilder.withBasePath(basePath).build();
  }

}
