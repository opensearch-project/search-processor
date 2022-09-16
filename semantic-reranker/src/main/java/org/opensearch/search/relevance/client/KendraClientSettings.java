/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.client;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.settings.SecureSetting;
import org.opensearch.common.settings.SecureString;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsException;

/**
 * A container for settings used to create a Kendra client.
 */
public final class KendraClientSettings {

  /**
   * The access key (ie login id) for connecting to Kendra.
   */
  public static final Setting<SecureString> ACCESS_KEY_SETTING = SecureSetting.secureString("semantic_ranker.kendra.access_key", null);

  /**
   * The secret key (ie password) for connecting to Kendra.
   */
  public static final Setting<SecureString> SECRET_KEY_SETTING = SecureSetting.secureString("semantic_ranker.kendra.secret_key", null);

  /**
   * The session token for connecting to Kendra.
   */
  public static final Setting<SecureString> SESSION_TOKEN_SETTING = SecureSetting.secureString("semantic_ranker.kendra.session_token", null);

  private static final Logger logger = LogManager.getLogger(KendraClientSettings.class);

  /**
   * Credentials to authenticate with Kendra.
   */
  final AWSCredentials credentials;

  protected KendraClientSettings(AWSCredentials credentials) {
    this.credentials = credentials;
  }

  static AWSCredentials loadCredentials(Settings settings) {
    try (SecureString key = ACCESS_KEY_SETTING.get(settings);
        SecureString secret = SECRET_KEY_SETTING.get(settings);
        SecureString sessionToken = SESSION_TOKEN_SETTING.get(settings)) {
      if (key.length() == 0 && secret.length() == 0) {
        if (sessionToken.length() > 0) {
          throw new SettingsException("Setting [{}] is set but [{}] and [{}] are not",
              SESSION_TOKEN_SETTING.getKey(), ACCESS_KEY_SETTING.getKey(), SECRET_KEY_SETTING.getKey());
        }

        logger.debug("Using either environment variables, system properties or instance profile credentials");
        return null;
      } else if (key.length() == 0 || secret.length() == 0) {
        throw new SettingsException("One of settings [{}] and [{}] is not set.",
            ACCESS_KEY_SETTING.getKey(), SECRET_KEY_SETTING.getKey());
      } else {
        final AWSCredentials credentials;
        if (sessionToken.length() == 0) {
          logger.debug("Using basic key/secret credentials");
          credentials = new BasicAWSCredentials(key.toString(), secret.toString());
        } else {
          logger.debug("Using basic session credentials");
          credentials = new BasicSessionCredentials(key.toString(), secret.toString(), sessionToken.toString());
        }
        return credentials;
      }
    }
  }

  /**
   * Parse settings for a single client.
   */
  public static KendraClientSettings getClientSettings(Settings settings) {
    final AWSCredentials credentials = loadCredentials(settings);
    return new KendraClientSettings(credentials);
  }

}
