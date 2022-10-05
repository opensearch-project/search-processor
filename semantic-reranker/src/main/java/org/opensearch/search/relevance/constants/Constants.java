/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.constants;

public class Constants {
  public static final String PLUGIN_NAME = "kendra_intelligent_ranking";
  public static final String PLUGIN_INDEX_SETTINGS_PREFIX = String.join(".", "index", PLUGIN_NAME);
  public static final String ENABLED_FIELD_NAME = "enabled";
  public static final String ENABLED_SETTING_NAME = String.join(".",PLUGIN_INDEX_SETTINGS_PREFIX, ENABLED_FIELD_NAME);

  public static final String KENDRA_RANKING_SERVICE_NAME = "kendrareranking";
}
