/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.transformer.kendraintelligentranking.client;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.http.IdleConnectionReaper;
import org.opensearch.test.OpenSearchTestCase;

import java.net.URI;

public class KendraHttpClientTests extends OpenSearchTestCase {

    public void testCreateClient() throws Exception {

        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials("accessKey", "secretKey");
        KendraClientSettings settings = new KendraClientSettings(basicAWSCredentials,
                "http://localhost",
                "us-west-2",
                "12345678",
                "myAwesomeRole"
        );

        try (KendraHttpClient client = new KendraHttpClient(settings)) {
            assertEquals(new URI("http://localhost/rescore-execution-plans/12345678/rescore"), client.buildRescoreURI());
        }
        IdleConnectionReaper.shutdown();
    }

}