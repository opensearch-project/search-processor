/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.client;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kendra.AWSkendra;
import com.amazonaws.services.kendra.AWSkendraClientBuilder;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class KendraClient {
  // TODO: Update this client. Kendra is placeholder and intentionally not being used right now.
  private AWSkendra client;

  public KendraClient(KendraClientSettings clientSettings) {
    this.client = AccessController.doPrivileged(
        (PrivilegedAction<AWSkendra>) () -> {
          final AWSCredentialsProvider credentialsProvider;
          final AWSCredentials credentials = clientSettings.credentials;
          if (credentials == null) {
            credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance();
          } else {
            credentialsProvider = new AWSStaticCredentialsProvider(credentials);
          }
          return AWSkendraClientBuilder.standard()
              .withCredentials(credentialsProvider)
              .withRegion(Regions.US_WEST_2)
              .build();
        }
    );
  }
}
