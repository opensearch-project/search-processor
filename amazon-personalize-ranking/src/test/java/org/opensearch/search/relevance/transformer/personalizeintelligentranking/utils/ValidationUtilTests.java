/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.utils;

import com.amazonaws.http.IdleConnectionReaper;
import org.opensearch.OpenSearchParseException;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.PersonalizeIntelligentRankerConfiguration;
import org.opensearch.test.OpenSearchTestCase;

import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.Constants.AMAZON_PERSONALIZED_RANKING_RECIPE_NAME;
import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.Constants.AMAZON_PERSONALIZED_RANKING_V2_RECIPE_NAME;

public class ValidationUtilTests extends OpenSearchTestCase {

    private static final String TYPE = "personalize_ranking";
    private static final String TAG = "test_tag";
    private String personalizeCampaign = "arn:aws:personalize:us-west-2:000000000000:campaign/test-campaign";
    private String iamRoleArn = "arn:aws:iam::000000000000:role/test";
    private String itemIdField = "ITEM_ID";
    private String region = "us-west-2";
    private double weight = 1.0;

    public void testValidRankerConfig () {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, AMAZON_PERSONALIZED_RANKING_RECIPE_NAME, itemIdField, region, weight);
        ValidationUtil.validatePersonalizeIntelligentRankerConfiguration(rankerConfig, TYPE, TAG);
    }

    public void testValidRankerConfigPersonalizedRankingV2 () {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, AMAZON_PERSONALIZED_RANKING_V2_RECIPE_NAME, itemIdField, region, weight);
        ValidationUtil.validatePersonalizeIntelligentRankerConfiguration(rankerConfig, TYPE, TAG);
    }

    public void testInvalidCampaignArn () {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration("invalid:campaign/test", iamRoleArn, AMAZON_PERSONALIZED_RANKING_RECIPE_NAME, itemIdField, region, weight);
        expectThrows(OpenSearchParseException.class, () ->
                ValidationUtil.validatePersonalizeIntelligentRankerConfiguration(rankerConfig, TYPE, TAG));
    }

    public void testEmptyCampaignArn () {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration("", iamRoleArn, AMAZON_PERSONALIZED_RANKING_RECIPE_NAME, itemIdField, region, weight);
        expectThrows(OpenSearchParseException.class, () ->
                ValidationUtil.validatePersonalizeIntelligentRankerConfiguration(rankerConfig, TYPE, TAG));
    }

    public void testNonPersonalizeArnAsCampaignArn () {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration("arn:aws:es:us-west-2:000000000000:domain/testmovies", iamRoleArn, AMAZON_PERSONALIZED_RANKING_RECIPE_NAME, itemIdField, region, weight);
        expectThrows(OpenSearchParseException.class, () ->
                ValidationUtil.validatePersonalizeIntelligentRankerConfiguration(rankerConfig, TYPE, TAG));
    }

    public void testInvalidIamRoleArn () {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, "invalid:arn/test", AMAZON_PERSONALIZED_RANKING_RECIPE_NAME, itemIdField, region, weight);
        expectThrows(OpenSearchParseException.class, () ->
                ValidationUtil.validatePersonalizeIntelligentRankerConfiguration(rankerConfig, TYPE, TAG));
    }

    public void testNonIamArnAsIamRoleArn () {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, "arn:aws:es:us-west-2:000000000000:domain/testmovies", AMAZON_PERSONALIZED_RANKING_RECIPE_NAME, itemIdField, region, weight);
        expectThrows(OpenSearchParseException.class, () ->
                ValidationUtil.validatePersonalizeIntelligentRankerConfiguration(rankerConfig, TYPE, TAG));
    }

    public void testEmptyIamRoleArnAllowed () {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, "", AMAZON_PERSONALIZED_RANKING_RECIPE_NAME, itemIdField, region, weight);
        ValidationUtil.validatePersonalizeIntelligentRankerConfiguration(rankerConfig, TYPE, TAG);
    }

    public void testInvalidWeightValueGreaterThanRange () {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, AMAZON_PERSONALIZED_RANKING_RECIPE_NAME, itemIdField, region, 3.0);
        expectThrows(OpenSearchParseException.class, () ->
                ValidationUtil.validatePersonalizeIntelligentRankerConfiguration(rankerConfig, TYPE, TAG));
    }

    public void testInvalidWeightValueLessThanRange () {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, AMAZON_PERSONALIZED_RANKING_RECIPE_NAME, itemIdField, region, -1.0);
        expectThrows(OpenSearchParseException.class, () ->
                ValidationUtil.validatePersonalizeIntelligentRankerConfiguration(rankerConfig, TYPE, TAG));
    }

    public void testNonPersonalizedRankingRecipeConfig () {
        PersonalizeIntelligentRankerConfiguration rankerConfig =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, "aws-user-personalization", itemIdField, region, -1.0);
        expectThrows(OpenSearchParseException.class, () ->
                ValidationUtil.validatePersonalizeIntelligentRankerConfiguration(rankerConfig, TYPE, TAG));
    }
}
