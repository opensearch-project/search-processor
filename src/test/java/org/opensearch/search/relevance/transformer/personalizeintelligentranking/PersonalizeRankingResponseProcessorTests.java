/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking;

import com.amazonaws.http.IdleConnectionReaper;
import org.apache.lucene.search.TotalHits;
import org.opensearch.OpenSearchParseException;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchResponseSections;
import org.opensearch.action.search.ShardSearchFailure;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.env.TestEnvironment;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClient;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClientSettings;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.PersonalizeRequestParameters;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.PersonalizeRuntimeTestUtil;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.SearchTestUtil;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.Constants.AMAZON_PERSONALIZED_RANKING_RECIPE_NAME;

public class PersonalizeRankingResponseProcessorTests extends OpenSearchTestCase {

    private static final String TYPE = PersonalizeRankingResponseProcessor.TYPE;
    private Settings settings = buildEnvSettings(Settings.EMPTY);
    private Environment env = TestEnvironment.newEnvironment(settings);
    private String personalizeCampaign = "arn:aws:personalize:us-west-2:000000000000:campaign/test-campaign";
    private String iamRoleArn = "arn:aws:iam::000000000000:role/test";
    private String itemIdField = "ITEM_ID";
    private String region = "us-west-2";
    private double weight = 1.0;
    private int numHits = 10;

    private PersonalizeClientSettings clientSettings = PersonalizeClientSettings.getClientSettings(env.settings());

    public void testCreateFactoryThrowsExceptionWithEmptyConfig() {
        PersonalizeRankingResponseProcessor.Factory factory
                = new PersonalizeRankingResponseProcessor.Factory(this.clientSettings);
        expectThrows(OpenSearchParseException.class, () -> factory.create(
                Collections.emptyMap(),
                null,
                null,
                false,
                Collections.emptyMap(),
                null
        ));
        IdleConnectionReaper.shutdown();
    }

    public void testFactory() {
        PersonalizeRankingResponseProcessor.Factory factory
                = new PersonalizeRankingResponseProcessor.Factory(this.clientSettings);
        // Test config without campaign
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("item_id_field", itemIdField);
        configuration.put("recipe", AMAZON_PERSONALIZED_RANKING_RECIPE_NAME);
        configuration.put("weight", String.valueOf(weight));
        configuration.put("iam_role_arn", iamRoleArn);
        configuration.put("aws_region", region);

        expectThrows(OpenSearchParseException.class, () -> factory.create(
                Collections.emptyMap(),
                null,
                null,
                false,
                configuration,
                null
        ));
        configuration.clear();

        // Test config without recipe
        configuration.put("campaign_arn", personalizeCampaign);
        configuration.put("item_id_field", itemIdField);
        configuration.put("weight", String.valueOf(weight));
        configuration.put("iam_role_arn", iamRoleArn);
        configuration.put("aws_region", region);

        expectThrows(OpenSearchParseException.class, () -> factory.create(
                Collections.emptyMap(),
                null,
                null,
                false,
                configuration,
                null
        ));
        configuration.clear();

        // Test config without region
        configuration.put("campaign_arn", personalizeCampaign);
        configuration.put("item_id_field", itemIdField);
        configuration.put("recipe", AMAZON_PERSONALIZED_RANKING_RECIPE_NAME);
        configuration.put("weight", String.valueOf(weight));
        configuration.put("iam_role_arn", iamRoleArn);
        expectThrows(OpenSearchParseException.class, () -> factory.create(
                Collections.emptyMap(),
                null,
                null,
                false,
                configuration,
                null
        ));
        configuration.clear();

        // Test config without weight
        configuration.put("campaign_arn", personalizeCampaign);
        configuration.put("item_id_field", itemIdField);
        configuration.put("recipe", AMAZON_PERSONALIZED_RANKING_RECIPE_NAME);
        configuration.put("iam_role_arn", iamRoleArn);
        configuration.put("aws_region", region);
        expectThrows(OpenSearchParseException.class, () -> factory.create(
                Collections.emptyMap(),
                null,
                null,
                false,
                configuration,
                null
        ));
        configuration.clear();

        // Test configuration with invalid weight value
        configuration.put("campaign_arn", personalizeCampaign);
        configuration.put("item_id_field", itemIdField);
        configuration.put("recipe", AMAZON_PERSONALIZED_RANKING_RECIPE_NAME);
        configuration.put("weight", "invalid");
        configuration.put("iam_role_arn", iamRoleArn);
        configuration.put("aws_region", region);
        expectThrows(OpenSearchParseException.class, () -> factory.create(
                Collections.emptyMap(),
                null,
                null,
                false,
                configuration,
                null
        ));
        configuration.clear();
        IdleConnectionReaper.shutdown();
    }

    public void testCreateFactoryWithAllPersonalizeConfig() throws Exception {
        PersonalizeRankingResponseProcessor.Factory factory
                = new PersonalizeRankingResponseProcessor.Factory(this.clientSettings);

        Map<String, Object> configuration = buildPersonalizeResponseProcessorConfig();

        PersonalizeRankingResponseProcessor personalizeResponseProcessor =
                factory.create(Collections.emptyMap(), "testTag", "testingAllFields", false, configuration, null);

        assertEquals(TYPE, personalizeResponseProcessor.getType());
        assertEquals("testTag", personalizeResponseProcessor.getTag());
        assertEquals("testingAllFields", personalizeResponseProcessor.getDescription());
        IdleConnectionReaper.shutdown();
    }

    public void testProcessorWithNoHits() throws Exception {
        PersonalizeClient mockClient = mock(PersonalizeClient.class);
        PersonalizeRankingResponseProcessor.Factory factory
                = new PersonalizeRankingResponseProcessor.Factory(this.clientSettings, (cp, r) -> mockClient);

        Map<String, Object> configuration = buildPersonalizeResponseProcessorConfig();

        PersonalizeRankingResponseProcessor personalizeResponseProcessor =
                factory.create(Collections.emptyMap(), "testTag", "testingAllFields", false, configuration, null);
        SearchRequest searchRequest = new SearchRequest();
        SearchHits hits = new SearchHits(new SearchHit[0], new TotalHits(0, TotalHits.Relation.EQUAL_TO), 0.0f);
        SearchResponseSections searchResponseSections = new SearchResponseSections(hits, null, null, false, false, null, 0);
        SearchResponse searchResponse = new SearchResponse(searchResponseSections, null, 1, 1, 0, 1, new ShardSearchFailure[0], null);

        SearchResponse response = personalizeResponseProcessor.processResponse(searchRequest, searchResponse);
        assertEquals(hits.getTotalHits().value, response.getHits().getTotalHits().value);
        IdleConnectionReaper.shutdown();
    }

    public void testProcessorWithPersonalizeContext() throws Exception {
        PersonalizeClient mockClient = PersonalizeRuntimeTestUtil.buildMockPersonalizeClient();

        PersonalizeRankingResponseProcessor.Factory factory
                = new PersonalizeRankingResponseProcessor.Factory(this.clientSettings, (cp, r) -> mockClient);

        Map<String, Object> configuration = buildPersonalizeResponseProcessorConfig();
        PersonalizeRankingResponseProcessor personalizeResponseProcessor =
                factory.create(Collections.emptyMap(), "testTag", "testingAllFields", false, configuration, null);

        Map<String, Object> personalizeContext = new HashMap<>();
        personalizeContext.put("contextKey2", "contextValue2");

        SearchResponse personalizedResponse =
                getPersonalizedRankingProcessorResponse(personalizeResponseProcessor, personalizeContext, numHits);

        List<SearchHit> transformedHits = Arrays.asList(personalizedResponse.getHits().getHits());
        List<String> rerankedDocumentIds;
        rerankedDocumentIds = transformedHits.stream()
                .filter(h -> h.getSourceAsMap().get(itemIdField) != null)
                .map(h -> h.getSourceAsMap().get(itemIdField).toString())
                .collect(Collectors.toList());

        ArrayList<String> expectedRankedDocumentIds = PersonalizeRuntimeTestUtil.expectedRankedItemIdsForGivenWeight(numHits, 1);
        assertEquals(expectedRankedDocumentIds, rerankedDocumentIds);
        IdleConnectionReaper.shutdown();
    }

    public void testProcessorWithHitsWithInvalidPersonalizeContext() throws Exception {
        PersonalizeClient mockClient = PersonalizeRuntimeTestUtil.buildMockPersonalizeClient();;

        PersonalizeRankingResponseProcessor.Factory factory
                = new PersonalizeRankingResponseProcessor.Factory(this.clientSettings, (cp, r) -> mockClient);

        Map<String, Object> configuration = buildPersonalizeResponseProcessorConfig();
        PersonalizeRankingResponseProcessor personalizeResponseProcessor =
                factory.create(Collections.emptyMap(), "testTag", "testingAllFields", false, configuration, null);

        Map<String, Object> personalizeContext = new HashMap<>();
        personalizeContext.put("contextKey2", 5);

        expectThrows(OpenSearchParseException.class, () ->
                getPersonalizedRankingProcessorResponse(personalizeResponseProcessor, personalizeContext, numHits));
        IdleConnectionReaper.shutdown();
    }

    public void testPersonalizeRankingResponse() throws Exception {
        PersonalizeClient personalizeClient = PersonalizeRuntimeTestUtil.buildMockPersonalizeClient();

        PersonalizeRankingResponseProcessor.Factory factory
                = new PersonalizeRankingResponseProcessor.Factory(this.clientSettings, (cp, r) -> personalizeClient);

        String itemField = "ITEM_ID";
        Map<String, Object> configuration = buildPersonalizeResponseProcessorConfig();

        PersonalizeRankingResponseProcessor responseProcessor =
                factory.create(Collections.emptyMap(), "testTag", "testingAllFields", false, configuration, null);

        SearchResponse personalizedResponse = getPersonalizedRankingProcessorResponse(responseProcessor, null, numHits);

        List<SearchHit> transformedHits = Arrays.asList(personalizedResponse.getHits().getHits());
        List<String> rerankedDocumentIds;
        rerankedDocumentIds = transformedHits.stream()
                .filter(h -> h.getSourceAsMap().get(itemField) != null)
                .map(h -> h.getSourceAsMap().get(itemField).toString())
                .collect(Collectors.toList());

        ArrayList<String> expectedRankedDocumentIds = PersonalizeRuntimeTestUtil.expectedRankedItemIdsForGivenWeight(numHits, 1);
        assertEquals(expectedRankedDocumentIds, rerankedDocumentIds);
        IdleConnectionReaper.shutdown();
    }

    public void testPersonalizeRankingResponseWithInvalidItemIdFieldName() throws Exception {
        PersonalizeClient personalizeClient = PersonalizeRuntimeTestUtil.buildMockPersonalizeClient();

        PersonalizeRankingResponseProcessor.Factory factory
                = new PersonalizeRankingResponseProcessor.Factory(this.clientSettings, (cp, r) -> personalizeClient);

        String itemFieldInvalid = "ITEM_ID_NOT_VALID";
        Map<String, Object> configuration = buildPersonalizeResponseProcessorConfig();
        configuration.put("item_id_field", itemFieldInvalid);

        PersonalizeRankingResponseProcessor responseProcessor =
                factory.create(Collections.emptyMap(), "testTag", "testingAllFields", false, configuration, null);

        expectThrows(OpenSearchParseException.class, () ->
                getPersonalizedRankingProcessorResponse(responseProcessor, null, numHits));
        IdleConnectionReaper.shutdown();
    }

    public void testPersonalizeRankingResponseWithDefaultItemIdField() throws Exception {
        PersonalizeClient personalizeClient = PersonalizeRuntimeTestUtil.buildMockPersonalizeClient();

        PersonalizeRankingResponseProcessor.Factory factory
                = new PersonalizeRankingResponseProcessor.Factory(this.clientSettings, (cp, r) -> personalizeClient);

        String itemIdFieldEmpty = "";
        Map<String, Object> configuration = buildPersonalizeResponseProcessorConfig();
        configuration.put("item_id_field", itemIdFieldEmpty);

        PersonalizeRankingResponseProcessor responseProcessor =
                factory.create(Collections.emptyMap(), "testTag", "testingAllFields", false, configuration, null);

        SearchResponse personalizedResponse = getPersonalizedRankingProcessorResponse(responseProcessor, null, numHits);

        List<SearchHit> transformedHits = Arrays.asList(personalizedResponse.getHits().getHits());
        List<String> rerankedDocumentIds;
        rerankedDocumentIds = transformedHits.stream()
                .filter(h -> h.getId() != null)
                .map(h -> h.getId())
                .collect(Collectors.toList());

        ArrayList<String> expectedRankedDocumentIds = PersonalizeRuntimeTestUtil.expectedRankedItemIdsForGivenWeight(numHits, 1);

        assertEquals(expectedRankedDocumentIds, rerankedDocumentIds);
        IdleConnectionReaper.shutdown();
    }

    private SearchResponse getPersonalizedRankingProcessorResponse(PersonalizeRankingResponseProcessor responseProcessor,
                                                                    Map<String, Object> personalizeContext,
                                                                    int numHits) throws Exception {

        PersonalizeRequestParameters personalizeRequestParams = new PersonalizeRequestParameters("user_1", personalizeContext);
        SearchRequest request = SearchTestUtil.createSearchRequestWithPersonalizeRequest(personalizeRequestParams);

        SearchHits searchHits = SearchTestUtil.getSampleSearchHitsForPersonalize(numHits);
        SearchResponseSections searchResponseSections = new SearchResponseSections(searchHits, null, null, false, false, null, 0);
        SearchResponse searchResponse = new SearchResponse(searchResponseSections, null, 1, 1, 0, 1, new ShardSearchFailure[0], null);

        SearchResponse personalizedResponse = responseProcessor.processResponse(request, searchResponse);

        return  personalizedResponse;
    }

    private Map<String, Object> buildPersonalizeResponseProcessorConfig() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("campaign_arn", personalizeCampaign);
        configuration.put("item_id_field", itemIdField);
        configuration.put("recipe", AMAZON_PERSONALIZED_RANKING_RECIPE_NAME);
        configuration.put("weight", String.valueOf(weight));
        configuration.put("iam_role_arn", iamRoleArn);
        configuration.put("aws_region", region);
        return configuration;
    }
}
