/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.client;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.personalizeruntime.model.GetPersonalizedRankingRequest;
import com.amazonaws.services.personalizeruntime.model.GetPersonalizedRankingResult;
import org.mockito.Mockito;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.PersonalizeRuntimeTestUtil;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;

public class PersonalizeClientTests extends OpenSearchTestCase {

    public void testCreateClient() throws IOException {
        AWSCredentials credentials = new BasicSessionCredentials("accessKey", "secretKey", "sessionToken");
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        String region = "us-west-2";
        try (PersonalizeClient client = new PersonalizeClient(credentialsProvider,region)) {
            assertTrue(client.getPersonalizeRuntime() != null);
        }
    }

    public void testGetPersonalizedRanking() {
        PersonalizeClient client = Mockito.mock(PersonalizeClient.class);
        GetPersonalizedRankingRequest request = PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingRequest();
        Mockito.when(client.getPersonalizedRanking(any())).thenReturn(PersonalizeRuntimeTestUtil.buildGetPersonalizedRankingResult());
        GetPersonalizedRankingResult result = client.getPersonalizedRanking(request);
        assertEquals(result.getRecommendationId(), "sampleRecommendationId");
    }
}
