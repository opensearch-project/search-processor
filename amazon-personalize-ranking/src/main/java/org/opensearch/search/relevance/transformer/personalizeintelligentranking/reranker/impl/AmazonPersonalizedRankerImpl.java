/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.impl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.personalizeruntime.model.GetPersonalizedRankingRequest;
import com.amazonaws.services.personalizeruntime.model.GetPersonalizedRankingResult;
import com.amazonaws.services.personalizeruntime.model.PredictedItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.ingest.ConfigurationUtils;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.PersonalizeRankingResponseProcessor;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClient;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.PersonalizeIntelligentRankerConfiguration;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.PersonalizeRequestParameters;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.PersonalizedRanker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Personalize Re Ranker implementation using Amazon Personalized Ranking recipe
 */
public class AmazonPersonalizedRankerImpl implements PersonalizedRanker {
    private static final Logger logger = LogManager.getLogger(AmazonPersonalizedRankerImpl.class);
    private final PersonalizeIntelligentRankerConfiguration rankerConfig;
    private final PersonalizeClient personalizeClient;
    private static final String INSUFFCIENT_PERMISSION_ERROR_MESSAGE =
            "Insufficient privileges for calling personalize campaign. Please ensure that the supplied role is configured correctly.";
    private static final String ACCESS_DENIED_EXCEPTION_ERROR_CODE = "AccessDeniedException";
    public AmazonPersonalizedRankerImpl(PersonalizeIntelligentRankerConfiguration config,
                                        PersonalizeClient client) {
        this.rankerConfig = config;
        this.personalizeClient = client;
    }

    /**
     * Re rank search hits using Personalize campaign that uses Personalized Ranking recipe
     * @param hits              search hits returned by open search
     * @param requestParameters request parameters for Personalize present in search request
     * @return search hots re ranked using Amazon Personalize
     */
    @Override
    public SearchHits rerank(SearchHits hits, PersonalizeRequestParameters requestParameters) {
        try {
            validatePersonalizeRequestParams(requestParameters);
            List<SearchHit> originalHits = Arrays.asList(hits.getHits());
            // Do not make Personalize call if weight is zero which implies Personalization is turned off.
            if (rankerConfig.getWeight() == 0) {
                logger.info("Not applying Personalized ranking. Given value for weight configuration: {}", rankerConfig.getWeight());
                return hits;
            }
            String itemIdfield = rankerConfig.getItemIdField();
            List<String> documentIdsToRank;
            // If item field is not specified in the configuration then use default _id field.
            if (itemIdfield != null && !itemIdfield.isBlank()) {
                documentIdsToRank = originalHits.stream()
                        .filter(h -> h.getSourceAsMap().get(itemIdfield) != null)
                        .map(h -> h.getSourceAsMap().get(itemIdfield).toString())
                        .collect(Collectors.toList());
            } else {
                documentIdsToRank = originalHits.stream()
                        .filter(h -> h.getId() != null)
                        .map(h -> h.getId())
                        .collect(Collectors.toList());
            }
            if (documentIdsToRank.size() == 0) {
                throw ConfigurationUtils.newConfigurationException(PersonalizeRankingResponseProcessor.TYPE, "", "item_id_field",
                        "no item ids found to apply Personalized reranking. Please check configured value for item_id_field");
            }
            logger.info("Document Ids to re-rank with Personalize: {}", Arrays.toString(documentIdsToRank.toArray()));
            String userId = requestParameters.getUserId();
            Map<String, String> context = requestParameters.getContext() != null ?
                                            requestParameters.getContext().entrySet().stream()
                                                    .collect(Collectors.toMap(Map.Entry::getKey, e -> (String)e.getValue()))
                                            : null;
            logger.info("User ID from personalize request parameters - User ID: {}", userId);
            if (context != null && !context.isEmpty()) {
                logger.info("Personalize context provided in the search request");
            }

            GetPersonalizedRankingRequest personalizeRequest = new GetPersonalizedRankingRequest()
                    .withCampaignArn(rankerConfig.getPersonalizeCampaign())
                    .withInputList(documentIdsToRank)
                    .withContext(context)
                    .withUserId(userId);
            GetPersonalizedRankingResult result = personalizeClient.getPersonalizedRanking(personalizeRequest);

            SearchHits personalizedHits = combineScores(hits, result);
            return personalizedHits;
        } catch (AmazonServiceException e) {
            logger.error("Exception while calling personalize campaign: {}", e.getMessage());
            if (ACCESS_DENIED_EXCEPTION_ERROR_CODE.equals(e.getErrorCode())) {
                throw new IllegalArgumentException(INSUFFCIENT_PERMISSION_ERROR_MESSAGE);
            }
            throw e;
        }
        catch (Exception ex) {
            logger.error("Failed to re rank with Personalize.", ex);
            throw ex;
        }
    }

    //Combine open search hits and personalize campaign response
    private SearchHits combineScores(SearchHits originalHits, GetPersonalizedRankingResult personalizedRankingResult) {
        List<PredictedItem> personalziedRanking = personalizedRankingResult.getPersonalizedRanking();
        List<String> personalizedRankedItemsList = new LinkedList<>();
        for (PredictedItem item : personalziedRanking) {
            personalizedRankedItemsList.add(item.getItemId());
        }
        int totalHits = originalHits.getHits().length;
        List<SearchHit> rerankedHits = new ArrayList<>(totalHits);
        float maxScore = 0f;
        double weight = rankerConfig.getWeight();
        for (int i = 0 ; i < totalHits ; i++) {
            String openSearchItemId;
            SearchHit hit = originalHits.getAt(i);
            String itemIdField = rankerConfig.getItemIdField();
            if (itemIdField != null && !(itemIdField.isBlank())) {
                openSearchItemId = hit.getSourceAsMap().get(rankerConfig.getItemIdField()).toString();
            } else {
                openSearchItemId = hit.getId();
            }
            int openSearchRank = i + 1;
            int personalizedRank = personalizedRankedItemsList.indexOf(openSearchItemId) + 1;
            float combinedScore = (float) (((1- weight) / (Math.log(openSearchRank + 1) / Math.log(2)))
                    + ((weight) / (Math.log(personalizedRank + 1) / Math.log(2))));
            maxScore = Math.max(maxScore, combinedScore);
            hit.score(combinedScore);
            rerankedHits.add(hit);
        }
        rerankedHits.sort(Comparator.comparing(SearchHit::getScore).reversed());
        return new SearchHits(rerankedHits.toArray(new SearchHit[0]), originalHits.getTotalHits(), maxScore);
    }

    /**
     * Validate Personalize configuration for calling Personalize service
     * @param requestParameters Request parameters for Personalize present in search request
     */
    private void validatePersonalizeRequestParams(PersonalizeRequestParameters requestParameters) {
        if (requestParameters == null || requestParameters.getUserId() == null || requestParameters.getUserId().isBlank()) {
            throw ConfigurationUtils.newConfigurationException(PersonalizeRankingResponseProcessor.TYPE, "", "user_id",
                    "required Personalize request parameter is missing");
        }
        if (requestParameters.getContext() != null) {
            try {
                requestParameters.getContext().entrySet().stream().forEach(e -> isValidPersonalizeContext(e));
            } catch (IllegalArgumentException iae) {
                throw ConfigurationUtils.newConfigurationException(PersonalizeRankingResponseProcessor.TYPE, "", "context", iae.getMessage());
            }
        }
    }

    private void isValidPersonalizeContext(Map.Entry<String, Object> contextEntry) throws IllegalArgumentException {
        if (!(contextEntry.getValue() instanceof String)) {
            throw new IllegalArgumentException("Personalize context value is not of type String. Invalid context value: " + contextEntry.getValue());
        }
    }
}
