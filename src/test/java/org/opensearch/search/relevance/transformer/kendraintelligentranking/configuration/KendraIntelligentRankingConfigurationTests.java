package org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration;

import org.junit.Test;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class KendraIntelligentRankingConfigurationTests extends OpenSearchTestCase {

    @Test
    public void parseWithNullParserAndContext() {
        try {
            KendraIntelligentRankingConfiguration.parse(null, null);
            fail();
        } catch (NullPointerException | IOException e) {
        }

    }

}