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

import java.util.List;

public class PersonalizeRequestParameterUtilTests extends OpenSearchTestCase {

    public void testExtractParameters() {
        PersonalizeRequestParameters expected = new PersonalizeRequestParameters("user_1");
        PersonalizeRequestParametersExtBuilder extBuilder = new PersonalizeRequestParametersExtBuilder();
        extBuilder.setRequestParameters(expected);
        SearchSourceBuilder sourceBuilder = SearchSourceBuilder.searchSource()
                .ext(List.of(extBuilder));
        SearchRequest request = new SearchRequest("my_index").source(sourceBuilder);
        PersonalizeRequestParameters actual = PersonalizeRequestParameterUtil.getPersonalizeRequestParameters(request);
        assertEquals(expected, actual);
    }
}