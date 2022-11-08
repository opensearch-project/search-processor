/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration;

import static org.opensearch.search.relevance.configuration.Constants.ORDER;
import static org.opensearch.search.relevance.configuration.Constants.PROPERTIES;
import static org.opensearch.search.relevance.configuration.Constants.RESULT_TRANSFORMER_SETTING_PREFIX;

public class Constants {
  // Transformer name
  public static final String KENDRA_INTELLIGENT_RANKING = "kendra_intelligent_ranking";
  // Transformer properties
  public static final String BODY_FIELD = "body_field";
  public static final String TITLE_FIELD = "title_field";

  public static final String KENDRA_SETTINGS_PREFIX =
      String.join(".", RESULT_TRANSFORMER_SETTING_PREFIX, KENDRA_INTELLIGENT_RANKING);

  public static final String ORDER_SETTING_NAME =
      String.join(".", KENDRA_SETTINGS_PREFIX, ORDER);
  public static final String BODY_FIELD_SETTING_NAME =
      String.join(".", KENDRA_SETTINGS_PREFIX, PROPERTIES, BODY_FIELD);
  public static final String TITLE_FIELD_SETTING_NAME =
      String.join(".", KENDRA_SETTINGS_PREFIX, PROPERTIES, TITLE_FIELD);

}
