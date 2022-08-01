/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance;

import org.opensearch.action.support.ActionFilter;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.search.relevance.actionfilter.SearchActionFilter;

import java.util.Arrays;
import java.util.List;

public class SemanticRerankerPlugin extends Plugin implements ActionPlugin {

  @Override
  public List<ActionFilter> getActionFilters() {
    return Arrays.asList(new SearchActionFilter());
  }
}
