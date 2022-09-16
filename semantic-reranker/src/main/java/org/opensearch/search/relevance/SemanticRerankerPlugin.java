/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.opensearch.action.support.ActionFilter;
import org.opensearch.client.Client;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.script.ScriptService;
import org.opensearch.search.relevance.actionfilter.SearchActionFilter;
import org.opensearch.search.relevance.client.KendraClient;
import org.opensearch.search.relevance.client.KendraClientSettings;
import org.opensearch.search.relevance.client.OpenSearchClient;
import org.opensearch.search.relevance.constants.Constants;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

public class SemanticRerankerPlugin extends Plugin implements ActionPlugin {

  private OpenSearchClient openSearchClient;
  private KendraClient kendraClient;
  
  @Override
  public List<ActionFilter> getActionFilters() {
    return Arrays.asList(new SearchActionFilter(this.openSearchClient, this.kendraClient));
  }
  
  @Override
  public List<Setting<?>> getSettings() {
    List<Setting<?>> settings = new ArrayList<>();
    settings.add(new Setting<>(Constants.ENABLED_SETTING_NAME, "", Function.identity(),
        Setting.Property.Dynamic, Setting.Property.IndexScope));
    settings.add(KendraClientSettings.ACCESS_KEY_SETTING);
    settings.add(KendraClientSettings.SECRET_KEY_SETTING);
    settings.add(KendraClientSettings.SESSION_TOKEN_SETTING);
    return settings;
  }
  
  @Override
  public Collection<Object> createComponents(
      Client client,
      ClusterService clusterService,
      ThreadPool threadPool,
      ResourceWatcherService resourceWatcherService,
      ScriptService scriptService,
      NamedXContentRegistry xContentRegistry,
      Environment environment,
      NodeEnvironment nodeEnvironment,
      NamedWriteableRegistry namedWriteableRegistry,
      IndexNameExpressionResolver indexNameExpressionResolver,
      Supplier<RepositoriesService> repositoriesServiceSupplier
  ) {
    this.openSearchClient = new OpenSearchClient(client);
    
    this.kendraClient = new KendraClient(KendraClientSettings.getClientSettings(environment.settings()));
    
    return ImmutableList.of(
        this.openSearchClient,
        this.kendraClient
    );
  }
  
}
