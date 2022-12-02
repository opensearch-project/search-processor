/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.configuration;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.common.settings.Settings;
import org.opensearch.search.SearchExtBuilder;
import org.opensearch.search.relevance.transformer.ResultTransformer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opensearch.search.relevance.configuration.Constants.RESULT_TRANSFORMER_SETTING_PREFIX;

public class ConfigurationUtils {

  /**
  * Get result transformer configurations from Search Request
  * @param settings all index settings configured for this plugin
  * @return ordered and validated list of result transformers, empty list if not specified
  */
  public static List<ResultTransformerConfiguration> getResultTransformersFromIndexConfiguration(Settings settings,
                                                                                                 Map<String, ResultTransformer> resultTransformerMap) {
    List<ResultTransformerConfiguration> indexLevelConfigs = new ArrayList<>();

    if (settings != null) {
      if (settings.getGroups(RESULT_TRANSFORMER_SETTING_PREFIX) != null) {
        for (Map.Entry<String, Settings> tranformerSettings : settings.getGroups(RESULT_TRANSFORMER_SETTING_PREFIX).entrySet()) {
          if (resultTransformerMap.containsKey(tranformerSettings.getKey())) {
            ResultTransformer transformer = resultTransformerMap.get(tranformerSettings.getKey());
            indexLevelConfigs.add(transformer.getConfigurationFactory().configureFromIndexSettings(tranformerSettings.getValue()));
          }
        }
      }
    }

    return reorderAndValidateConfigs(indexLevelConfigs);
  }

  /**
   * Get result transformer configurations from Search Request
   * @param searchRequest input request
   * @return ordered and validated list of result transformers, empty list if not specified
   */
  public static List<ResultTransformerConfiguration> getResultTransformersFromRequestConfiguration(
      final SearchRequest searchRequest) {

    // Fetch result transformers specified in request
    SearchConfigurationExtBuilder requestLevelSearchConfiguration = null;
    if (searchRequest.source() != null && searchRequest.source().ext() != null && !searchRequest.source().ext().isEmpty()) {
      // Filter ext builders by name
      List<SearchExtBuilder> extBuilders = searchRequest.source().ext().stream()
          .filter(searchExtBuilder -> SearchConfigurationExtBuilder.NAME.equals(searchExtBuilder.getWriteableName()))
          .collect(Collectors.toList());
      if (!extBuilders.isEmpty()) {
        requestLevelSearchConfiguration = (SearchConfigurationExtBuilder) extBuilders.get(0);
      }
    }

    List<ResultTransformerConfiguration> requestLevelConfigs = new ArrayList<>();
    if (requestLevelSearchConfiguration != null) {
      requestLevelConfigs = reorderAndValidateConfigs(requestLevelSearchConfiguration.getResultTransformers());
    }
    return requestLevelConfigs;
  }

  /**
   * Sort configurations in ascending order of invocation, and validate
   * @param configs list of result transformer configurations
   * @return ordered and validated list of result transformers
   */
  public static List<ResultTransformerConfiguration> reorderAndValidateConfigs(
      final List<ResultTransformerConfiguration> configs) throws IllegalArgumentException {

    // Sort
    configs.sort(Comparator.comparingInt(ResultTransformerConfiguration::getOrder));

    for (int i = 0; i < configs.size(); ++i) {
      if (configs.get(i).getOrder() != (i + 1)) {
        throw new IllegalArgumentException("Expected order [" + (i + 1) + "] for transformer [" +
            configs.get(i).getTransformerName() + "], but found [" + configs.get(i).getOrder() + "]");
      }
    }

    return configs;
  }
}
