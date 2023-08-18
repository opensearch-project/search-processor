/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.ranker;

import org.mockito.Mockito;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClient;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.PersonalizeIntelligentRankerConfiguration;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.PersonalizedRanker;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.PersonalizedRankerFactory;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.impl.AmazonPersonalizedRankerImpl;
import org.opensearch.test.OpenSearchTestCase;

import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.Constants.AMAZON_PERSONALIZED_RANKING_RECIPE_NAME;

public class PersonalizeRankerFactoryTests extends OpenSearchTestCase {

    private String personalizeCampaign = "arn:aws:personalize:us-west-2:000000000000:campaign/test-campaign";
    private String iamRoleArn = "sampleRoleArn";
    private String itemIdField = "ITEM_ID";
    private String region = "us-west-2";
    private double weight = 0.25;

    public void testGetPersonalizeRankerForPersonalizedRankingRecipe() {
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, AMAZON_PERSONALIZED_RANKING_RECIPE_NAME, itemIdField, region, weight);

        PersonalizedRankerFactory factory = new PersonalizedRankerFactory();
        PersonalizedRanker ranker = factory.getPersonalizedRanker(rankerConfig, client);
        assertEquals(ranker.getClass(), AmazonPersonalizedRankerImpl.class);
    }

    public void testGetPersonalizeRankerForUnknownRecipe() {
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, "sample-recipe", itemIdField, region, weight);

        PersonalizedRankerFactory factory = new PersonalizedRankerFactory();
        PersonalizedRanker ranker = factory.getPersonalizedRanker(rankerConfig, client);
        assertNull(ranker);
    }
}
