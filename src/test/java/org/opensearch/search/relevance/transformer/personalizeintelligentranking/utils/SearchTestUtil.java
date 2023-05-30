/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils;

import org.apache.lucene.search.TotalHits;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;

import java.io.IOException;
import java.util.Map;

public class SearchTestUtil {
    public static SearchHits getSampleSearchHitsForPersonalize(int numHits) throws IOException {
        SearchHit[] hitsArray = new SearchHit[numHits];
        for (int i = 0; i < numHits; i++) {
            XContentBuilder sourceContent = JsonXContent.contentBuilder()
                    .startObject()
                    .field("_id", String.valueOf(i))
                    .field("ITEM_ID", String.valueOf(i))
                    .field("body", "Body text for document number " + i)
                    .field("title", "This is the title for document " + i)
                    .endObject();
            hitsArray[i] = new SearchHit(i, "doc" + i, Map.of(), Map.of());
            hitsArray[i].score((float) (numHits-i)/10);
            hitsArray[i].sourceRef(BytesReference.bytes(sourceContent));
        }
        SearchHits searchHits = new SearchHits(hitsArray, new TotalHits(numHits, TotalHits.Relation.EQUAL_TO), 1.0f);
        return searchHits;
    }
}
