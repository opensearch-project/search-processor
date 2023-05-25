/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.pipeline;

import org.apache.lucene.search.TotalHits;
import org.opensearch.OpenSearchParseException;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchResponseSections;
import org.opensearch.common.bytes.BytesArray;
import org.opensearch.common.document.DocumentField;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.env.TestEnvironment;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraClientSettings;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraHttpClient;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraIntelligentClientTests;

import java.util.*;


public class KendraRankingResponseProcessorTests extends KendraIntelligentClientTests {
    private static final String TYPE = "kendra_ranking";
    private Settings settings = buildEnvSettings(Settings.EMPTY);
    private Environment env = TestEnvironment.newEnvironment(settings);

    private KendraClientSettings clientSettings = KendraClientSettings.getClientSettings(env.settings());

    private SearchRequest createRequest() {
        QueryBuilder query = new MatchQueryBuilder("body", "value");
        SearchSourceBuilder source = new SearchSourceBuilder().query(query);
        return new SearchRequest().source(source);
    }

    private SearchResponse createResponse(int size) {
        SearchHit[] hits = new SearchHit[size];
        for (int i = 0; i < size; i++) {
            Map<String, DocumentField> searchHitFields = new HashMap<>();
            searchHitFields.put("field", new DocumentField("value" + i, Collections.emptyList()));
            searchHitFields.put("body", new DocumentField("body" + i, Collections.emptyList()));
            hits[i] = new SearchHit(i, "doc " + i, searchHitFields, Collections.emptyMap());
            hits[i].sourceRef(new BytesArray("{ \"field "+ "\" : \"value"+ i + "\" ,\"body"+ "\" : \"body"+ i + "\" }"));
            hits[i].score(i);
        }
        SearchHits searchHits = new SearchHits(hits, new TotalHits(size * 2L, TotalHits.Relation.EQUAL_TO), size);
        SearchResponseSections searchResponseSections = new SearchResponseSections(searchHits, null, null, false, false, null, 0);
        return new SearchResponse(searchResponseSections, null, 1, 1, 0, 10, null, null);
    }

    public void testFactory() throws Exception {

        KendraRankingResponseProcessor.Factory factory = new KendraRankingResponseProcessor.Factory( this.clientSettings);

        //test create without title field, expect exceptions
        expectThrows(OpenSearchParseException.class, () -> factory.create(
                Collections.emptyMap(),
                null,
                null,
                Collections.emptyMap()
        ));

        //test create with all fields
        List<String> titleField= new ArrayList<>();
        titleField.add("field");
        Map<String,Object> configuration = new HashMap<>();
        configuration.put("title_field","field");
        configuration.put("body_field","body");
        configuration.put("doc_limit","500");
        KendraRankingResponseProcessor processorWithAllFields = factory.create(Collections.emptyMap(),"tmp0","testingAllFields", configuration);
        assertEquals(TYPE, processorWithAllFields.getType());
        assertEquals("tmp0", processorWithAllFields.getTag());
        assertEquals("testingAllFields", processorWithAllFields.getDescription());

        //test create with required field
        Map<String,Object> shortConfiguration = new HashMap<>();
        shortConfiguration.put("body_field","body");
        KendraRankingResponseProcessor processorWithOneFields = factory.create(Collections.emptyMap(),"tmp1","testingBodyField", shortConfiguration);
        assertEquals(TYPE, processorWithOneFields.getType());
        assertEquals("tmp1", processorWithOneFields.getTag());
        assertEquals("testingBodyField", processorWithOneFields.getDescription());

        //test create with null doc_limit field
        Map<String,Object> nullDocLimitConfiguration = new HashMap<>();
        nullDocLimitConfiguration.put("body_field","body");
        nullDocLimitConfiguration.put("doc_limit",null);
        KendraRankingResponseProcessor processorWithNullDocLimit = factory.create(Collections.emptyMap(),"tmp2","testingNullDocLimit", nullDocLimitConfiguration);
        assertEquals(TYPE, processorWithNullDocLimit.getType());
        assertEquals("tmp2", processorWithNullDocLimit.getTag());
        assertEquals("testingNullDocLimit", processorWithNullDocLimit.getDescription());

        //test create with null title field
        Map<String,Object> nullTitleConfiguration = new HashMap<>();
        nullTitleConfiguration.put("body_field","body");
        nullTitleConfiguration.put("title_field",null);
        KendraRankingResponseProcessor processorWithNullTitleField = factory.create(Collections.emptyMap(),"tmp3","testingNullTitleField", nullTitleConfiguration);
        assertEquals(TYPE, processorWithNullTitleField.getType());
        assertEquals("tmp3", processorWithNullTitleField.getTag());
        assertEquals("testingNullTitleField", processorWithNullTitleField.getDescription());

    }
    public void testRankingResponse() throws Exception {
        KendraHttpClient kendraClient = buildMockHttpClient();
        List<String> titleField = new ArrayList<>();
        titleField.add("field");
        List<String> bodyField = new ArrayList<>();
        bodyField.add("body");

        //test response with titleField, bodyField and docLimit
        KendraRankingResponseProcessor processorWtOptionalConfig = new KendraRankingResponseProcessor(null,null,titleField,bodyField,500,kendraClient);
        int size = 5;
        SearchResponse reRankedResponse0 = processorWtOptionalConfig.processResponse(createRequest(),createResponse(size));
        assertEquals(size,reRankedResponse0.getHits().getHits().length);

        //test response with null doc limit
        KendraRankingResponseProcessor processorWtTwoConfig = new KendraRankingResponseProcessor(null,null,titleField,bodyField,null,kendraClient);
        SearchResponse reRankedResponse1 = processorWtTwoConfig.processResponse(createRequest(),createResponse(size));
        assertEquals(size,reRankedResponse1.getHits().getHits().length);

        //test response with null doc limit and null title field
        KendraRankingResponseProcessor processorWtOneConfig = new KendraRankingResponseProcessor(null,null,null,bodyField,null,kendraClient);
        SearchResponse reRankedResponse2 = processorWtOneConfig.processResponse(createRequest(),createResponse(size));
        assertEquals(size,reRankedResponse2.getHits().getHits().length);

    }
}
