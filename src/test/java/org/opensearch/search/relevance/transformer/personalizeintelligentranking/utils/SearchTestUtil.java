/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils;

import org.apache.lucene.search.TotalHits;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.PersonalizeRequestParameters;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.PersonalizeRequestParametersExtBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SearchTestUtil {
    public static SearchHits getSampleSearchHitsForPersonalize(int numHits) throws IOException {
        SearchHit[] hitsArray = new SearchHit[numHits];
        float maxScore = 0.0f;
        for (int i = 0; i < numHits; i++) {
            XContentBuilder sourceContent = JsonXContent.contentBuilder()
                    .startObject()
                    .field("ITEM_ID", String.valueOf(i))
                    .field("body", "Body text for document number " + i)
                    .field("title", "This is the title for document " + i)
                    .endObject();
            hitsArray[i] = new SearchHit(i, String.valueOf(i), Map.of(), Map.of());
            float score = (float)(numHits-i)/10;
            maxScore = Math.max(score, maxScore);
            hitsArray[i].score(score);
            hitsArray[i].sourceRef(BytesReference.bytes(sourceContent));
        }
        SearchHits searchHits = new SearchHits(hitsArray, new TotalHits(numHits, TotalHits.Relation.EQUAL_TO), maxScore);
        return searchHits;
    }

    public static SearchRequest createSearchRequestWithPersonalizeRequest(PersonalizeRequestParameters personalizeRequestParams) {
        PersonalizeRequestParametersExtBuilder extBuilder = new PersonalizeRequestParametersExtBuilder();
        extBuilder.setRequestParameters(personalizeRequestParams);
        SearchSourceBuilder sourceBuilder = SearchSourceBuilder.searchSource()
                .ext(List.of(extBuilder));

        SearchRequest searchRequest = new SearchRequest().source(sourceBuilder);
        return searchRequest;
    }
}
