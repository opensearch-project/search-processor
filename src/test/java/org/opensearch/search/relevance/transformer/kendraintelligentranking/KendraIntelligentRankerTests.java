/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking;

import org.apache.lucene.search.TotalHits;
import org.mockito.Mockito;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.index.query.MatchAllQueryBuilder;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.relevance.configuration.ResultTransformerConfiguration;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraClientSettings;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraHttpClient;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankingConfiguration;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankingConfiguration.KendraIntelligentRankingProperties;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreRequest;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreResult;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreResultItem;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KendraIntelligentRankerTests extends OpenSearchTestCase {
    private static KendraHttpClient buildMockHttpClient(Function<RescoreRequest, RescoreResult> mockRescoreImpl) {
        KendraHttpClient kendraHttpClient = Mockito.mock(KendraHttpClient.class);
        Mockito.when(kendraHttpClient.isValid()).thenReturn(true);
        Mockito.doAnswer(invocation -> {
            RescoreRequest rescoreRequest = invocation.getArgument(0);
            return mockRescoreImpl.apply(rescoreRequest);
        }).when(kendraHttpClient).rescore(Mockito.any(RescoreRequest.class));
        return kendraHttpClient;
    }

    private static KendraHttpClient buildMockHttpClient() {
        return buildMockHttpClient(r -> new RescoreResult());
    }

    public void testGetSettings() {
        List<Setting<?>> settings = new KendraIntelligentRanker(buildMockHttpClient()).getTransformerSettings();
        assertNotNull(settings);
        assertFalse(settings.isEmpty());
    }

    public void testPreprocess() {
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(buildMockHttpClient());
        KendraIntelligentRankingProperties properties =
                new KendraIntelligentRankingProperties(List.of("body"), List.of("title"), 50);
        ResultTransformerConfiguration configuration = new KendraIntelligentRankingConfiguration(1, properties);
        SearchRequest originalRequest = new SearchRequest()
                .source(new SearchSourceBuilder()
                        .fetchSource(false)
                        .from(5)
                        .size(10));
        SearchRequest transformedRequest = ranker.preprocessRequest(originalRequest, configuration);
        assertTrue(transformedRequest.source().fetchSource().fetchSource());
        assertEquals(0, transformedRequest.source().from());
        assertEquals(50, transformedRequest.source().size());
    }

    public void testShouldNotTransformWithoutSource() {
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(buildMockHttpClient());
        SearchRequest originalRequest = new SearchRequest();
        boolean shouldTransform = ranker.shouldTransform(originalRequest, new KendraIntelligentRankingConfiguration());
        assertFalse(shouldTransform);
    }

    public void testShouldNotTransformWithoutQuery() {
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(buildMockHttpClient());
        SearchRequest originalRequest = new SearchRequest()
                .source(new SearchSourceBuilder().query(null));
        boolean shouldTransform = ranker.shouldTransform(originalRequest, new KendraIntelligentRankingConfiguration());
        assertFalse(shouldTransform);
    }

    public void testShouldNotTransformWithScroll() {
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(buildMockHttpClient());
        SearchRequest originalRequest = new SearchRequest()
                .source(new SearchSourceBuilder())
                .scroll("5h");
        boolean shouldTransform = ranker.shouldTransform(originalRequest, new KendraIntelligentRankingConfiguration());
        assertFalse(shouldTransform);
    }

    public void testShouldNotTransformWithSort() {
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(buildMockHttpClient());
        SearchRequest originalRequest = new SearchRequest()
                .source(new SearchSourceBuilder()
                        .sort("foo"));
        boolean shouldTransform = ranker.shouldTransform(originalRequest, new KendraIntelligentRankingConfiguration());
        assertFalse(shouldTransform);
    }

    public void testShouldNotTransformWithInvalidClient() {
        Settings emptySettings = Settings.builder().build();
        KendraHttpClient invalidClient = new KendraHttpClient(KendraClientSettings.getClientSettings(emptySettings));
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(invalidClient);

        // Otherwise valid search request:
        SearchRequest originalRequest = new SearchRequest()
                .source(new SearchSourceBuilder()
                        .query(new MatchAllQueryBuilder()));
        KendraIntelligentRankingProperties properties =
                new KendraIntelligentRankingProperties(List.of("body"), List.of("title"), 10);

        ResultTransformerConfiguration configuration = new KendraIntelligentRankingConfiguration(1, properties);
        boolean shouldTransform = ranker.shouldTransform(originalRequest, configuration);
        assertFalse(shouldTransform);
    }

    public void testShouldNotTransformIfFromExceedsDocLimit() {
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(buildMockHttpClient());
        SearchRequest originalRequest = new SearchRequest()
                .source(new SearchSourceBuilder()
                        .from(20));
        KendraIntelligentRankingProperties properties =
                new KendraIntelligentRankingProperties(List.of("body"), List.of("title"), 10);
        ResultTransformerConfiguration configuration = new KendraIntelligentRankingConfiguration(1, properties);
        boolean shouldTransform = ranker.shouldTransform(originalRequest, configuration);
        assertFalse(shouldTransform);
    }

    public void testShouldTransformTrue() {
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(buildMockHttpClient());
        SearchRequest originalRequest = new SearchRequest()
                .source(new SearchSourceBuilder()
                        .query(new MatchAllQueryBuilder()));
        KendraIntelligentRankingProperties properties =
                new KendraIntelligentRankingProperties(List.of("body"), List.of("title"), 10);
        ResultTransformerConfiguration configuration = new KendraIntelligentRankingConfiguration(1, properties);
        boolean shouldTransform = ranker.shouldTransform(originalRequest, configuration);
        assertTrue(shouldTransform);
    }

    public void testTransformInvalidQueryType() {
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(buildMockHttpClient());
        SearchRequest originalRequest = new SearchRequest()
                .source(new SearchSourceBuilder().query(new MatchAllQueryBuilder()));
        KendraIntelligentRankingProperties properties =
                new KendraIntelligentRankingProperties(List.of("body"), List.of("title"), 10);
        ResultTransformerConfiguration configuration = new KendraIntelligentRankingConfiguration(1, properties);

        SearchHits searchHits = new SearchHits(new SearchHit[0], new TotalHits(0, TotalHits.Relation.EQUAL_TO), 1.0f);

        SearchHits transformedHits = ranker.transform(searchHits, originalRequest, configuration);
        assertSame(searchHits, transformedHits);
    }

    public void testTransformEmptyHits() {
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(buildMockHttpClient());
        SearchRequest originalRequest = new SearchRequest()
                .source(new SearchSourceBuilder().query(new MatchQueryBuilder("body", "foo")));
        KendraIntelligentRankingProperties properties =
                new KendraIntelligentRankingProperties(List.of("body"), List.of("title"), 10);
        ResultTransformerConfiguration configuration = new KendraIntelligentRankingConfiguration(1, properties);

        SearchHits searchHits = new SearchHits(new SearchHit[0], new TotalHits(0, TotalHits.Relation.EQUAL_TO), 1.0f);

        SearchHits transformedHits = ranker.transform(searchHits, originalRequest, configuration);
        assertSame(searchHits, transformedHits);
    }

    public void testTransformHits() throws IOException {
        SearchRequest originalRequest = new SearchRequest()
                .source(new SearchSourceBuilder().query(new MatchQueryBuilder("body", "foo")));

        int docLimit = randomIntBetween(1, 20);
        KendraIntelligentRankingProperties properties =
                new KendraIntelligentRankingProperties(List.of("body"), List.of("title"), docLimit);
        ResultTransformerConfiguration configuration = new KendraIntelligentRankingConfiguration(1, properties);

        int numHits = docLimit + randomInt(20);
        SearchHit[] hitsArray = new SearchHit[numHits];
        for (int i = 0; i < numHits; i++) {
            XContentBuilder sourceContent = JsonXContent.contentBuilder()
                    .startObject()
                    .field("_id", String.valueOf(i))
                    .field("body", "Body text for document number " + i)
                    .field("title", "This is the title for document " + i)
                    .endObject();
            hitsArray[i] = new SearchHit(i, "doc" + i, Map.of(), Map.of());
            hitsArray[i].sourceRef(BytesReference.bytes(sourceContent));
        }
        SearchHits searchHits = new SearchHits(hitsArray, new TotalHits(numHits, TotalHits.Relation.EQUAL_TO), 1.0f);

        AtomicReference<RescoreRequest> rescoreRequestRef = new AtomicReference<>();
        KendraIntelligentRanker ranker = new KendraIntelligentRanker(buildMockHttpClient(req -> {
            rescoreRequestRef.set(req);
            // Return the top N results in reverse order.
            List<RescoreResultItem> resultItems = req.getDocuments().stream()
                            .map(d -> {
                                RescoreResultItem item = new RescoreResultItem();
                                item.setDocumentId(d.getGroupId());
                                item.setScore(randomFloat());
                                return item;
                            }).collect(Collectors.toList());
            Collections.reverse(resultItems);
            RescoreResult result = new RescoreResult();
            result.setResultItems(resultItems);
            return result;
        }));
        SearchHits transformedHits = ranker.transform(searchHits, originalRequest, configuration);

        assertNotSame(searchHits, transformedHits);
        // The top N (according to doc limit) should be in reverse order
        for (int i = 0; i < docLimit; i++) {
            assertEquals("doc" + (docLimit - i - 1), transformedHits.getHits()[i].getId());
        }
        // The remainder should be in the original order
        for (int i = docLimit; i < numHits; i++) {
            assertEquals("doc" + i, transformedHits.getHits()[i].getId());
        }
    }

}