/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.settings;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.opensearch.common.settings.SecureSetting;
import org.opensearch.common.settings.SecureString;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Setting.Property;
import org.opensearch.search.relevance.constants.Constants;

public class KendraIntelligentRankerSettings {

  /**
   * Flag controlling whether to invoke Kendra for rescoring.
   */
  public static final Setting<Boolean> RANKER_ENABLED_SETTING = Setting.boolSetting(Constants.ENABLED_SETTING_NAME, false,
      Property.Dynamic, Property.IndexScope);

  /**
   * Document field to be considered as "body" when invoking Kendra.
   */
  public static final Setting<List<String>> BODY_FIELD_SETTING = Setting.listSetting(Constants.BODY_FIELD_SETTING_NAME, Collections.emptyList(),
      Function.identity(), new BodyFieldSettingValidator(),
      Property.Dynamic, Property.IndexScope);

  static final class BodyFieldSettingValidator implements Setting.Validator<List<String>> {

    @Override
    public void validate(List<String> value) {

    }

    @Override
    public void validate(final List<String> value, final Map<Setting<?>, Object> settings) {
      if (value != null && value.size() > 1) {
        throw new IllegalArgumentException("[" + Constants.BODY_FIELD + "] can have at most 1 element");
      }
    }
  }

  /**
   * The access key (ie login id) for connecting to Kendra.
   */
  public static final Setting<SecureString> ACCESS_KEY_SETTING = SecureSetting.secureString("kendra_intelligent_ranking.aws.access_key", null);

  /**
   * The secret key (ie password) for connecting to Kendra.
   */
  public static final Setting<SecureString> SECRET_KEY_SETTING = SecureSetting.secureString("kendra_intelligent_ranking.aws.secret_key", null);

  /**
   * The session token for connecting to Kendra.
   */
  public static final Setting<SecureString> SESSION_TOKEN_SETTING = SecureSetting.secureString("kendra_intelligent_ranking.aws.session_token", null);

  public static final Setting<String> SERVICE_ENDPOINT_SETTING = Setting.simpleString("kendra_intelligent_ranking.service.endpoint", Setting.Property.NodeScope);

  public static final Setting<String> SERVICE_REGION_SETTING = Setting.simpleString("kendra_intelligent_ranking.service.region", Setting.Property.NodeScope);

  public static final Setting<String> ENDPOINT_ID_SETTING = Setting.simpleString("kendra_intelligent_ranking.service.resource_endpoint_id", Setting.Property.NodeScope);

  public static final Setting<String> ASSUME_ROLE_ARN_SETTING = Setting.simpleString("kendra_intelligent_ranking.service.assume_role_arn", Setting.Property.NodeScope);
}
