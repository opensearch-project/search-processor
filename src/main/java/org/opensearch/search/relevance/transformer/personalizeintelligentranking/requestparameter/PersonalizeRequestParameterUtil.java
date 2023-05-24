/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.search.SearchExtBuilder;

import java.util.List;
import java.util.stream.Collectors;

public class PersonalizeRequestParameterUtil {

    public static PersonalizeRequestParameters getPersonalizeRequestParameters(SearchRequest searchRequest) {
        PersonalizeRequestParametersExtBuilder personalizeRequestParameterExtBuilder = null;
        if (searchRequest.source() != null && searchRequest.source().ext() != null && !searchRequest.source().ext().isEmpty()) {
            List<SearchExtBuilder> extBuilders = searchRequest.source().ext().stream()
                    .filter(extBuilder -> PersonalizeRequestParametersExtBuilder.NAME.equals(extBuilder.getWriteableName()))
                    .collect(Collectors.toList());

            if (!extBuilders.isEmpty()) {
                personalizeRequestParameterExtBuilder = (PersonalizeRequestParametersExtBuilder) extBuilders.get(0);
            }
        }
        PersonalizeRequestParameters personalizeRequestParameters = null;
        if (personalizeRequestParameterExtBuilder != null) {
            personalizeRequestParameters = personalizeRequestParameterExtBuilder.getRequestParameters();
        }
        return personalizeRequestParameters;
    }
}
