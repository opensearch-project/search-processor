/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils;
import com.amazonaws.arn.Arn;
import org.opensearch.ingest.ConfigurationUtils;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.PersonalizeIntelligentRankerConfiguration;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.Constants.AMAZON_PERSONALIZED_RANKING_RECIPE_NAME;

public class ValidationUtil {
    private static Set<String> SUPPORTED_PERSONALIZE_RECIPES = new HashSet<>(Arrays.asList(AMAZON_PERSONALIZED_RANKING_RECIPE_NAME));

    /**
     * Validate Personalize configuration for calling Personalize service.
     * Throws OpenSearchParseException type exception if validation fails.
     * @param config Personalize intelligent ranker configuration
     * @param processorType Name of search pipeline processor
     * @param processorTag Name of processor tag
     */
    public static void validatePersonalizeIntelligentRankerConfiguration (PersonalizeIntelligentRankerConfiguration config,
                                                                          String processorType,
                                                                          String processorTag
                                                                          ) {
        // Validate weight value
        if (config.getWeight() < 0.0 || config.getWeight() > 1.0) {
            throw ConfigurationUtils.newConfigurationException(processorType, processorTag, "weight", "invalid value for weight");
        }
        // Validate Personalize campaign ARN
        if(!isValidCampaignOrRoleArn(config.getPersonalizeCampaign(), "personalize")) {
            throw ConfigurationUtils.newConfigurationException(processorType, processorTag, "campaign_arn", "invalid format for Personalize campaign arn");
        }
        // Validate IAM Role Arn for Personalize access
        String iamRoleArn = config.getIamRoleArn();
        if(!(iamRoleArn != null || iamRoleArn.isBlank()) && !isValidCampaignOrRoleArn(iamRoleArn, "iam")) {
            throw ConfigurationUtils.newConfigurationException(processorType, processorTag, "iam_role_arn", "invalid format for Personalize iam role arn");
        }
        // Validate Personalize recipe
        if(!SUPPORTED_PERSONALIZE_RECIPES.contains(config.getRecipe())) {
            throw ConfigurationUtils.newConfigurationException(processorType, processorTag, "recipe", "not supported recipe provided");
        }
    }

    private static boolean isValidCampaignOrRoleArn(String arn, String expectedService) {
        try {
            Arn arnObj = Arn.fromString(arn);
            return arnObj.getService().equals(expectedService);
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }
}
