package org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class KendraIntelligentRankingConfigurationTest extends TestCase {

    @Test
    public void parseWithNullParserAndContext() {
        try {
            KendraIntelligentRankingConfiguration.parse(null, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fail();
    }
}