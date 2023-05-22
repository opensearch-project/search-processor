/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.relevance.transformer.personalizeintelligentranking.ranker.impl;

import org.mockito.Mockito;
import org.opensearch.search.SearchHits;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClient;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.PersonalizeIntelligentRankerConfiguration;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.PersonalizeRequestParameters;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.impl.AmazonPersonalizedRankerImpl;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.PersonalizeRuntimeTestUtil;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.SearchTestUtil;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;

public class AmazonPersonalizeRankerImplTests extends OpenSearchTestCase {

    private String personalizeCampaign = "arn:aws:personalize:us-west-2:000000000000:campaign/test-campaign";
    private String iamRoleArn = "sampleRoleArn";
    private String recipe = "sample-personalize-recipe";
    private String itemIdField = "ITEM_ID";
    private String region = "us-west-2";
    private double weight = 0.25;

    public void testReRank() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdField, region, weight);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult());

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(10);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);
        assertEquals(responseHits.getHits().length, transformedHits.getHits().length);
    }

    public void testReRankWithoutItemIdFieldInConfig() throws IOException {
        String blankItemIdField = "";
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, blankItemIdField, region, weight);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult());

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(10);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);
        assertEquals(responseHits.getHits().length, transformedHits.getHits().length);
    }
}
