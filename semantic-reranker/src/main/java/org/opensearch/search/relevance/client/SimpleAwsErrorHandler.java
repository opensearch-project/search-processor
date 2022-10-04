/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.client;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.http.HttpResponseHandler;

import java.nio.charset.StandardCharsets;

public class SimpleAwsErrorHandler implements HttpResponseHandler<AmazonServiceException> {
  @Override public AmazonServiceException handle(HttpResponse response) throws Exception {
    AmazonServiceException ase = new AmazonServiceException(new String(response.getContent().readAllBytes(), StandardCharsets.UTF_8));
    ase.setStatusCode(response.getStatusCode());
    ase.setServiceName(response.getRequest().getServiceName());
    ase.setErrorCode(response.getStatusText());
    return ase;
  }

  @Override public boolean needsConnectionLeftOpen() {
    return false;
  }
}
