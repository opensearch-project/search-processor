/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.client;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.http.ExecutionContext;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.http.HttpResponseHandler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import org.opensearch.search.relevance.constants.Constants;
import org.opensearch.search.relevance.model.dto.RerankRequest;
import org.opensearch.search.relevance.model.dto.RerankResult;

public class KendraHttpClient {
  private final AmazonHttpClient amazonHttpClient;
  private final HttpResponseHandler<AmazonServiceException> errorHandler;
  private final HttpResponseHandler<String> responseHandler;
  private final AWSCredentials awsCredentials;
  private final AWS4Signer aws4Signer;
  private final String serviceEndpoint;
  private final String endpointId;

  private final ObjectMapper objectMapper;

  public KendraHttpClient(KendraClientSettings clientSettings) {
    // TODO: remove below line once DNS is fixed
    AccessController.doPrivileged((PrivilegedAction<String>) () -> System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true"));
    
    amazonHttpClient = AccessController.doPrivileged((PrivilegedAction<AmazonHttpClient>) () -> new AmazonHttpClient(new ClientConfiguration()));
    errorHandler = new SimpleAwsErrorHandler();
    responseHandler = new SimpleResponseHandler();
    aws4Signer = new AWS4Signer();
    aws4Signer.setServiceName(Constants.KENDRA_RANKING_SERVICE_NAME);
    aws4Signer.setRegionName(clientSettings.getServiceRegion());
    objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    awsCredentials = clientSettings.getCredentials();
    serviceEndpoint = clientSettings.getServiceEndpoint();
    endpointId = clientSettings.getEndpointId();
  }

  public RerankResult rerank(RerankRequest rerankRequest) {
    return AccessController.doPrivileged((PrivilegedAction<RerankResult>) () -> {
      try {
        rerankRequest.setRerankingEndpointId(endpointId);
        System.out.println(rerankRequest.getRerankingEndpointId());
        System.out.println(rerankRequest.getDocuments());
        System.out.println(rerankRequest.getQueryText());
        Request<Void> request = new DefaultRequest<>(aws4Signer.getServiceName());
        request.setHttpMethod(HttpMethodName.POST);
        request.setEndpoint(URI.create(serviceEndpoint));
        request.setHeaders(Map.of("Content-Type", "application/x-amz-json-1.0", "X-Amz-Target", "AWSKendraRerankingFrontendService.Rerank"));
        System.out.println("objectMapper: " + objectMapper.writeValueAsString(rerankRequest));
        request.setContent(new ByteArrayInputStream(objectMapper.writeValueAsString(rerankRequest).getBytes(StandardCharsets.UTF_8)));
        aws4Signer.sign(request, awsCredentials);

        Response<String> rsp = amazonHttpClient
            .requestExecutionBuilder()
            .executionContext(new ExecutionContext(true))
            .request(request)
            .errorResponseHandler(errorHandler)
            .execute(responseHandler);

        return objectMapper.readValue(rsp.getAwsResponse(), RerankResult.class);
      } catch (Exception ex) {
        throw new RuntimeException("Exception executing request.", ex);
      }
    });
  }

}
