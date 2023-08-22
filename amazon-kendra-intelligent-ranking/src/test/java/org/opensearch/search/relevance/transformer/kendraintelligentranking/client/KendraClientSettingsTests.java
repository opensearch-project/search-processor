/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.transformer.kendraintelligentranking.client;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import org.opensearch.common.settings.MockSecureSettings;
import org.opensearch.common.settings.SecureSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsException;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankerSettings;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class KendraClientSettingsTests extends OpenSearchTestCase {

    private static final String REGION = "us-west-2";
    private static final String ENDPOINT = "http://localhost";
    private static final String ACCESS_KEY = "my-access-key";
    private static final String SECRET_KEY = "my-secret-key";
    private static final String ASSUMED_ROLE = "assumed-role";
    private static final String SESSION_TOKEN = "session-token";


    private static KendraClientSettings buildClientSettings(boolean withAccessKey, boolean withSecretKey,
                                                            boolean withSessionToken) throws IOException {
        try (MockSecureSettings secureSettings = new MockSecureSettings()) {
            if (withAccessKey) {
                secureSettings.setString(KendraIntelligentRankerSettings.ACCESS_KEY_SETTING.getKey(), ACCESS_KEY);
            }
            if (withSecretKey) {
                secureSettings.setString(KendraIntelligentRankerSettings.SECRET_KEY_SETTING.getKey(), SECRET_KEY);
            }
            if (withSessionToken) {
                secureSettings.setString(KendraIntelligentRankerSettings.SESSION_TOKEN_SETTING.getKey(), SESSION_TOKEN);
            }
            Settings settings = Settings.builder()
                    .put(KendraIntelligentRankerSettings.SERVICE_REGION_SETTING.getKey(), REGION)
                    .put(KendraIntelligentRankerSettings.SERVICE_ENDPOINT_SETTING.getKey(), ENDPOINT)
                    .put(KendraIntelligentRankerSettings.ASSUME_ROLE_ARN_SETTING.getKey(), ASSUMED_ROLE)
                    .setSecureSettings(secureSettings)
                    .build();

            return KendraClientSettings.getClientSettings(settings);
        }
    }

    public void testWithBasicCredentials() throws IOException {
        KendraClientSettings clientSettings = buildClientSettings(true, true, false);

        AWSCredentials credentials = clientSettings.getCredentials();
        assertEquals(ACCESS_KEY, credentials.getAWSAccessKeyId());
        assertEquals(SECRET_KEY, credentials.getAWSSecretKey());
        assertFalse(credentials instanceof AWSSessionCredentials);
        assertEquals(REGION, clientSettings.getServiceRegion());
        assertEquals(ENDPOINT, clientSettings.getServiceEndpoint());
        assertEquals(ASSUMED_ROLE, clientSettings.getAssumeRoleArn());
    }

    public void testWithSessionCredentials() throws IOException {
        KendraClientSettings clientSettings = buildClientSettings(true, true, true);

        AWSCredentials credentials = clientSettings.getCredentials();
        assertEquals(ACCESS_KEY, credentials.getAWSAccessKeyId());
        assertEquals(SECRET_KEY, credentials.getAWSSecretKey());
        assertTrue(credentials instanceof AWSSessionCredentials);
        AWSSessionCredentials sessionCredentials = (AWSSessionCredentials) credentials;
        assertEquals(SESSION_TOKEN, sessionCredentials.getSessionToken());
        assertEquals(REGION, clientSettings.getServiceRegion());
        assertEquals(ENDPOINT, clientSettings.getServiceEndpoint());
        assertEquals(ASSUMED_ROLE, clientSettings.getAssumeRoleArn());
    }

    public void testWithoutCredentials() throws IOException {
        KendraClientSettings clientSettings = buildClientSettings(false, false, false);

        assertNull(clientSettings.getCredentials());
        assertEquals(REGION, clientSettings.getServiceRegion());
        assertEquals(ENDPOINT, clientSettings.getServiceEndpoint());
        assertEquals(ASSUMED_ROLE, clientSettings.getAssumeRoleArn());
    }

    public void testWithoutAccessKey() {
        expectThrows(SettingsException.class, () -> buildClientSettings(false, true, false));
        expectThrows(SettingsException.class, () -> buildClientSettings(false, true, true));
    }

    public void testWithoutSecretKey() {
        expectThrows(SettingsException.class, () -> buildClientSettings(true, false, false));
        expectThrows(SettingsException.class, () -> buildClientSettings(true, false, true));
    }

    public void testWithSessionTokenButNoCredentials() {
        expectThrows(SettingsException.class, () -> buildClientSettings(false, false, true));
    }
}