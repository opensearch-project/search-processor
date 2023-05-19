/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.client;

import org.mockito.Mockito;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreRequest;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreResult;
import org.opensearch.test.OpenSearchTestCase;

import java.util.function.Function;

public class KendraIntelligentClientTests extends OpenSearchTestCase {
    protected static KendraHttpClient buildMockHttpClient(Function<RescoreRequest, RescoreResult> mockRescoreImpl) {
        KendraHttpClient kendraHttpClient = Mockito.mock(KendraHttpClient.class);
        Mockito.when(kendraHttpClient.isValid()).thenReturn(true);
        Mockito.doAnswer(invocation -> {
            RescoreRequest rescoreRequest = invocation.getArgument(0);
            return mockRescoreImpl.apply(rescoreRequest);
        }).when(kendraHttpClient).rescore(Mockito.any(RescoreRequest.class));
        return kendraHttpClient;
    }

    protected static KendraHttpClient buildMockHttpClient() {
        return buildMockHttpClient(r -> new RescoreResult());
    }

}
