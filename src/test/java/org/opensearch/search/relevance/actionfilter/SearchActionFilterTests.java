/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.actionfilter;

import org.apache.lucene.search.TotalHits;
import org.mockito.Mockito;
import org.opensearch.action.ActionFuture;
import org.opensearch.action.ActionListener;
import org.opensearch.action.admin.indices.settings.get.GetSettingsAction;
import org.opensearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.opensearch.action.delete.DeleteAction;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteRequestBuilder;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.search.SearchAction;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchRequestBuilder;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.ShardSearchFailure;
import org.opensearch.action.support.ActionFilterChain;
import org.opensearch.client.Client;
import org.opensearch.common.collect.ImmutableOpenMap;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.internal.InternalSearchResponse;
import org.opensearch.search.relevance.client.OpenSearchClient;
import org.opensearch.search.relevance.configuration.ResultTransformerConfiguration;
import org.opensearch.search.relevance.configuration.SearchConfigurationExtBuilder;
import org.opensearch.search.relevance.transformer.ResultTransformer;
import org.opensearch.search.relevance.transformer.ResultTransformerType;
import org.opensearch.tasks.Task;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SearchActionFilterTests extends OpenSearchTestCase {

    public void testIgnoresDelete() {
        Client client = Mockito.mock(Client.class);
        OpenSearchClient openSearchClient = new OpenSearchClient(client);
        SearchActionFilter searchActionFilter = new SearchActionFilter(Collections.emptyMap(), openSearchClient);

        Task task = Mockito.mock(Task.class);
        DeleteRequest deleteRequest = new DeleteRequestBuilder(null, DeleteAction.INSTANCE).request();
        AtomicBoolean proceedCalled = new AtomicBoolean(false);
        ActionFilterChain<DeleteRequest, DeleteResponse> deleteFilterChain =
                (task1, action, request, listener) -> proceedCalled.set(true);
        searchActionFilter.apply(task, DeleteAction.NAME, deleteRequest, null, deleteFilterChain);
        assertTrue(proceedCalled.get());
    }

    public void testIgnoresSearchRequestOnZeroIndices() {
        Client client = Mockito.mock(Client.class);
        OpenSearchClient openSearchClient = new OpenSearchClient(client);
        SearchActionFilter searchActionFilter = new SearchActionFilter(Collections.emptyMap(), openSearchClient);

        Task task = Mockito.mock(Task.class);
        SearchRequest searchRequest = new SearchRequestBuilder(null, SearchAction.INSTANCE).request();
        AtomicBoolean proceedCalled = new AtomicBoolean(false);
        ActionFilterChain<SearchRequest, SearchResponse> searchFilterChain =
                (task1, action, request, listener) -> proceedCalled.set(true);
        searchActionFilter.apply(task, SearchAction.NAME, searchRequest, null, searchFilterChain);
        assertTrue(proceedCalled.get());
    }

    public void testIgnoresSearchRequestOnMultipleIndices() {
        Client client = Mockito.mock(Client.class);
        OpenSearchClient openSearchClient = new OpenSearchClient(client);
        SearchActionFilter searchActionFilter = new SearchActionFilter(Collections.emptyMap(), openSearchClient);

        Task task = Mockito.mock(Task.class);
        SearchRequest searchRequest = new SearchRequestBuilder(null, SearchAction.INSTANCE)
                .setIndices("index1", "index2")
                .request();
        AtomicBoolean proceedCalled = new AtomicBoolean(false);
        ActionFilterChain<SearchRequest, SearchResponse> searchFilterChain =
                (task1, action, request, listener) -> proceedCalled.set(true);
        searchActionFilter.apply(task, SearchAction.NAME, searchRequest, null, searchFilterChain);
        assertTrue(proceedCalled.get());
    }

    private static Client buildMockClient(String indexName, Settings... settings) {
        Client client = Mockito.mock(Client.class);
        ActionFuture<GetSettingsResponse> mockGetSettingsFuture = Mockito.mock(ActionFuture.class);

        Settings.Builder settingsBuilder = Settings.builder();
        for (Settings settingsEntry : settings) {
            settingsBuilder.put(settingsEntry);
        }
        Settings settingsObj = settingsBuilder.build();
        ImmutableOpenMap<String, Settings> indexSettingsMap = ImmutableOpenMap.<String, Settings>builder()
                .fPut(indexName, settingsObj)
                .build();
        ImmutableOpenMap<String, Settings> emptyMap = ImmutableOpenMap.<String, Settings>builder().build();
        GetSettingsResponse getSettingsResponse = new GetSettingsResponse(indexSettingsMap, emptyMap);
        when(mockGetSettingsFuture.actionGet()).thenReturn(getSettingsResponse);
        when(client.execute(eq(GetSettingsAction.INSTANCE), any(GetSettingsRequest.class)))
                .thenReturn(mockGetSettingsFuture);
        return client;
    }

    public void testOperatesOnSingleIndexWithNoTransformers() {
        Client client = buildMockClient("index");
        OpenSearchClient openSearchClient = new OpenSearchClient(client);
        SearchActionFilter searchActionFilter = new SearchActionFilter(Collections.emptyMap(), openSearchClient);

        Task task = Mockito.mock(Task.class);
        SearchRequest searchRequest = new SearchRequestBuilder(null, SearchAction.INSTANCE)
                .setIndices("index")
                .request();
        AtomicBoolean proceedCalled = new AtomicBoolean(false);
        ActionFilterChain<SearchRequest, SearchResponse> searchFilterChain =
                (task1, action, request, listener) -> proceedCalled.set(true);
        searchActionFilter.apply(task, SearchAction.NAME, searchRequest, null, searchFilterChain);
        assertTrue(proceedCalled.get());
    }


    private static class MockTransformer implements ResultTransformer {
        private boolean getTransformerSettingsWasCalled = false;
        private boolean shouldTransformWasCalled = false;
        private boolean transformWasCalled = false;


        @Override
        public List<Setting<?>> getTransformerSettings() {
            getTransformerSettingsWasCalled = true;
            return Collections.emptyList();
        }

        @Override
        public boolean shouldTransform(SearchRequest request, ResultTransformerConfiguration configuration) {
            shouldTransformWasCalled = true;
            return true;
        }

        @Override
        public SearchHits transform(SearchHits hits, SearchRequest request,
                                    ResultTransformerConfiguration configuration) {
            transformWasCalled = true;
            return hits;
        }
    }

    private static final ResultTransformerConfiguration MOCK_TRANSFORMER_CONFIGURATION =
            new ResultTransformerConfiguration() {
                @Override
                public int getOrder() {
                    return 1;
                }

                @Override
                public ResultTransformerType getType() {
                    // For now, the only supported type
                    return ResultTransformerType.KENDRA_INTELLIGENT_RANKING;
                }

                @Override
                public void writeTo(StreamOutput out) throws IOException {
                }

                @Override
                public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                    return null;
                }
            };

    public void testTransformerDoesNotRunWhenNotEnabled() {
        Client client = buildMockClient("index");
        OpenSearchClient openSearchClient = new OpenSearchClient(client);

        MockTransformer mockTransformer = new MockTransformer();

        Map<ResultTransformerType, ResultTransformer> transformerMap =
                Map.of(ResultTransformerType.KENDRA_INTELLIGENT_RANKING, mockTransformer);

        SearchActionFilter searchActionFilter = new SearchActionFilter(transformerMap, openSearchClient);

        Task task = Mockito.mock(Task.class);
        SearchRequest searchRequest = new SearchRequestBuilder(null, SearchAction.INSTANCE)
                .setIndices("index")
                .request();
        AtomicBoolean proceedCalled = new AtomicBoolean(false);
        ActionFilterChain<SearchRequest, SearchResponse> searchFilterChain =
                (task1, action, request, listener) -> proceedCalled.set(true);
        searchActionFilter.apply(task, SearchAction.NAME, searchRequest, null, searchFilterChain);
        assertTrue(proceedCalled.get());
        // We should try to check for index-level settings
        assertTrue(mockTransformer.getTransformerSettingsWasCalled);
        assertFalse(mockTransformer.transformWasCalled);
        assertFalse(mockTransformer.shouldTransformWasCalled);
    }

    public void testTransformEnabledInRequest() {
        Client client = buildMockClient("index");
        OpenSearchClient openSearchClient = new OpenSearchClient(client);

        MockTransformer mockTransformer = new MockTransformer();

        Map<ResultTransformerType, ResultTransformer> transformerMap =
                Map.of(ResultTransformerType.KENDRA_INTELLIGENT_RANKING, mockTransformer);

        SearchActionFilter searchActionFilter = new SearchActionFilter(transformerMap, openSearchClient);

        Task task = Mockito.mock(Task.class);
        SearchRequest searchRequest = new SearchRequestBuilder(null, SearchAction.INSTANCE)
                .setSource(
                        new SearchSourceBuilder()
                                .ext(
                                        Collections.singletonList(new SearchConfigurationExtBuilder()
                                                .setResultTransformers(
                                                        Collections.singletonList(MOCK_TRANSFORMER_CONFIGURATION)
                                                )
                                        )
                                )
                ).setIndices("index")
                .request();
        AtomicBoolean proceedCalled = new AtomicBoolean(false);
        SearchResponse searchResponse = buildMockSearchResponse();

        ActionFilterChain<SearchRequest, SearchResponse> searchFilterChain =
                (task1, action, request, listener) -> {
                    proceedCalled.set(true);
                    listener.onResponse(searchResponse);
                };
        AtomicBoolean onResponseCalled = new AtomicBoolean(false);
        AtomicBoolean onFailureCalled = new AtomicBoolean(false);
        ActionListener<SearchResponse> downstreamListener = new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                onResponseCalled.set(true);
            }

            @Override
            public void onFailure(Exception e) {
                onFailureCalled.set(true);
            }
        };
        searchActionFilter.apply(task, SearchAction.NAME, searchRequest, downstreamListener, searchFilterChain);
        assertTrue(proceedCalled.get());
        // We should NOT try to check for index-level settings, because we saw request-level settings
        assertFalse(mockTransformer.getTransformerSettingsWasCalled);
        assertTrue(mockTransformer.transformWasCalled);
        assertTrue(mockTransformer.shouldTransformWasCalled);
        assertTrue(onResponseCalled.get());
        assertFalse(onFailureCalled.get());
    }

    private static SearchResponse buildMockSearchResponse() {
        return new SearchResponse(new InternalSearchResponse(
                new SearchHits(new SearchHit[0], new TotalHits(100, TotalHits.Relation.EQUAL_TO), 1.0f),
                null, null, null, false, false, 1
        ), null, 1, 1, 0, 0, new ShardSearchFailure[0],
                new SearchResponse.Clusters(1, 1, 0));
    }

    public void testTransformEnabledByIndexSetting() {
        String prefix = "index.plugin.searchrelevance.result_transformer." +
                ResultTransformerType.KENDRA_INTELLIGENT_RANKING;
        Settings enablePluginSettings = Settings.builder()
                .put(prefix + ".order", 1)
                .build();
        Client client = buildMockClient("index", enablePluginSettings);
        OpenSearchClient openSearchClient = new OpenSearchClient(client);

        MockTransformer mockTransformer = new MockTransformer();

        Map<ResultTransformerType, ResultTransformer> transformerMap =
                Map.of(ResultTransformerType.KENDRA_INTELLIGENT_RANKING, mockTransformer);

        SearchActionFilter searchActionFilter = new SearchActionFilter(transformerMap, openSearchClient);

        Task task = Mockito.mock(Task.class);
        SearchRequest searchRequest = new SearchRequestBuilder(null, SearchAction.INSTANCE)
                .setIndices("index")
                .request();
        AtomicBoolean proceedCalled = new AtomicBoolean(false);
        SearchResponse searchResponse = buildMockSearchResponse();

        ActionFilterChain<SearchRequest, SearchResponse> searchFilterChain =
                (task1, action, request, listener) -> {
                    proceedCalled.set(true);
                    listener.onResponse(searchResponse);
                };
        AtomicBoolean onResponseCalled = new AtomicBoolean(false);
        AtomicBoolean onFailureCalled = new AtomicBoolean(false);
        ActionListener<SearchResponse> downstreamListener = new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                onResponseCalled.set(true);
            }

            @Override
            public void onFailure(Exception e) {
                onFailureCalled.set(true);
            }
        };
        searchActionFilter.apply(task, SearchAction.NAME, searchRequest, downstreamListener, searchFilterChain);
        assertTrue(proceedCalled.get());
        // We should NOT try to check for index-level settings, because we saw request-level settings
        assertTrue(mockTransformer.getTransformerSettingsWasCalled);
        assertTrue(mockTransformer.transformWasCalled);
        assertTrue(mockTransformer.shouldTransformWasCalled);
        assertTrue(onResponseCalled.get());
        assertFalse(onFailureCalled.get());
    }
}