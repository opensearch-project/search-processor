/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import org.opensearch.search.relevance.client.OpenSearchClient;
import org.opensearch.search.relevance.configuration.ResultTransformerConfigurationFactory;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraClientSettings;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraHttpClient;
import org.opensearch.search.relevance.configuration.SearchConfigurationExtBuilder;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.KendraIntelligentRanker;
import org.opensearch.search.relevance.transformer.ResultTransformer;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankerSettings;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankingConfigurationFactory;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

public class SearchRelevancePlugin extends Plugin implements ActionPlugin, SearchPlugin {

  private OpenSearchClient openSearchClient;
  private KendraHttpClient kendraClient;
  private KendraIntelligentRanker kendraIntelligentRanker;

  private Collection<ResultTransformer> getAllResultTransformers() {
    // Initialize and add other transformers here
    return List.of(this.kendraIntelligentRanker);
  }

  private Collection<ResultTransformerConfigurationFactory> getResultTransformerConfigurationFactories() {
    return List.of(KendraIntelligentRankingConfigurationFactory.INSTANCE);
  }
  
  @Override
  public List<ActionFilter> getActionFilters() {
    return Arrays.asList(new SearchActionFilter(getAllResultTransformers(), openSearchClient));
  }
  
  @Override
  public List<Setting<?>> getSettings() {
    // NOTE: cannot use kendraIntelligentRanker.getTransformerSettings because the object is not yet created
    List<Setting<?>> allTransformerSettings = new ArrayList<>();
    allTransformerSettings.addAll(KendraIntelligentRankerSettings.getAllSettings());
    // Add settings for other transformers here
    return allTransformerSettings;
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
    this.kendraIntelligentRanker = new KendraIntelligentRanker(this.kendraClient);
    
    return Arrays.asList(
        this.openSearchClient,
        this.kendraClient,
        this.kendraIntelligentRanker
    );
  }

  @Override
  public List<SearchExtSpec<?>> getSearchExts() {
    Map<String, ResultTransformerConfigurationFactory> resultTransformerMap = getResultTransformerConfigurationFactories().stream()
            .collect(Collectors.toMap(ResultTransformerConfigurationFactory::getName, i -> i));
    return Collections.singletonList(
        new SearchExtSpec<>(SearchConfigurationExtBuilder.NAME,
                input -> new SearchConfigurationExtBuilder(input, resultTransformerMap),
                parser -> SearchConfigurationExtBuilder.parse(parser, resultTransformerMap)));
  }
  
}
