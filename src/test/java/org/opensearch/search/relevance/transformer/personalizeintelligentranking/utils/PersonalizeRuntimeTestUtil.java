/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils;

import com.amazonaws.services.personalizeruntime.model.GetPersonalizedRankingRequest;
import com.amazonaws.services.personalizeruntime.model.GetPersonalizedRankingResult;
import com.amazonaws.services.personalizeruntime.model.PredictedItem;

import java.util.ArrayList;
import java.util.List;

public class PersonalizeRuntimeTestUtil {

    public static GetPersonalizedRankingRequest buildGetPersonalizedRankingRequest() {
        GetPersonalizedRankingRequest request = new GetPersonalizedRankingRequest()
                .withUserId("sampleUserId")
                .withInputList(new ArrayList<String>())
                .withCampaignArn("sampleCampaign");
        return request;
    }

    public static GetPersonalizedRankingResult buildGetPersonalizedRankingResult() {
        List<PredictedItem> predictedItems = new ArrayList<>();
        GetPersonalizedRankingResult result = new GetPersonalizedRankingResult()
                .withPersonalizedRanking(predictedItems)
                .withRecommendationId("sampleRecommendationId");
        return result;
    }

    public static GetPersonalizedRankingResult buildGetPersonalizedRankingResult(int numOfHits) {
        List<PredictedItem> predictedItems = new ArrayList<>();
        for(int i = numOfHits; i >= 1; i--){
            PredictedItem predictedItem = new PredictedItem().
                    withScore((double) i/10).
                    withItemId(String.valueOf(i-1));
            predictedItems.add(predictedItem);
        }
        GetPersonalizedRankingResult result = new GetPersonalizedRankingResult()
                .withPersonalizedRanking(predictedItems)
                .withRecommendationId("sampleRecommendationId");
        return result;
    }


    public static GetPersonalizedRankingResult buildGetPersonalizedRankingResultWhenItemIdConfigIsEmpty(int numOfHits) {
        List<PredictedItem> predictedItems = new ArrayList<>();
        for(int i = numOfHits; i >= 1; i--){
            PredictedItem predictedItem = new PredictedItem().
                    withScore((double) i/10).
                    withItemId("doc"+ (i - 1));
            predictedItems.add(predictedItem);
        }

        GetPersonalizedRankingResult result = new GetPersonalizedRankingResult()
                .withPersonalizedRanking(predictedItems)
                .withRecommendationId("sampleRecommendationId");
        return result;
    }

    public static ArrayList<String> expectedRankedItemIdsWhenWeightIsOne(int numOfHits){
        ArrayList<String> expectedRankedItemIds = new ArrayList<>();
        for(int i = numOfHits; i >= 1; i--){
            expectedRankedItemIds.add(String.valueOf(i-1));
        }
        return expectedRankedItemIds;
    }

    public static ArrayList<String> expectedRankedItemIdsWhenWeightIsZero(int numOfHits){
        ArrayList<String> expectedRankedItemIds = new ArrayList<>();
        for(int i = 0; i <numOfHits; i++){
            expectedRankedItemIds.add(String.valueOf(i));
        }
        return expectedRankedItemIds;
    }

    public static ArrayList<String> expectedRankedItemIdsWhenWeightIsOneWithNullItemIdField(int numOfHits){
        ArrayList<String> expectedRankedItemIds = new ArrayList<>();
        for(int i = numOfHits; i >= 1; i--){
            expectedRankedItemIds.add("doc" + (i - 1));
        }
        return expectedRankedItemIds;
    }

    public static ArrayList<String> expectedRankedItemIdsWhenWeightIsZeroWithNullItemIdField(int numOfHits){
        ArrayList<String> expectedRankedItemIds = new ArrayList<>();
        for(int i = 0; i <numOfHits; i++){
            expectedRankedItemIds.add("doc" + i);
        }
        return expectedRankedItemIds;
    }
}
