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
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.collect.ImmutableOpenMap;
import org.opensearch.common.document.DocumentField;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.internal.InternalSearchResponse;
import org.opensearch.search.relevance.client.OpenSearchClient;
import org.opensearch.search.relevance.configuration.ResultTransformerConfiguration;
import org.opensearch.search.relevance.configuration.ResultTransformerConfigurationFactory;
import org.opensearch.search.relevance.configuration.SearchConfigurationExtBuilder;
import org.opensearch.search.relevance.transformer.ResultTransformer;
import org.opensearch.tasks.Task;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SearchActionFilterTests extends OpenSearchTestCase {

    /**
     * This filter only operates on search requests. Other request types (e.g. Delete) will still pass through.
     */
    public void testIgnoresDelete() {
        Client client = Mockito.mock(Client.class);
        OpenSearchClient openSearchClient = new OpenSearchClient(client);
        SearchActionFilter searchActionFilter = new SearchActionFilter(Collections.emptyList(), openSearchClient);

        Task task = Mockito.mock(Task.class);
        DeleteRequest deleteRequest = new DeleteRequestBuilder(null, DeleteAction.INSTANCE).request();
        AtomicBoolean proceedCalled = new AtomicBoolean(false);
        ActionFilterChain<DeleteRequest, DeleteResponse> deleteFilterChain =
                (task1, action, request, listener) -> proceedCalled.set(true);
        searchActionFilter.apply(task, DeleteAction.NAME, deleteRequest, null, deleteFilterChain);
        assertTrue(proceedCalled.get());
    }

    /**
     * Test short-circuit code path where we skip the filter if no index is specified.
     */
    public void testIgnoresSearchRequestOnZeroIndices() {
        Client client = Mockito.mock(Client.class);
        OpenSearchClient openSearchClient = new OpenSearchClient(client);
        SearchActionFilter searchActionFilter = new SearchActionFilter(Collections.emptyList(), openSearchClient);

        Task task = Mockito.mock(Task.class);
        SearchRequest searchRequest = new SearchRequestBuilder(null, SearchAction.INSTANCE).request();
        AtomicBoolean proceedCalled = new AtomicBoolean(false);
        ActionFilterChain<SearchRequest, SearchResponse> searchFilterChain =
                (task1, action, request, listener) -> proceedCalled.set(true);
        searchActionFilter.apply(task, SearchAction.NAME, searchRequest, null, searchFilterChain);
        assertTrue(proceedCalled.get());
    }

    /**
     * Test short-circuit code path where we skip the filter if multiple indices are specified.
     */
    public void testIgnoresSearchRequestOnMultipleIndices() {
        Client client = Mockito.mock(Client.class);
        OpenSearchClient openSearchClient = new OpenSearchClient(client);
        SearchActionFilter searchActionFilter = new SearchActionFilter(Collections.emptyList(), openSearchClient);

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

    /**
     * Probe the code path where we have one index, but no transformers.
     */
    public void testOperatesOnSingleIndexWithNoTransformers() {
        Client client = buildMockClient("index");
        OpenSearchClient openSearchClient = new OpenSearchClient(client);
        SearchActionFilter searchActionFilter = new SearchActionFilter(Collections.emptyList(), openSearchClient);

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

        public static final String NAME = "mock_transformer";

        public MockTransformer() {
            requestTransformer = i -> {};
        }

        public MockTransformer(Consumer<SearchRequest> requestTransformer) {
            this.requestTransformer = requestTransformer;
        }

        private final Consumer<SearchRequest> requestTransformer;
        private boolean getTransformerSettingsWasCalled = false;
        private boolean shouldTransformWasCalled = false;
        private boolean transformWasCalled = false;
        private boolean preproccessRequestWasCalled = false;

        @Override
        public List<Setting<?>> getTransformerSettings() {
            getTransformerSettingsWasCalled = true;
            return Collections.emptyList();
        }

        @Override
        public ResultTransformerConfigurationFactory getConfigurationFactory() {
            return MOCK_CONFIGURATION_FACTORY;
        }


        @Override
        public boolean shouldTransform(SearchRequest request, ResultTransformerConfiguration configuration) {
            shouldTransformWasCalled = true;
            return true;
        }

        @Override
        public SearchRequest preprocessRequest(SearchRequest request, ResultTransformerConfiguration configuration) {
            preproccessRequestWasCalled = true;
            return request;
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
                public String getTransformerName() {
                    return MockTransformer.NAME;
                }

                @Override
                public void writeTo(StreamOutput out) throws IOException {
                }

                @Override
                public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                    return null;
                }
            };

    private static ResultTransformerConfigurationFactory MOCK_CONFIGURATION_FACTORY = new ResultTransformerConfigurationFactory() {
        @Override
        public String getName() {
            return MockTransformer.NAME;
        }

        @Override
        public ResultTransformerConfiguration configure(Settings indexSettings) {
            return MOCK_TRANSFORMER_CONFIGURATION;
        }

        @Override
        public ResultTransformerConfiguration configure(XContentParser parser) {
            return MOCK_TRANSFORMER_CONFIGURATION;
        }

        @Override
        public ResultTransformerConfiguration configure(StreamInput streamInput) {
            return MOCK_TRANSFORMER_CONFIGURATION;
        }
    };

    /**
     * Even if a transformer is wired into the SearchActionFilter, if it's not enabled by search request or
     * index setting, the transformer will not be called.
     */
    public void testTransformerDoesNotRunWhenNotEnabled() {
        Client client = buildMockClient("index");
        OpenSearchClient openSearchClient = new OpenSearchClient(client);

        MockTransformer mockTransformer = new MockTransformer();

        SearchActionFilter searchActionFilter = new SearchActionFilter(List.of(mockTransformer), openSearchClient);

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
        assertFalse(mockTransformer.preproccessRequestWasCalled);
        assertFalse(mockTransformer.transformWasCalled);
        assertFalse(mockTransformer.shouldTransformWasCalled);
    }

    /**
     * Should be able to enable transformer explicitly in a search request.
     */
    public void testTransformEnabledInRequest() throws IOException {
        Client client = buildMockClient("index");
        OpenSearchClient openSearchClient = new OpenSearchClient(client);

        MockTransformer mockTransformer = new MockTransformer();

        SearchActionFilter searchActionFilter = new SearchActionFilter(List.of(mockTransformer), openSearchClient);

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
        SearchResponse searchResponse = buildMockSearchResponse(randomInt(20));

        ActionFilterChain<SearchRequest, SearchResponse> searchFilterChain =
                (task1, action, request, listener) -> {
                    proceedCalled.set(true);
                    listener.onResponse(searchResponse);
                };
        AtomicBoolean onResponseCalled = new AtomicBoolean(false);
        AtomicBoolean onFailureCalled = new AtomicBoolean(false);
        ActionListener<SearchResponse> downstreamListener = new ActionListener<>() {
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
        assertTrue(mockTransformer.preproccessRequestWasCalled);
        assertTrue(mockTransformer.transformWasCalled);
        assertTrue(mockTransformer.shouldTransformWasCalled);
        assertTrue(onResponseCalled.get());
        assertFalse(onFailureCalled.get());
    }

    private static SearchResponse buildMockSearchResponse(int numHits) throws IOException {
        SearchHit[] hitsArray = new SearchHit[numHits];
        for (int i = 0; i < numHits; i++) {
            XContentBuilder sourceContent = JsonXContent.contentBuilder()
                    .startObject()
                    .field("_id", String.valueOf(i))
                    .field("title", "doc" + i)
                    .endObject();
            hitsArray[i] = new SearchHit(i, String.valueOf(i),
                    Map.of("title", new DocumentField("title", List.of("doc" + i))), Map.of());
            hitsArray[i].sourceRef(BytesReference.bytes(sourceContent));
        }

        return new SearchResponse(new InternalSearchResponse(
                new SearchHits(hitsArray, new TotalHits(100, TotalHits.Relation.EQUAL_TO), 1.0f),
                null, null, null, false, false, 1
        ), null, 1, 1, 0, 0, new ShardSearchFailure[0],
                new SearchResponse.Clusters(1, 1, 0));
    }

    /**
     * Should be able to enable transformer on all queries via index setting.
     */
    public void testTransformEnabledByIndexSetting() throws IOException {
        String prefix = "index.plugin.searchrelevance.result_transformer." +
                MockTransformer.NAME;
        Settings enablePluginSettings = Settings.builder()
                .put(prefix + ".order", 1)
                .build();
        Client client = buildMockClient("index", enablePluginSettings);
        OpenSearchClient openSearchClient = new OpenSearchClient(client);

        MockTransformer mockTransformer = new MockTransformer();

        SearchActionFilter searchActionFilter = new SearchActionFilter(List.of(mockTransformer), openSearchClient);

        Task task = Mockito.mock(Task.class);
        SearchRequest searchRequest = new SearchRequestBuilder(null, SearchAction.INSTANCE)
                .setIndices("index")
                .request();
        AtomicBoolean proceedCalled = new AtomicBoolean(false);
        SearchResponse searchResponse = buildMockSearchResponse(randomInt(20));

        ActionFilterChain<SearchRequest, SearchResponse> searchFilterChain =
                (task1, action, request, listener) -> {
                    proceedCalled.set(true);
                    listener.onResponse(searchResponse);
                };
        AtomicBoolean onResponseCalled = new AtomicBoolean(false);
        AtomicBoolean onFailureCalled = new AtomicBoolean(false);
        ActionListener<SearchResponse> downstreamListener = new ActionListener<>() {
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
        assertTrue(mockTransformer.preproccessRequestWasCalled);
        assertTrue(mockTransformer.transformWasCalled);
        assertTrue(mockTransformer.shouldTransformWasCalled);
        assertTrue(onResponseCalled.get());
        assertFalse(onFailureCalled.get());
    }

    /**
     * Verify that even if the transformer overrides source, from, and fetchSource, the original values get applied
     * in the end.
     */
    public void testOutputUsesOriginalSourceParameters() throws IOException {
        Client client = buildMockClient("index");
        OpenSearchClient openSearchClient = new OpenSearchClient(client);

        MockTransformer mockTransformer = new MockTransformer(request -> {
            // Modify the request to always fetch source + request results 0-50
            request.source()
                    .from(0)
                    .size(50)
                    .fetchSource(true);
        });

        SearchActionFilter searchActionFilter = new SearchActionFilter(List.of(mockTransformer), openSearchClient);

        Task task = Mockito.mock(Task.class);
        SearchRequest searchRequest = new SearchRequestBuilder(null, SearchAction.INSTANCE)
                .setSource(
                        new SearchSourceBuilder()
                                .from(10)
                                .size(10)
                                .fetchSource(false)
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
        SearchResponse searchResponse = buildMockSearchResponse(50);

        ActionFilterChain<SearchRequest, SearchResponse> searchFilterChain =
                (task1, action, request, listener) -> {
                    proceedCalled.set(true);
                    listener.onResponse(searchResponse);
                };
        AtomicBoolean onResponseCalled = new AtomicBoolean(false);
        AtomicBoolean onFailureCalled = new AtomicBoolean(false);
        AtomicReference<SearchResponse> returnedResponse = new AtomicReference<>();
        ActionListener<SearchResponse> downstreamListener = new ActionListener<>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                returnedResponse.set(searchResponse);
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
        assertTrue(mockTransformer.preproccessRequestWasCalled);
        assertTrue(mockTransformer.transformWasCalled);
        assertTrue(mockTransformer.shouldTransformWasCalled);
        assertTrue(onResponseCalled.get());
        assertFalse(onFailureCalled.get());

        assertNotNull(returnedResponse.get());
        SearchResponse response = returnedResponse.get();
        assertEquals(10, response.getHits().getHits().length);
        for (int i = 0; i < 10; i++) {
            assertEquals("doc" + (10 + i), response.getHits().getHits()[i].field("title").getValue());
            assertFalse(response.getHits().getHits()[i].hasSource());
        }
    }

    /**
     * Check that we handle the case where the transformer returns top N, but the "from" starts after that.
     */
    public void testReturnEmptyWhenOriginalFromExceedsHitCount() throws IOException {
        Client client = buildMockClient("index");
        OpenSearchClient openSearchClient = new OpenSearchClient(client);

        MockTransformer mockTransformer = new MockTransformer(request -> {
            // Modify the request to always fetch source + request results 0-50
            request.source()
                    .from(0)
                    .size(50)
                    .fetchSource(true);
        });

        SearchActionFilter searchActionFilter = new SearchActionFilter(List.of(mockTransformer), openSearchClient);

        Task task = Mockito.mock(Task.class);
        SearchRequest searchRequest = new SearchRequestBuilder(null, SearchAction.INSTANCE)
                .setSource(
                        new SearchSourceBuilder()
                                .from(50)
                                .size(10)
                                .fetchSource(false)
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
        SearchResponse searchResponse = buildMockSearchResponse(50);

        ActionFilterChain<SearchRequest, SearchResponse> searchFilterChain =
                (task1, action, request, listener) -> {
                    proceedCalled.set(true);
                    listener.onResponse(searchResponse);
                };
        AtomicBoolean onResponseCalled = new AtomicBoolean(false);
        AtomicBoolean onFailureCalled = new AtomicBoolean(false);
        AtomicReference<SearchResponse> returnedResponse = new AtomicReference<>();
        ActionListener<SearchResponse> downstreamListener = new ActionListener<>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                returnedResponse.set(searchResponse);
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
        assertTrue(mockTransformer.preproccessRequestWasCalled);
        assertTrue(mockTransformer.transformWasCalled);
        assertTrue(mockTransformer.shouldTransformWasCalled);
        assertTrue(onResponseCalled.get());
        assertFalse(onFailureCalled.get());

        assertNotNull(returnedResponse.get());
        SearchResponse response = returnedResponse.get();
        assertEquals(0, response.getHits().getHits().length);
    }
}