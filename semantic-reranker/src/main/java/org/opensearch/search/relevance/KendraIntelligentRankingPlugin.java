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
import java.util.Collections;
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
import org.opensearch.plugins.SearchPlugin;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.script.ScriptService;
import org.opensearch.search.relevance.actionfilter.SearchActionFilter;
import org.opensearch.search.relevance.client.KendraClientSettings;
import org.opensearch.search.relevance.client.KendraHttpClient;
import org.opensearch.search.relevance.client.OpenSearchClient;
import org.opensearch.search.relevance.control.KendraSearchExtBuilder;
import org.opensearch.search.relevance.constants.Constants;
import org.opensearch.search.relevance.ranker.KendraIntelligentRanker;
import org.opensearch.search.relevance.ranker.Ranker;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

public class KendraIntelligentRankingPlugin extends Plugin implements ActionPlugin, SearchPlugin {

  private OpenSearchClient openSearchClient;
  private KendraHttpClient kendraClient;
  private Ranker ranker;
  
  @Override
  public List<ActionFilter> getActionFilters() {
    return Arrays.asList(new SearchActionFilter(this.ranker));
  }
  
  @Override
  public List<Setting<?>> getSettings() {
    List<Setting<?>> settings = new ArrayList<>();
    // Following settings are stored in index settings
    settings.add(new Setting<>(Constants.ENABLED_SETTING_NAME, "", Function.identity(),
        Setting.Property.Dynamic, Setting.Property.IndexScope));
    // Following settings are stored in opensearch.keystore
    settings.add(KendraClientSettings.ACCESS_KEY_SETTING);
    settings.add(KendraClientSettings.SECRET_KEY_SETTING);
    settings.add(KendraClientSettings.SESSION_TOKEN_SETTING);
    // Following settings are stored in opensearch.yml
    settings.add(KendraClientSettings.SERVICE_ENDPOINT_SETTING);
    settings.add(KendraClientSettings.SERVICE_REGION_SETTING);
    settings.add(KendraClientSettings.ENDPOINT_ID_SETTING);
    settings.add(KendraClientSettings.ASSUME_ROLE_ARN_SETTING);
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
    this.kendraClient = new KendraHttpClient(KendraClientSettings.getClientSettings(environment.settings()));
    this.ranker = new KendraIntelligentRanker(this.openSearchClient, this.kendraClient);
    
    return ImmutableList.of(
        this.openSearchClient,
        this.kendraClient,
        this.ranker
    );
  }

  @Override
  public List<SearchExtSpec<?>> getSearchExts() {
    return Collections.singletonList(
        new SearchExtSpec<>(KendraSearchExtBuilder.NAME, KendraSearchExtBuilder::new, KendraSearchExtBuilder::parse));
  }
  
}