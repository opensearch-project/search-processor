/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.configuration;

import static org.opensearch.search.relevance.transformer.TransformerType.RESULT_TRANSFORMER;

public class Constants {
  public static final String PLUGIN_NAME = "searchrelevance";

  public static final String PLUGIN_SETTING_PREFIX =
      String.join(".", "index", "plugin", PLUGIN_NAME);
  public static final String RESULT_TRANSFORMER_SETTING_PREFIX =
      String.join(".", PLUGIN_SETTING_PREFIX, RESULT_TRANSFORMER.toString());

  public static final String PROPERTIES = "properties";
  public static final String ORDER = "order";
}
