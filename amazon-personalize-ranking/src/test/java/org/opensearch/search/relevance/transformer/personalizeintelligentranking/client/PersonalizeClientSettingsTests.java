/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.client;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import org.opensearch.common.settings.SecureSetting;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.SettingsException;
import org.opensearch.common.settings.SecureString;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.PersonalizeClientSettingsTestUtil;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.PersonalizeClientSettingsTestUtil.ACCESS_KEY;
import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.PersonalizeClientSettingsTestUtil.SECRET_KEY;
import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils.PersonalizeClientSettingsTestUtil.SESSION_TOKEN;

public class PersonalizeClientSettingsTests extends OpenSearchTestCase {

    public void testWithBasicCredentials() throws IOException {
        PersonalizeClientSettings clientSettings = PersonalizeClientSettingsTestUtil.buildClientSettings(true, true, false);
        AWSCredentials credentials = clientSettings.getCredentials();
        assertEquals(ACCESS_KEY, credentials.getAWSAccessKeyId());
        assertEquals(SECRET_KEY, credentials.getAWSSecretKey());
        assertFalse(credentials instanceof AWSSessionCredentials);
    }

    public void testWithGetAllSetting() throws IOException {
        PersonalizeClientSettings clientSettings = PersonalizeClientSettingsTestUtil.buildClientSettings(true, true, true);
        assertEquals(clientSettings.getAllSettings().size(), 3);
        Setting<SecureString> ACCESS_KEY_SETTING = SecureSetting.secureString("personalized_search_ranking.aws.access_key", null);
        Setting<SecureString> SECRET_KEY_SETTING = SecureSetting.secureString("personalized_search_ranking.aws.secret_key", null);
        Setting<SecureString> SESSION_TOKEN_SETTING = SecureSetting.secureString("personalized_search_ranking.aws.session_token", null);
        assertEquals(ACCESS_KEY_SETTING, clientSettings.getAllSettings().toArray()[0]);
        assertEquals(SECRET_KEY_SETTING, clientSettings.getAllSettings().toArray()[1]);
        assertEquals(SESSION_TOKEN_SETTING, clientSettings.getAllSettings().toArray()[2]);
    }

    public void testWithSessionCredentials() throws IOException {
        PersonalizeClientSettings clientSettings = PersonalizeClientSettingsTestUtil.buildClientSettings(true, true, true);
        AWSCredentials credentials = clientSettings.getCredentials();
        assertEquals(ACCESS_KEY, credentials.getAWSAccessKeyId());
        assertEquals(SECRET_KEY, credentials.getAWSSecretKey());
        assertTrue(credentials instanceof AWSSessionCredentials);
        AWSSessionCredentials sessionCredentials = (AWSSessionCredentials) credentials;
        assertEquals(SESSION_TOKEN, sessionCredentials.getSessionToken());
    }

    public void testWithoutCredentials() throws IOException {
        PersonalizeClientSettings clientSettings = PersonalizeClientSettingsTestUtil.buildClientSettings(false, false, false);
        assertNull(clientSettings.getCredentials());
    }

    public void testWithoutAccessKey() {
        expectThrows(SettingsException.class, () -> PersonalizeClientSettingsTestUtil.buildClientSettings(false, true, false));
        expectThrows(SettingsException.class, () -> PersonalizeClientSettingsTestUtil.buildClientSettings(false, true, true));
    }

    public void testWithoutSecretKey() {
        expectThrows(SettingsException.class, () -> PersonalizeClientSettingsTestUtil.buildClientSettings(true, false, false));
        expectThrows(SettingsException.class, () -> PersonalizeClientSettingsTestUtil.buildClientSettings(true, false, true));
    }

    public void testWithSessionTokenButNoCredentials() {
        expectThrows(SettingsException.class, () -> PersonalizeClientSettingsTestUtil.buildClientSettings(false, false, true));
    }
}
