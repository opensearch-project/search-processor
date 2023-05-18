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
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClient;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.PersonalizeIntelligentRankerConfiguration;
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
     * @param hits  search hits returned by open search
     * @return search hots re ranked using Amazon Personalize
     */
    @Override
    public SearchHits rerank(SearchHits hits) {
        try {
            List<SearchHit> originalHits = Arrays.asList(hits.getHits());
            String itemIdfield = rankerConfig.getItemIdField();
            List<String> documentIdsToRank;
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
            //TODO: Parse user id from request
            String userId = "28";
            GetPersonalizedRankingRequest personalizeRequest = new GetPersonalizedRankingRequest()
                    .withCampaignArn(rankerConfig.getPersonalizeCampaign())
                    .withInputList(documentIdsToRank)
                    .withUserId(userId);
            GetPersonalizedRankingResult result = personalizeClient.getPersonalizedRanking(personalizeRequest);

            List<PredictedItem> rankedItems = result.getPersonalizedRanking();

            //TODO: Combine Personalize and open search result. Change the result after transform logic is implemented
            return hits;
        } catch (Exception ex) {
            logger.error("Failed to re rank with Personalize. Returning original search results without Personalize re ranking.", ex);
            return hits;
        }
    }
}
