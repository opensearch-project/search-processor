/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils;

import org.opensearch.common.settings.MockSecureSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClientSettings;

import java.io.IOException;

public class PersonalizeClientSettingsTestUtil {
    public static final String ACCESS_KEY = "my-access-key";
    public static final String SECRET_KEY = "my-secret-key";
    public static final String SESSION_TOKEN = "session-token";

    public static PersonalizeClientSettings buildClientSettings(boolean withAccessKey, boolean withSecretKey,
                                                                 boolean withSessionToken) throws IOException {
        try (MockSecureSettings secureSettings = new MockSecureSettings()) {
            if (withAccessKey) {
                secureSettings.setString(PersonalizeClientSettings.ACCESS_KEY_SETTING.getKey(), ACCESS_KEY);
            }
            if (withSecretKey) {
                secureSettings.setString(PersonalizeClientSettings.SECRET_KEY_SETTING.getKey(), SECRET_KEY);
            }
            if (withSessionToken) {
                secureSettings.setString(PersonalizeClientSettings.SESSION_TOKEN_SETTING.getKey(), SESSION_TOKEN);
            }
            Settings settings = Settings.builder()
                    .setSecureSettings(secureSettings)
                    .build();
            return PersonalizeClientSettings.getClientSettings(settings);
        }
    }
}
