/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.client;

import com.amazonaws.http.HttpResponse;
import com.amazonaws.http.HttpResponseHandler;

import java.nio.charset.StandardCharsets;

public class SimpleResponseHandler implements HttpResponseHandler<String> {
  @Override public String handle(HttpResponse response) throws Exception {
    return new String(response.getContent().readAllBytes(), StandardCharsets.UTF_8);
  }

  @Override public boolean needsConnectionLeftOpen() {
    return false;
  }
}
