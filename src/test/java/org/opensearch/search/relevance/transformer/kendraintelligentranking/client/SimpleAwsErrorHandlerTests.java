/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.transformer.kendraintelligentranking.client;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.opensearch.test.OpenSearchTestCase;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class SimpleAwsErrorHandlerTests extends OpenSearchTestCase {

    private static final int STATUS_CODE = HttpStatus.SC_PAYMENT_REQUIRED;
    private static final String STATUS_TEXT = "Payment required";
    private static final String SERVICE_NAME = "my-service";
    private static final String ERROR_MESSAGE = "This is the error message";

    public void testBehavior() throws Exception {
        SimpleAwsErrorHandler errorHandler = new SimpleAwsErrorHandler();
        assertFalse(errorHandler.needsConnectionLeftOpen());
        Request<?> request = new DefaultRequest<>(SERVICE_NAME);
        HttpResponse httpResponse = new HttpResponse(request, null, null);
        httpResponse.setContent(new ByteArrayInputStream(ERROR_MESSAGE.getBytes(StandardCharsets.UTF_8)));
        httpResponse.setStatusCode(STATUS_CODE);
        httpResponse.setStatusText(STATUS_TEXT);

        AmazonServiceException ase = errorHandler.handle(httpResponse);

        assertEquals(ERROR_MESSAGE, ase.getErrorMessage());
        assertEquals(STATUS_CODE, ase.getStatusCode());
        assertEquals(STATUS_TEXT, ase.getErrorCode());
        assertEquals(SERVICE_NAME, ase.getServiceName());
    }
}