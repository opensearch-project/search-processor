/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration;

/**
 * A container for holding Personalize ranker configuration
 */
public class PersonalizeIntelligentRankerConfiguration {
    private final String personalizeCampaign;
    private final String iamRoleArn;
    private final String recipe;
    private final String itemIdField;
    private final String region;
    private final double weight;

    /**
     *
     * @param personalizeCampaign Personalize campaign
     * @param iamRoleArn          IAM Role ARN for accessing Personalize campaign
     * @param recipe              Personalize recipe associated with campaign
     * @param itemIdField         Item ID field to pick up item id for Personalize input
     * @param region              AWS region
     * @param weight              Configurable coefficient to control Personalization of search results
     */
    public PersonalizeIntelligentRankerConfiguration(String personalizeCampaign,
                                                     String iamRoleArn,
                                                     String recipe,
                                                     String itemIdField,
                                                     String region,
                                                     double weight) {
        this.personalizeCampaign = personalizeCampaign;
        this.iamRoleArn = iamRoleArn;
        this.recipe = recipe;
        this.itemIdField = itemIdField;
        this.region = region;
        this.weight = weight;
    }

    /**
     * Get PErsonalize campaign
     * @return Personalize campaign
     */
    public String getPersonalizeCampaign() {
        return personalizeCampaign;
    }

    /**
     * Get recipe
     * @return Recipe associated with Personalize campaign
     */
    public String getRecipe() {
        return recipe;
    }

    /**
     * Get Item ID field
     * @return Item ID field
     */
    public String getItemIdField() {
        return itemIdField;
    }

    /**
     * Get AWS region
     * @return AWS region
     */
    public String getRegion() {
        return region;
    }

    /**
     *
     * @return weight value
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Get IAM role ARN for Personalize campaign
     * @return IAM role for accessing Personalize campaign
     */
    public String getIamRoleArn() {
        return iamRoleArn;
    }
}
