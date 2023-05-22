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
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.Constants;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClient;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.PersonalizeIntelligentRankerConfiguration;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.PersonalizeRequestParameters;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.PersonalizedRanker;

import java.util.Arrays;
import java.util.List;
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
            // If item field is not specified in the configruation then use default _id field.
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
            // TODO: Parse context from request parameters
            String userId = requestParameters.getUserId();
            logger.info("User ID from request parameters. User ID: {}", userId);
            GetPersonalizedRankingRequest personalizeRequest = new GetPersonalizedRankingRequest()
                    .withCampaignArn(rankerConfig.getPersonalizeCampaign())
                    .withInputList(documentIdsToRank)
                    .withUserId(userId);
            GetPersonalizedRankingResult result = personalizeClient.getPersonalizedRanking(personalizeRequest);

            //TODO: Combine Personalize and open search result. Change the result after transform logic is implemented
            return hits;
        } catch (Exception ex) {
            logger.error("Failed to re rank with Personalize. Returning original search results without Personalize re ranking.", ex);
            return hits;
        }
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
}
