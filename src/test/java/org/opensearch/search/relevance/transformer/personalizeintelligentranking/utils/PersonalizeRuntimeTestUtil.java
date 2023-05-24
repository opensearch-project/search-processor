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
}
