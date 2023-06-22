/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.impl;

import com.amazonaws.services.personalizeruntime.model.GetPersonalizedRankingRequest;
import com.amazonaws.services.personalizeruntime.model.GetPersonalizedRankingResult;
import com.amazonaws.services.personalizeruntime.model.PredictedItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.TotalHits;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClient;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.PersonalizeIntelligentRankerConfiguration;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.PersonalizeRequestParameters;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.PersonalizedRanker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Personalize Re Ranker implementation using Amazon Personalized Ranking recipe
 */
public class AmazonPersonalizedRankerImpl implements PersonalizedRanker {
    private static final Logger logger = LogManager.getLogger(AmazonPersonalizedRankerImpl.class);
    private final PersonalizeIntelligentRankerConfiguration rankerConfig;
    private final PersonalizeClient personalizeClient;
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
            if (!isValidPersonalizeConfigPresent(requestParameters)) {
                throw new IllegalArgumentException("Required configurations missing from Personalize " +
                        "response processor configuration or search request parameters");
            }
            List<SearchHit> originalHits = Arrays.asList(hits.getHits());
            String itemIdfield = rankerConfig.getItemIdField();
            List<String> documentIdsToRank;
            // If item field is not specified in the configuration then use default _id field.
            if (!itemIdfield.isEmpty()) {
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
            logger.info("Document Ids to re-rank with Personalize: {}", Arrays.toString(documentIdsToRank.toArray()));
            String userId = requestParameters.getUserId();
            Map<String, String> context = requestParameters.getContext() != null ?
                                            requestParameters.getContext().entrySet().stream()
                                                    .collect(Collectors.toMap(Map.Entry::getKey, e -> isValidPersonalizeContext(e)))
                                            : null;
            logger.info("User ID from request parameters. User ID: {}", userId);
            if (context != null && !context.isEmpty()) {
                logger.info("Personalize context provided in the search request");
            }

            GetPersonalizedRankingRequest personalizeRequest = new GetPersonalizedRankingRequest()
                    .withCampaignArn(rankerConfig.getPersonalizeCampaign())
                    .withInputList(documentIdsToRank)
                    .withContext(context)
                    .withUserId(userId);
            GetPersonalizedRankingResult result = personalizeClient.getPersonalizedRanking(personalizeRequest);

            List<PredictedItem> personalizeRrankingResult = result.getPersonalizedRanking();
            Map<String, Float> idToPersonalizeRankingScoreMap = new HashMap<>();
            Map<String, Float> idToOpenSearchScoreMap = new HashMap<>();
            Map<String, SearchHit> itemIdToSearchHitMap = new HashMap<>();
            // Build a map with key as item id and value as personalize ranking score
            for (PredictedItem item : personalizeRrankingResult) {
                idToPersonalizeRankingScoreMap.put(item.getItemId(), item.getScore().floatValue());
            }

            // Build a map with key as item id and value as open search scores and another map
            // with key as item id and value as corresponding search hit
            for (SearchHit hit : originalHits) {
                if (!itemIdfield.isEmpty()){
                    idToOpenSearchScoreMap.put(hit.getSourceAsMap().get(itemIdfield).toString(), hit.getScore());
                    itemIdToSearchHitMap.put(hit.getSourceAsMap().get(itemIdfield).toString(), hit);
                }
                else{
                    idToOpenSearchScoreMap.put(hit.getId(), hit.getScore());
                    itemIdToSearchHitMap.put(hit.getId(), hit);
                }
            }


            float weight = (float) rankerConfig.getWeight();
            SearchHits newHits = combineScores(idToPersonalizeRankingScoreMap, idToOpenSearchScoreMap,
                    itemIdToSearchHitMap, hits.getTotalHits(), weight);
            return newHits;
        } catch (Exception ex) {
            logger.error("Failed to re rank with Personalize. Returning original search results without Personalize re ranking.", ex);
            return hits;
        }
    }

    //Combine open search hits and personalize campaign response
    public SearchHits combineScores(Map<String, Float> idToPersonalizeRankingScoreMap,
                                     Map<String, Float> idToOpenSearchScoreMap,
                                     Map<String, SearchHit> itemIdToSearchHitMap,
                                     TotalHits totalHits, float weight) {
        //Update open search score based on the personalize campaign response for each item id
        List<String> openSearchItemId = new ArrayList<String>(idToOpenSearchScoreMap.keySet());
        for (String itemId : openSearchItemId) {
            if(idToPersonalizeRankingScoreMap.containsKey(itemId)){
                float personalizedScore = idToPersonalizeRankingScoreMap.get(itemId);
                float openSearchScore = idToOpenSearchScoreMap.get(itemId);
                float combinedScore = (float) (weight / Math.log(openSearchScore + 1)
                        + (1 - weight) / Math.log(personalizedScore + 1));
                idToOpenSearchScoreMap.put(itemId, combinedScore);
            }
        }

        //Create a new list of search hits in the decreasing order of the combined scores
        Map<String, Float> sortedScores = sortByValue(idToOpenSearchScoreMap);

        List<SearchHit> rerankedHits = sortedScores.keySet().stream()
                .map(itemId -> {
                    SearchHit hit = itemIdToSearchHitMap.get(itemId);
                    hit.score(sortedScores.get(itemId));
                    return hit;
                })
                .collect(Collectors.toList());
        float maxScore = sortedScores.values().stream().max(Float::compare).orElse(0f);
        return new SearchHits(rerankedHits.toArray(new SearchHit[0]), totalHits, maxScore);
    }


    //Sort map by reverse order of the values
    public Map<String, Float> sortByValue(Map<String, Float> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }


    /**
     * Validate Personalize configuration for calling Personalize service
     * @param requestParameters Request parameters for Personalize present in search request
     * @return True if valid configuration present else false.
     */
    public boolean isValidPersonalizeConfigPresent(PersonalizeRequestParameters requestParameters) {
        boolean isValidPersonalizeConfig = true;

        if (requestParameters == null || requestParameters.getUserId().isEmpty()) {
            isValidPersonalizeConfig = false;
            logger.error("Required Personalize parameters are not provided in the search request");
        }

        if (rankerConfig == null || rankerConfig.getPersonalizeCampaign().isEmpty() ||
                rankerConfig.getWeight() < 0.0 || rankerConfig.getWeight() > 1.0) {
            isValidPersonalizeConfig = false;
            logger.error("Required Personalized ranker configuration is missing");
        }
        return isValidPersonalizeConfig;
    }

    private String isValidPersonalizeContext(Map.Entry<String, Object> contextEntry) throws IllegalArgumentException {
        if (contextEntry.getValue() instanceof String) {
            return (String) contextEntry.getValue();
        } else {
            throw new IllegalArgumentException("Personalize context value is not of type String. " +
                    "Invalid context value: " + contextEntry.getValue());
        }
    }
}
