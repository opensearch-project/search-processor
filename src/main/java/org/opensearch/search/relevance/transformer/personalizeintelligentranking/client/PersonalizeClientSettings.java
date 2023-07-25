/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.client;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.core.common.settings.SecureString;
import org.opensearch.common.settings.SecureSetting;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsException;

/**
 * Container for personalize client settings such as AWS credentials
 */
public final class PersonalizeClientSettings {

    private static final Logger logger = LogManager.getLogger(PersonalizeClientSettings.class);

    /**
     * The access key (ie login id) for connecting to Personalize.
     */
    public static final Setting<SecureString> ACCESS_KEY_SETTING = SecureSetting.secureString("personalized_search_ranking.aws.access_key", null);

    /**
     * The secret key (ie password) for connecting to Personalize.
     */
    public static final Setting<SecureString> SECRET_KEY_SETTING = SecureSetting.secureString("personalized_search_ranking.aws.secret_key", null);

    /**
     * The session token for connecting to Personalize.
     */
    public static final Setting<SecureString> SESSION_TOKEN_SETTING = SecureSetting.secureString("personalized_search_ranking.aws.session_token", null);

    private final AWSCredentials credentials;

    protected PersonalizeClientSettings(AWSCredentials credentials) {
        this.credentials = credentials;
    }

    public AWSCredentials getCredentials() {
        return credentials;
    }

    /**
     * Load AWS credentials from open search keystore if available
     * @param settings Open search settings
     * @return AWS credentials
     */
    static AWSCredentials loadCredentials(Settings settings) {
        try (SecureString key = ACCESS_KEY_SETTING.get(settings);
             SecureString secret = SECRET_KEY_SETTING.get(settings);
             SecureString sessionToken = SESSION_TOKEN_SETTING.get(settings)) {
            if (key.length() == 0 && secret.length() == 0) {
                if (sessionToken.length() > 0) {
                    throw new SettingsException("Setting [{}] is set but [{}] and [{}] are not",
                            SESSION_TOKEN_SETTING.getKey(), ACCESS_KEY_SETTING.getKey(), SECRET_KEY_SETTING.getKey());
                }
                logger.info("Using either environment variables, system properties or instance profile credentials");
                return null;
            } else if (key.length() == 0 || secret.length() == 0) {
                throw new SettingsException("One of settings [{}] and [{}] is not set.",
                        ACCESS_KEY_SETTING.getKey(), SECRET_KEY_SETTING.getKey());
            } else {
                final AWSCredentials credentials;
                if (sessionToken.length() == 0) {
                    logger.info("Using basic key/secret credentials");
                    credentials = new BasicAWSCredentials(key.toString(), secret.toString());
                } else {
                    logger.info("Using basic session credentials");
                    credentials = new BasicSessionCredentials(key.toString(), secret.toString(), sessionToken.toString());
                }
                return credentials;
            }
        }
    }

    /**
     * Get Personalize client settings
     * @param settings Open search settings
     * @return Personalize client settings instance with AWS credentials
     */
    public static PersonalizeClientSettings getClientSettings(Settings settings) {
        final AWSCredentials credentials = loadCredentials(settings);
        return new PersonalizeClientSettings(credentials);
    }
}
