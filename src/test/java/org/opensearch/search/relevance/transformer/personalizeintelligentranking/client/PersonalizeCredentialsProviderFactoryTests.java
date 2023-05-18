/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.client;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.http.IdleConnectionReaper;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.PersonalizeClientSettingsTestUtil;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class PersonalizeCredentialsProviderFactoryTests extends OpenSearchTestCase {

    public void testGetStaticCredentialsProviderWithoutIAMRole() throws IOException {
        PersonalizeClientSettings settings =
                PersonalizeClientSettingsTestUtil.buildClientSettings(true, true, true);

        PersonalizeCredentialsProviderFactory factory = new PersonalizeCredentialsProviderFactory();
        AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(settings);
        assertEquals(credentialsProvider.getClass(), AWSStaticCredentialsProvider.class);
    }

    public void testGetDefaultCredentialsProviderWithoutIAMRole() throws IOException {
        PersonalizeClientSettings settings =
                PersonalizeClientSettingsTestUtil.buildClientSettings(false, false, false);

        PersonalizeCredentialsProviderFactory factory = new PersonalizeCredentialsProviderFactory();
        AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(settings);
        assertEquals(credentialsProvider.getClass(), DefaultAWSCredentialsProviderChain.class);
    }

    public void testGetCredentialsProviderWithIAMRole() throws IOException {
        PersonalizeClientSettings settings =
                PersonalizeClientSettingsTestUtil.buildClientSettings(true, true, true);

        String iamRoleArn = "test-iam-role-arn";
        String awsRegion = "us-west-2";
        PersonalizeCredentialsProviderFactory factory = new PersonalizeCredentialsProviderFactory();
        AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(settings, iamRoleArn, awsRegion);
        assertEquals(credentialsProvider.getClass(), STSAssumeRoleSessionCredentialsProvider.class);
        IdleConnectionReaper.shutdown();
    }

    public void testGetStaticCredentialsProviderWithEmptyIAMRole() throws IOException {
        PersonalizeClientSettings settings =
                PersonalizeClientSettingsTestUtil.buildClientSettings(true, true, true);

        String iamRoleArn = "";
        String awsRegion = "us-west-2";
        PersonalizeCredentialsProviderFactory factory = new PersonalizeCredentialsProviderFactory();
        AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(settings, iamRoleArn, awsRegion);
        assertEquals(credentialsProvider.getClass(), AWSStaticCredentialsProvider.class);
    }

    public void testGetDefaultCredentialsProviderWithEmptyIAMRole() throws IOException {
        PersonalizeClientSettings settings =
                PersonalizeClientSettingsTestUtil.buildClientSettings(false, false, false);

        String iamRoleArn = "";
        String awsRegion = "us-west-2";
        PersonalizeCredentialsProviderFactory factory = new PersonalizeCredentialsProviderFactory();
        AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(settings, iamRoleArn, awsRegion);
        assertEquals(credentialsProvider.getClass(), DefaultAWSCredentialsProviderChain.class);
    }
}
