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
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.http.ExecutionContext;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.http.HttpResponseHandler;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;

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
  private final AWSCredentialsProvider awsCredentialsProvider;
  private final AWS4Signer aws4Signer;
  private final String serviceEndpoint;
  private final String endpointId;
  private final ObjectMapper objectMapper;

  public KendraHttpClient(KendraClientSettings clientSettings) {
    amazonHttpClient = AccessController.doPrivileged((PrivilegedAction<AmazonHttpClient>) () -> new AmazonHttpClient(new ClientConfiguration()));
    errorHandler = new SimpleAwsErrorHandler();
    responseHandler = new SimpleResponseHandler();
    aws4Signer = new AWS4Signer();
    aws4Signer.setServiceName(Constants.KENDRA_RANKING_SERVICE_NAME);
    aws4Signer.setRegionName(clientSettings.getServiceRegion());
    objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    serviceEndpoint = clientSettings.getServiceEndpoint();
    endpointId = clientSettings.getEndpointId();

    final AWSCredentialsProvider credentialsProvider;
    final AWSCredentials credentials = clientSettings.getCredentials();
    if (credentials == null) {
      // Use environment variables, system properties or instance profile credentials.
      credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance();
    } else {
      // Use keystore credentials.
      credentialsProvider = new AWSStaticCredentialsProvider(credentials);
    }

    final String assumeRoleArn = clientSettings.getAssumeRoleArn();
    if (assumeRoleArn != null && !assumeRoleArn.isBlank()) {
      // If AssumeRoleArn was provided in config, use auto-refreshed role credentials.
      awsCredentialsProvider = AccessController.doPrivileged(
          (PrivilegedAction<AWSCredentialsProvider>) () -> {
            AWSSecurityTokenService awsSecurityTokenService = AWSSecurityTokenServiceClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(clientSettings.getServiceRegion())
                .build();

            return new STSAssumeRoleSessionCredentialsProvider.Builder(clientSettings.getAssumeRoleArn(), Constants.ASSUME_ROLE_SESSION_NAME)
                .withStsClient(awsSecurityTokenService)
                .build();
          });
    } else {
      awsCredentialsProvider = credentialsProvider;
    }
  }

  public RerankResult rerank(RerankRequest rerankRequest) {
    return AccessController.doPrivileged((PrivilegedAction<RerankResult>) () -> {
      try {
        rerankRequest.setRerankingEndpointId(endpointId);
        Request<Void> request = new DefaultRequest<>(aws4Signer.getServiceName());
        request.setHttpMethod(HttpMethodName.POST);
        request.setEndpoint(URI.create(serviceEndpoint));
        request.setHeaders(Map.of("Content-Type", "application/x-amz-json-1.0", "X-Amz-Target", "AWSKendraRerankingFrontendService.Rerank"));
        request.setContent(new ByteArrayInputStream(objectMapper.writeValueAsString(rerankRequest).getBytes(StandardCharsets.UTF_8)));
        aws4Signer.sign(request, awsCredentialsProvider.getCredentials());

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