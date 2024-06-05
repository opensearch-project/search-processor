/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.client.PersonalizeClient;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.PersonalizeIntelligentRankerConfiguration;
import org.opensearch.search.relevance.transformer.personalizeintelligentranking.reranker.impl.AmazonPersonalizedRankerImpl;

import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.Constants.AMAZON_PERSONALIZED_RANKING_RECIPE_NAME;
import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.configuration.Constants.AMAZON_PERSONALIZED_RANKING_V2_RECIPE_NAME;

/**
 * Factory for creating Personalize ranker instance based on Personalize ranker configuration
 */
public class PersonalizedRankerFactory {
    private static final Logger logger = LogManager.getLogger(PersonalizedRankerFactory.class);

    /**
     * Create an instance of Personalize ranker based on ranker configuration
     * @param config Personalize ranker configuration
     * @param client Personalize client
     * @return Personalize ranker instance
     */
    public PersonalizedRanker getPersonalizedRanker(PersonalizeIntelligentRankerConfiguration config, PersonalizeClient client){
        PersonalizedRanker ranker = null;
        String recipeInConfig = config.getRecipe();
        if (recipeInConfig.equals(AMAZON_PERSONALIZED_RANKING_RECIPE_NAME)
                || recipeInConfig.equals(AMAZON_PERSONALIZED_RANKING_V2_RECIPE_NAME)) {
            ranker = new AmazonPersonalizedRankerImpl(config, client);
        } else {
            logger.error("Personalize recipe provided in configuration is not supported for re ranking search results");
            //TODO : throw user error exception
        }
        return ranker;
    }
}
