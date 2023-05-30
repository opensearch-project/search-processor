/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.test.OpenSearchTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonalizeRequestParameterUtilTests extends OpenSearchTestCase {

    public void testExtractParameters() {
        PersonalizeRequestParameters expected = new PersonalizeRequestParameters("user_1", new HashMap<>());
        PersonalizeRequestParametersExtBuilder extBuilder = new PersonalizeRequestParametersExtBuilder();
        extBuilder.setRequestParameters(expected);
        SearchSourceBuilder sourceBuilder = SearchSourceBuilder.searchSource()
                .ext(List.of(extBuilder));
        SearchRequest request = new SearchRequest("my_index").source(sourceBuilder);
        PersonalizeRequestParameters actual = PersonalizeRequestParameterUtil.getPersonalizeRequestParameters(request);
        assertEquals(expected, actual);
    }

    public void testExtractParametersWithContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("contextKey", "contextValue");
        PersonalizeRequestParameters expected = new PersonalizeRequestParameters("user_1", context);
        PersonalizeRequestParametersExtBuilder extBuilder = new PersonalizeRequestParametersExtBuilder();
        extBuilder.setRequestParameters(expected);
        SearchSourceBuilder sourceBuilder = SearchSourceBuilder.searchSource()
                .ext(List.of(extBuilder));
        SearchRequest request = new SearchRequest("my_index").source(sourceBuilder);
        PersonalizeRequestParameters actual = PersonalizeRequestParameterUtil.getPersonalizeRequestParameters(request);
        assertEquals(expected, actual);
    }

    public void testPersonalizeRequestParametersEquals() {
        Map<String, Object> notExpectedContext = new HashMap<>();
        notExpectedContext.put("contextKey", "contextValue");
        PersonalizeRequestParameters notExpected = new PersonalizeRequestParameters("user_1", notExpectedContext);

        Map<String, Object> expectedContext = new HashMap<>();
        expectedContext.put("contextKey2", "contextValue2");
        PersonalizeRequestParameters expected = new PersonalizeRequestParameters("user_1", expectedContext);
        PersonalizeRequestParametersExtBuilder extBuilder = new PersonalizeRequestParametersExtBuilder();
        extBuilder.setRequestParameters(expected);
        SearchSourceBuilder sourceBuilder = SearchSourceBuilder.searchSource()
                .ext(List.of(extBuilder));
        SearchRequest request = new SearchRequest("my_index").source(sourceBuilder);
        PersonalizeRequestParameters actual = PersonalizeRequestParameterUtil.getPersonalizeRequestParameters(request);
        assertNotEquals(notExpected, actual);
    }

    public void testPersonalizeRequestParametersContextMapDifferentSize() {
        Map<String, Object> notExpectedContext = new HashMap<>();
        notExpectedContext.put("contextKey", "contextValue");
        PersonalizeRequestParameters notExpected = new PersonalizeRequestParameters("user_1", notExpectedContext);

        Map<String, Object> expectedContext = new HashMap<>();
        expectedContext.put("contextKey2", "contextValue2");
        expectedContext.put("contextKey22", "contextValue22");
        PersonalizeRequestParameters expected = new PersonalizeRequestParameters("user_1", expectedContext);
        PersonalizeRequestParametersExtBuilder extBuilder = new PersonalizeRequestParametersExtBuilder();
        extBuilder.setRequestParameters(expected);
        SearchSourceBuilder sourceBuilder = SearchSourceBuilder.searchSource()
                .ext(List.of(extBuilder));
        SearchRequest request = new SearchRequest("my_index").source(sourceBuilder);
        PersonalizeRequestParameters actual = PersonalizeRequestParameterUtil.getPersonalizeRequestParameters(request);
        assertNotEquals(notExpected, actual);
    }

    public void testPersonalizeRequestParametersUserIdDiffers() {
        Map<String, Object> notExpectedContext = new HashMap<>();
        notExpectedContext.put("contextKey", "contextValue");
        PersonalizeRequestParameters notExpected = new PersonalizeRequestParameters("user_1", notExpectedContext);

        Map<String, Object> expectedContext = new HashMap<>();
        expectedContext.put("contextKey", "contextValue");
        PersonalizeRequestParameters expected = new PersonalizeRequestParameters("user_2", expectedContext);
        PersonalizeRequestParametersExtBuilder extBuilder = new PersonalizeRequestParametersExtBuilder();
        extBuilder.setRequestParameters(expected);
        SearchSourceBuilder sourceBuilder = SearchSourceBuilder.searchSource()
                .ext(List.of(extBuilder));
        SearchRequest request = new SearchRequest("my_index").source(sourceBuilder);
        PersonalizeRequestParameters actual = PersonalizeRequestParameterUtil.getPersonalizeRequestParameters(request);
        assertNotEquals(notExpected, actual);
    }
}