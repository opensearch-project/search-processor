/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration;

import org.opensearch.test.OpenSearchTestCase;

public class PersonalizeIntelligentRankerConfigurationTests extends OpenSearchTestCase {

    public void createConfigurationTest() {
        String personalizeCampaign = "arn:aws:personalize:us-west-2:000000000000:campaign/test-campaign";
        String iamRoleArn = "sampleRoleArn";
        String recipe = "sample-personalize-recipe";
        String itemIdField = "ITEM_ID";
        String region = "us-west-2";
        double weight = 0.25;

        PersonalizeIntelligentRankerConfiguration config =
                new PersonalizeIntelligentRankerConfiguration(personalizeCampaign, iamRoleArn, recipe, itemIdField, region, weight);

        assertEquals(config.getPersonalizeCampaign(), personalizeCampaign);
        assertEquals(config.getIamRoleArn(), iamRoleArn);
        assertEquals(config.getRecipe(), recipe);
        assertEquals(config.getItemIdField(), itemIdField);
        assertEquals(config.getRegion(), region);
        assertEquals(config.getWeight(), weight, 0.0);
    }
}
