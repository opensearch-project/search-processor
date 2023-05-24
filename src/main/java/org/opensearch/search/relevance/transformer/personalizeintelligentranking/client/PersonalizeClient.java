/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.client;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.personalizeruntime.AmazonPersonalizeRuntime;
import com.amazonaws.services.personalizeruntime.AmazonPersonalizeRuntimeClientBuilder;
import com.amazonaws.services.personalizeruntime.model.GetPersonalizedRankingRequest;
import com.amazonaws.services.personalizeruntime.model.GetPersonalizedRankingResult;

import java.io.Closeable;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Amazon Personalize client implementation for getting personalized ranking
 */
public class PersonalizeClient implements Closeable {
    private final AmazonPersonalizeRuntime personalizeRuntime;

    /**
     * Constructor for Amazon Personalize client
     * @param credentialsProvider Credentials to be used for accessing Amazon Personalize
     * @param awsRegion AWS region where Amazon Personalize campaign is hosted
     */
    public PersonalizeClient(AWSCredentialsProvider credentialsProvider, String awsRegion) {
        personalizeRuntime = AccessController.doPrivileged(
                (PrivilegedAction<AmazonPersonalizeRuntime>) () -> AmazonPersonalizeRuntimeClientBuilder.standard()
                        .withCredentials(credentialsProvider)
                        .withRegion(awsRegion)
                        .build());
    }

    /**
     * Get Personalize runtime client
     * @return Personalize runtime client
     */
    public AmazonPersonalizeRuntime getPersonalizeRuntime() {
        return personalizeRuntime;
    }

    /**
     * Get Personalized ranking using Personalized runtime client
     * @param request Get personalized ranking request
     * @return Personalized ranking results
     */
    public GetPersonalizedRankingResult getPersonalizedRanking(GetPersonalizedRankingRequest request) {
        GetPersonalizedRankingResult result;
        try {
            result = AccessController.doPrivileged(
                    (PrivilegedAction<GetPersonalizedRankingResult>) () -> personalizeRuntime.getPersonalizedRanking(request));
        } catch (AmazonServiceException ex) {
            throw ex;
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        if (personalizeRuntime != null) {
            personalizeRuntime.shutdown();
        }
    }
}