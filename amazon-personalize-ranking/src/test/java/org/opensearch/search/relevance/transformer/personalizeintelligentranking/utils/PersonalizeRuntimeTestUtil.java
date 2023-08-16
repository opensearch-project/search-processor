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
import org.mockito.Mockito;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;

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

    public static ArrayList<String> expectedRankedItemIdsForGivenWeight(int numOfHits, int weight){
        ArrayList<String> expectedRankedItemIds = new ArrayList<>();
        if (weight == 0) {
            for (int i = 0; i < numOfHits; i++) {
                expectedRankedItemIds.add(String.valueOf(i));
            }
        } else if (weight == 1){
            for(int i = numOfHits; i >= 1; i--){
                expectedRankedItemIds.add(String.valueOf(i-1));
            }
        }
        return expectedRankedItemIds;
    }

    public static PersonalizeClient buildMockPersonalizeClient() {
        return buildMockPersonalizeClient(r -> buildGetPersonalizedRankingResult(10));
    }

    private static PersonalizeClient buildMockPersonalizeClient(
            Function<GetPersonalizedRankingRequest, GetPersonalizedRankingResult> mockGetPersonalizedRankingImpl) {
        PersonalizeClient personalizeClient = Mockito.mock(PersonalizeClient.class);
        Mockito.doAnswer(invocation -> {
          GetPersonalizedRankingRequest request = invocation.getArgument(0);
          return mockGetPersonalizedRankingImpl.apply(request);
        }).when(personalizeClient).getPersonalizedRanking(any(GetPersonalizedRankingRequest.class));
        return personalizeClient;
    }
}
