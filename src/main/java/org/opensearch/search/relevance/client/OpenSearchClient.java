/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.client;

import org.opensearch.action.admin.indices.settings.get.GetSettingsAction;
import org.opensearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.opensearch.client.Client;
import org.opensearch.common.settings.Settings;

public class OpenSearchClient {
  private final Client client;

  public OpenSearchClient(Client client) {
    this.client = client;
  }

  public Settings getIndexSettings(String indexName, String[] settingNames) {
    GetSettingsRequest getSettingsRequest = new GetSettingsRequest()
        .indices(indexName);
    if (settingNames != null && settingNames.length > 0) {
      getSettingsRequest.names(settingNames);
    }
    GetSettingsResponse getSettingsResponse = client.execute(GetSettingsAction.INSTANCE, getSettingsRequest).actionGet();
    return getSettingsResponse.getIndexToSettings().get(indexName);
  }
}
