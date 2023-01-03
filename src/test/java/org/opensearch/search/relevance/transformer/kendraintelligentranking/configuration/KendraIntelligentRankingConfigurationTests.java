/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration;

import org.junit.Test;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class KendraIntelligentRankingConfigurationTests extends OpenSearchTestCase {
    public void testParseWithNullParserAndContext() {
        try {
            KendraIntelligentRankingConfiguration.parse(null);
            fail();
        } catch (NullPointerException | IOException e) {
        }
    }
}