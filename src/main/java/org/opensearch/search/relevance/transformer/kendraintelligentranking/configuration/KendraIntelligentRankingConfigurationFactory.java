/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.search.relevance.configuration.ResultTransformerConfiguration;
import org.opensearch.search.relevance.configuration.ResultTransformerConfigurationFactory;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.KendraIntelligentRanker;

import java.io.IOException;

public class KendraIntelligentRankingConfigurationFactory implements ResultTransformerConfigurationFactory {
    private KendraIntelligentRankingConfigurationFactory() {

    }

    public static final KendraIntelligentRankingConfigurationFactory INSTANCE =
            new KendraIntelligentRankingConfigurationFactory();

    @Override
    public String getName() {
        return KendraIntelligentRanker.NAME;
    }

    @Override
    public ResultTransformerConfiguration configure(Settings indexSettings) {
        return new KendraIntelligentRankingConfiguration(indexSettings);
    }

    @Override
    public ResultTransformerConfiguration configure(XContentParser parser) throws IOException {
        return KendraIntelligentRankingConfiguration.parse(parser);
    }

    @Override
    public ResultTransformerConfiguration configure(StreamInput streamInput) throws IOException {
        return new KendraIntelligentRankingConfiguration(streamInput);
    }
}
