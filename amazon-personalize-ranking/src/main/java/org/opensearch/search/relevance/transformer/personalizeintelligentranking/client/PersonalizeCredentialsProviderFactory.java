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
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Factory implementation for getting Personalize credentials
 */
public final class PersonalizeCredentialsProviderFactory {
    private static final Logger logger = LogManager.getLogger(PersonalizeCredentialsProviderFactory.class);
    private static final String ASSUME_ROLE_SESSION_NAME = "OpenSearchPersonalizeIntelligentRankingPluginSession";

    private PersonalizeCredentialsProviderFactory() {
    }

    /**
     * Get AWS credentials provider either from static credentials from open search keystore or
     * using DefaultAWSCredentialsProviderChain.
     * @param clientSettings Personalize client settings
     * @return AWS credentials provider for accessing Personalize
     */
    static AWSCredentialsProvider getCredentialsProvider(PersonalizeClientSettings clientSettings) {
        final AWSCredentialsProvider credentialsProvider;
        final AWSCredentials credentials = clientSettings.getCredentials();
        if (credentials == null) {
            logger.info("Credentials not present in open search keystore. Using DefaultAWSCredentialsProviderChain for credentials.");
            credentialsProvider = AccessController.doPrivileged(
                    (PrivilegedAction<AWSCredentialsProvider>) () -> DefaultAWSCredentialsProviderChain.getInstance());
        } else {
            logger.info("Using credentials provided in open search keystore");
            credentialsProvider = AccessController.doPrivileged(
                    (PrivilegedAction<AWSCredentialsProvider>) () -> new AWSStaticCredentialsProvider(credentials));
        }
        return credentialsProvider;
    }

    /**
     * Get AWS credentials provider by assuming IAM role if provided or else
     * use static credentials or DefaultAWSCredentialsProviderChain.
     * @param clientSettings        Personalize client settings
     * @param personalizeIAMRole    IAM role configuration for accessing Personalize
     * @param awsRegion             AWS region
     * @return AWS credentials provider for accessing Amazon Personalize
     */
    public static AWSCredentialsProvider getCredentialsProvider(PersonalizeClientSettings clientSettings,
                                                         String personalizeIAMRole,
                                                         String awsRegion) {

        final AWSCredentialsProvider credentialsProvider;
        AWSCredentialsProvider baseCredentialsProvider = getCredentialsProvider(clientSettings);

        if (personalizeIAMRole != null && !personalizeIAMRole.isBlank()) {
            logger.info("Using IAM Role provided to access Personalize.");
            // If IAM role ARN was provided in config, then use auto-refreshed role credentials.
            credentialsProvider = AccessController.doPrivileged(
                    (PrivilegedAction<AWSCredentialsProvider>) () -> {
                        AWSSecurityTokenService awsSecurityTokenService = AWSSecurityTokenServiceClientBuilder.standard()
                                .withCredentials(baseCredentialsProvider)
                                .withRegion(awsRegion)
                                .build();

                        return new STSAssumeRoleSessionCredentialsProvider.Builder(personalizeIAMRole, ASSUME_ROLE_SESSION_NAME)
                                .withStsClient(awsSecurityTokenService)
                                .build();
                    });
        } else {
            logger.info("IAM Role for accessing Personalize is not provided.");
            credentialsProvider = baseCredentialsProvider;
        }
        return credentialsProvider;
    }
}
