/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.relevance.transformer.personalizeintelligentranking.ranker.impl;

import org.mockito.Mockito;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClient;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.PersonalizeIntelligentRankerConfiguration;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.PersonalizeRequestParameters;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.impl.AmazonPersonalizedRankerImpl;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.PersonalizeRuntimeTestUtil;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.SearchTestUtil;
import org.opensearch.test.OpenSearchTestCase;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;

public class AmazonPersonalizeRankerImplTests extends OpenSearchTestCase {

    private String personalizeCampaign = "arn:aws:personalize:us-west-2:000000000000:campaign/test-campaign";
    private String iamRoleArn = "sampleRoleArn";
    private String recipe = "sample-personalize-recipe";
    private String itemIdField = "ITEM_ID";
    private String region = "us-west-2";
    private double weight = 0.25;
    private int numOfHits = 10;

    public void testReRank() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdField, region, weight);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult());

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
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
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);
        assertEquals(responseHits.getHits().length, transformedHits.getHits().length);
    }

    public void testReRankWithRequestParameterContext() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdField, region, weight);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult());

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        Map<String, Object> context = new HashMap<>();
        context.put("contextKey", "contextValue");
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        requestParameters.setContext(context);
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);
        assertEquals(responseHits.getHits().length, transformedHits.getHits().length);
    }

    public void testReRankWithInvalidRequestParameterContext() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdField, region, weight);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult());

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        Map<String, Object> context = new HashMap<>();
        context.put("contextKey", 2);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        requestParameters.setContext(context);
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);
        assertEquals(responseHits.getHits().length, transformedHits.getHits().length);
    }

    public void testReRankWithNoUserId() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdField, region, weight);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult());

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        Map<String, Object> context = new HashMap<>();
        context.put("contextKey", "contextValue");
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setContext(context);
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);
        assertEquals(responseHits.getHits().length, transformedHits.getHits().length);
    }

    public void testReRankWithEmptyItemIdField() throws IOException {
        String itemIdEmpty = "";
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdEmpty, region, weight);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult());

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);
        assertEquals(responseHits.getHits().length, transformedHits.getHits().length);
    }

    public void testReRankWithNullItemIdField() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, null, region, weight);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult());

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);
        assertEquals(responseHits.getHits().length, transformedHits.getHits().length);
    }


    public void testReRankWithWeightAsZero() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdField, region, 0);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult(numOfHits));

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);

        List<SearchHit> originalHits = Arrays.asList(transformedHits.getHits());
        String itemIdfield = rankerConfig.getItemIdField();
        List<String> rerankedDocumentIds;

        rerankedDocumentIds = originalHits.stream()
                .filter(h -> h.getSourceAsMap().get(itemIdfield) != null)
                .map(h -> h.getSourceAsMap().get(itemIdfield).toString())
                .collect(Collectors.toList());

        ArrayList<String> expectedRankedDocumentIds = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsZero(numOfHits);

        assertEquals(expectedRankedDocumentIds, rerankedDocumentIds);
    }


    public void testReRankWithWeightAsOne() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdField, region, 1);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult(numOfHits));


        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);

        List<SearchHit> originalHits = Arrays.asList(transformedHits.getHits());
        String itemIdfield = rankerConfig.getItemIdField();
        List<String> rerankedDocumentIds;

        rerankedDocumentIds = originalHits.stream()
                .filter(h -> h.getSourceAsMap().get(itemIdfield) != null)
                .map(h -> h.getSourceAsMap().get(itemIdfield).toString())
                .collect(Collectors.toList());

        ArrayList<String> expectedRankedDocumentIds = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsOne(numOfHits);

        assertEquals(expectedRankedDocumentIds, rerankedDocumentIds);
    }

    public void testReRankWithWeightAsNietherZeroOrOne() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdField, region, weight);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult(numOfHits));

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);

        List<SearchHit> originalHits = Arrays.asList(transformedHits.getHits());
        String itemIdfield = rankerConfig.getItemIdField();
        List<String> rerankedDocumentIds;

        rerankedDocumentIds = originalHits.stream()
                .filter(h -> h.getSourceAsMap().get(itemIdfield) != null)
                .map(h -> h.getSourceAsMap().get(itemIdfield).toString())
                .collect(Collectors.toList());

        ArrayList<String> rerankedDocumentIdsWhenWeightIsOne = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsOne(numOfHits);
        ArrayList<String> rerankedDocumentIdsWhenWeightIsZero = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsZero(numOfHits);

        assertNotEquals(rerankedDocumentIdsWhenWeightIsOne, rerankedDocumentIds);
        assertNotEquals(rerankedDocumentIdsWhenWeightIsZero, rerankedDocumentIds);
    }

    public void testReRankWithWeightAsGreaterThanOne() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdField, region, 2);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult(numOfHits));

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);

        List<SearchHit> originalHits = Arrays.asList(transformedHits.getHits());
        String itemIdfield = rankerConfig.getItemIdField();
        List<String> rerankedDocumentIds;

        rerankedDocumentIds = originalHits.stream()
                .filter(h -> h.getSourceAsMap().get(itemIdfield) != null)
                .map(h -> h.getSourceAsMap().get(itemIdfield).toString())
                .collect(Collectors.toList());

        ArrayList<String> expectedRankedDocumentIds = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsZero(numOfHits);
        assertEquals(expectedRankedDocumentIds, rerankedDocumentIds);
    }

    public void testReRankWithWeightAsLessThanZero() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdField, region, -1);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult(numOfHits));

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);

        List<SearchHit> originalHits = Arrays.asList(transformedHits.getHits());
        String itemIdfield = rankerConfig.getItemIdField();
        List<String> rerankedDocumentIds;

        rerankedDocumentIds = originalHits.stream()
                .filter(h -> h.getSourceAsMap().get(itemIdfield) != null)
                .map(h -> h.getSourceAsMap().get(itemIdfield).toString())
                .collect(Collectors.toList());

        ArrayList<String> expectedRankedDocumentIds = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsZero(numOfHits);
        assertEquals(expectedRankedDocumentIds, rerankedDocumentIds);
    }


    public void testReRankWithWeightAsZeroWithNullItemIdField() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, "", region, 0);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResultWhenItemIdConfigIsEmpty(numOfHits));

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);

        List<SearchHit> originalHits = Arrays.asList(transformedHits.getHits());
        String itemIdfield = rankerConfig.getItemIdField();
        List<String> rerankedDocumentIds;

        rerankedDocumentIds = originalHits.stream()
                .filter(h -> h.getId() != null)
                .map(h -> h.getId())
                .collect(Collectors.toList());

        ArrayList<String> expectedRankedDocumentIds = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsZeroWithNullItemIdField(numOfHits);

        assertEquals(expectedRankedDocumentIds, rerankedDocumentIds);
    }


    public void testReRankWithWeightAsOneWithNullItemIdField() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, "", region, 1);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResultWhenItemIdConfigIsEmpty(numOfHits));


        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);

        List<SearchHit> originalHits = Arrays.asList(transformedHits.getHits());
        String itemIdfield = rankerConfig.getItemIdField();
        List<String> rerankedDocumentIds;

        rerankedDocumentIds = originalHits.stream()
                .filter(h -> h.getId() != null)
                .map(h -> h.getId())
                .collect(Collectors.toList());

        ArrayList<String> expectedRankedDocumentIds = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsOneWithNullItemIdField(numOfHits);

        assertEquals(expectedRankedDocumentIds, rerankedDocumentIds);
    }

    public void testReRankWithWeightAsNietherZeroOrOneWithNullItemIdField() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, "", region, weight);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResultWhenItemIdConfigIsEmpty(numOfHits));

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);

        List<SearchHit> originalHits = Arrays.asList(transformedHits.getHits());
        String itemIdfield = rankerConfig.getItemIdField();
        List<String> rerankedDocumentIds;

        rerankedDocumentIds = originalHits.stream()
                .filter(h -> h.getId() != null)
                .map(h -> h.getId())
                .collect(Collectors.toList());

        ArrayList<String> rerankedDocumentIdsWhenWeightIsOne = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsOneWithNullItemIdField(numOfHits);
        ArrayList<String> rerankedDocumentIdsWhenWeightIsZero = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsZeroWithNullItemIdField(numOfHits);

        assertNotEquals(rerankedDocumentIdsWhenWeightIsOne, rerankedDocumentIds);
        assertNotEquals(rerankedDocumentIdsWhenWeightIsZero, rerankedDocumentIds);
    }

    public void testReRankWithWeightAsGreaterThanOneWithNullItemIdField() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, "", region, 2);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResultWhenItemIdConfigIsEmpty(numOfHits));

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);

        List<SearchHit> originalHits = Arrays.asList(transformedHits.getHits());
        String itemIdfield = rankerConfig.getItemIdField();
        List<String> rerankedDocumentIds;

        rerankedDocumentIds = originalHits.stream()
                .filter(h -> h.getId() != null)
                .map(h -> h.getId())
                .collect(Collectors.toList());

        ArrayList<String> expectedRankedDocumentIds = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsZeroWithNullItemIdField(numOfHits);
        assertEquals(expectedRankedDocumentIds, rerankedDocumentIds);
    }

    public void testReRankWithWeightAsLessThanZeroWithNullItemIdField() throws IOException {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, "", region, -1);
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResultWhenItemIdConfigIsEmpty(numOfHits));

        AmazonPersonalizedRankerImpl ranker = new AmazonPersonalizedRankerImpl(rankerConfig, client);
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        SearchHits responseHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numOfHits);
        SearchHits transformedHits = ranker.rerank(responseHits, requestParameters);

        List<SearchHit> originalHits = Arrays.asList(transformedHits.getHits());
        String itemIdfield = rankerConfig.getItemIdField();
        List<String> rerankedDocumentIds;

        rerankedDocumentIds = originalHits.stream()
                .filter(h -> h.getId() != null)
                .map(h -> h.getId())
                .collect(Collectors.toList());

        ArrayList<String> expectedRankedDocumentIds = PersonalizeRuntimeTestUtil.expectedRankedItemIdsWhenWeightIsZeroWithNullItemIdField(numOfHits);
        assertEquals(expectedRankedDocumentIds, rerankedDocumentIds);
    }

}
