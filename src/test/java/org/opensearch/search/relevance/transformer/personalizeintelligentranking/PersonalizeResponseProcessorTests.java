/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking;

import com.amazonaws.http.IdleConnectionReaper;
import org.mockito.Mockito;
import org.opensearch.OpenSearchParseException;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchResponseSections;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.env.TestEnvironment;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClient;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClientSettings;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.PersonalizeIntelligentRankerConfiguration;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.PersonalizedRankerFactory;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.impl.AmazonPersonalizedRankerImpl;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.PersonalizeRuntimeTestUtil;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.SearchTestUtil;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

public class PersonalizeResponseProcessorTests extends OpenSearchTestCase {

    private static final String TYPE = "personalize_ranking";
    private Settings settings = buildEnvSettings(Settings.EMPTY);
    private Environment env = TestEnvironment.newEnvironment(settings);
    private String personalizeCampaign = "arn:aws:personalize:us-west-2:000000000000:campaign/test-campaign";
    private String iamRoleArn = "";
    private String recipe = "sample-personalize-recipe";
    private String itemIdField = "ITEM_ID";
    private String region = "us-west-2";
    private double weight = 0.25;

    private PersonalizeClientSettings clientSettings = PersonalizeClientSettings.getClientSettings(env.settings());

    public void testCreateFactoryThrowsExceptionWithEmptyConfig() {
        PersonalizeRankingResponseProcessor.Factory factory
                = new PersonalizeRankingResponseProcessor.Factory(this.clientSettings);
        expectThrows(OpenSearchParseException.class, () -> factory.create(
                Collections.emptyMap(),
                null,
                null,
                Collections.emptyMap()
        ));
    }

    public void testCreateFactoryWithAllPersonalizeConfig() throws Exception {
        PersonalizeRankingResponseProcessor.Factory factory
                = new PersonalizeRankingResponseProcessor.Factory(this.clientSettings);

        Map<String,Object> configuration = new HashMap<>();
        configuration.put("campaign_arn", personalizeCampaign);
        configuration.put("item_id_field", itemIdField);
        configuration.put("recipe", recipe);
        configuration.put("weight", String.valueOf(weight));
        configuration.put("iam_role_arn", iamRoleArn);
        configuration.put("aws_region", region);

        PersonalizeRankingResponseProcessor personalizeResponseProcessor =
                factory.create(Collections.emptyMap(),"testTag","testingAllFields", configuration);

        assertEquals(TYPE, personalizeResponseProcessor.getType());
        assertEquals("testTag", personalizeResponseProcessor.getTag());
        assertEquals("testingAllFields", personalizeResponseProcessor.getDescription());
        IdleConnectionReaper.shutdown();
    }
}
