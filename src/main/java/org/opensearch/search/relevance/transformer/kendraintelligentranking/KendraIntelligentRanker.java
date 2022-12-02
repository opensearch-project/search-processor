/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.common.settings.Setting;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.SearchService;
import org.opensearch.search.relevance.configuration.ResultTransformerConfiguration;
import org.opensearch.search.relevance.configuration.ResultTransformerConfigurationFactory;
import org.opensearch.search.relevance.transformer.ResultTransformer;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraHttpClient;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankerSettings;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankingConfiguration;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankingConfigurationFactory;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.KendraIntelligentRankingException;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.PassageScore;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.Document;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreRequest;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreResult;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreResultItem;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.BM25Scorer;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.PassageGenerator;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.QueryParser;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.QueryParser.QueryParserResult;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.TextTokenizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

import static org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.Constants.BODY_FIELD;

public class KendraIntelligentRanker implements ResultTransformer {
    private static final int MAX_SENTENCE_LENGTH_IN_TOKENS = 35;
    private static final int MIN_PASSAGE_LENGTH_IN_TOKENS = 100;
    private static final int MAX_PASSAGE_COUNT = 10;
    private static final int TITLE_TOKENS_TRIMMED = 15;
    private static final int BODY_PASSAGE_TRIMMED = 200;
    private static final double BM25_B_VALUE = 0.75;
    private static final double BM25_K1_VALUE = 1.6;
    private static final int TOP_K_PASSAGES = 3;

    private static final Logger logger = LogManager.getLogger(KendraIntelligentRanker.class);
    public static final String NAME = "kendra_intelligent_ranking";

    private final KendraHttpClient kendraClient;
    private final TextTokenizer textTokenizer;
    private final QueryParser queryParser;

    public KendraIntelligentRanker(KendraHttpClient kendraClient) {
        this.kendraClient = kendraClient;
        this.textTokenizer = new TextTokenizer();
        this.queryParser = new QueryParser();
    }

    @Override
    public List<Setting<?>> getTransformerSettings() {
        return KendraIntelligentRankerSettings.getAllSettings();
    }


    @Override
    public ResultTransformerConfigurationFactory getConfigurationFactory() {
        return KendraIntelligentRankingConfigurationFactory.INSTANCE;
    }


    /**
     * Check if search request is eligible for rescore
     *
     * @param request Search Request
     * @return boolean decision on whether to re-rank
     */
    @Override
    public boolean shouldTransform(final SearchRequest request, final ResultTransformerConfiguration configuration) {
        if (request.source() == null || request.source().query() == null) {
            return false;
        }
        KendraIntelligentRankingConfiguration kendraConfiguration = (KendraIntelligentRankingConfiguration) configuration;

        // Skip if there is scroll, sorting, or the start of the page is greater than the document limit for Kendra Ranking
        if (request.scroll() != null ||
                (request.source().sorts() != null && !request.source().sorts().isEmpty()) ||
                request.source().from() >= kendraConfiguration.getProperties().getDocLimit()) {
            return false;
        }
        if (!kendraClient.isValid()) {
            logger.warn("Kendra ranking endpoint was not configured. Skipping reranking.");
            return false;
        }
        return true;
    }

    @Override
    public SearchRequest preprocessRequest(final SearchRequest request, final ResultTransformerConfiguration configuration) {
        // Source is returned in response hits by default. If disabled by the user, overwrite and enable
        // in order to access document contents for reranking, then suppress at response time.
        if (request.source() != null && request.source().fetchSource() != null &&
                !request.source().fetchSource().fetchSource()) {
            request.source().fetchSource(true);
        }

        int from = request.source().from() == -1 ? SearchService.DEFAULT_FROM : request.source().from();
        int size = request.source().size() == -1 ? SearchService.DEFAULT_SIZE : request.source().size();

        KendraIntelligentRankingConfiguration kendraConfiguration = (KendraIntelligentRankingConfiguration) configuration;
        int sizeOverride = Math.max(kendraConfiguration.getProperties().getDocLimit(), from + size);
        request.source().from(SearchService.DEFAULT_FROM);
        request.source().size(sizeOverride);
        return request;
    }

    /**
     * @param hits    Search hits to rerank with respect to query
     * @param request Search request
     * @return SearchHits reranked search hits
     */
    @Override
    public SearchHits transform(final SearchHits hits,
                                final SearchRequest request,
                                final ResultTransformerConfiguration configuration) {
        if (hits.getHits().length == 0) {
            // Avoid call to rerank empty results
            return hits;
        }
        KendraIntelligentRankingConfiguration kendraConfig = (KendraIntelligentRankingConfiguration) configuration;
        QueryParserResult queryParserResult = queryParser.parse(
                request.source().query(),
                kendraConfig.getProperties().getBodyFields(),
                kendraConfig.getProperties().getTitleFields());
        if (queryParserResult == null) {
            // Unknown query type or query does not reference body field
            return hits;
        }
        KendraIntelligentRankingConfiguration kendraConfiguration = (KendraIntelligentRankingConfiguration) configuration;
        try {
            List<SearchHit> originalHits = Arrays.asList(hits.getHits());
            final int numberOfHitsToRerank = Math.min(originalHits.size(), kendraConfiguration.getProperties().getDocLimit());
            List<Document> originalHitsAsDocuments = new ArrayList<>();
            Map<String, SearchHit> idToSearchHitMap = new HashMap<>();
            for (int j = 0; j < numberOfHitsToRerank; ++j) {
                Map<String, Object> docSourceMap = originalHits.get(j).getSourceAsMap();
                PassageGenerator passageGenerator = new PassageGenerator();
                String bodyFieldName = queryParserResult.getBodyFieldName();
                String titleFieldName = queryParserResult.getTitleFieldName();
                if (docSourceMap.get(bodyFieldName) == null) {
                    String errorMessage = String.format(Locale.ENGLISH,
                            "Kendra Intelligent Ranking cannot be applied when documents are missing %s [%s]. Document ID [%s].",
                            BODY_FIELD, bodyFieldName, originalHits.get(j).getId());
                    logger.error(errorMessage);
                    throw new KendraIntelligentRankingException(errorMessage);
                }
                List<List<String>> passages = passageGenerator.generatePassages(docSourceMap.get(bodyFieldName).toString(),
                        MAX_SENTENCE_LENGTH_IN_TOKENS, MIN_PASSAGE_LENGTH_IN_TOKENS, MAX_PASSAGE_COUNT);
                List<List<String>> topPassages = getTopPassages(queryParserResult.getQueryText(), passages);
                List<String> tokenizedTitle = null;
                if (titleFieldName != null && docSourceMap.get(titleFieldName) != null) {
                    tokenizedTitle = textTokenizer.tokenize(docSourceMap.get(queryParserResult.getTitleFieldName()).toString());
                    // If tokens list is empty, use null
                    if (tokenizedTitle.isEmpty()) {
                        tokenizedTitle = null;
                    } else if (tokenizedTitle.size() > TITLE_TOKENS_TRIMMED) {
                        tokenizedTitle = tokenizedTitle.subList(0, TITLE_TOKENS_TRIMMED);
                    }
                }
                for (int i = 0; i < topPassages.size(); ++i) {
                    List<String> passageTokens = topPassages.get(i);
                    if (passageTokens != null && !passageTokens.isEmpty() && passageTokens.size() > BODY_PASSAGE_TRIMMED) {
                        passageTokens = passageTokens.subList(0, BODY_PASSAGE_TRIMMED);
                    }
                    originalHitsAsDocuments.add(new Document(
                            originalHits.get(j).getId() + "@" + (i + 1),
                            originalHits.get(j).getId(),
                            tokenizedTitle,
                            passageTokens,
                            originalHits.get(j).getScore()));
                }
                // Map search hits by their ID in order to map Kendra response documents back to hits later
                idToSearchHitMap.put(originalHits.get(j).getId(), originalHits.get(j));
            }

            final RescoreRequest rescoreRequest = new RescoreRequest(queryParserResult.getQueryText(), originalHitsAsDocuments);
            final RescoreResult rescoreResult = kendraClient.rescore(rescoreRequest);

            List<SearchHit> newSearchHits = new ArrayList<>();
            float maxScore = 0;
            for (RescoreResultItem rescoreResultItem : rescoreResult.getResultItems()) {
                SearchHit searchHit = idToSearchHitMap.get(rescoreResultItem.getDocumentId());
                if (searchHit == null) {
                    String errorMessage = String.format(Locale.ENGLISH,
                            "Response from Kendra Intelligent Ranking service references document ID [%s], which does not exist in original results",
                            rescoreResultItem.getDocumentId());
                    logger.error(errorMessage);
                    throw new KendraIntelligentRankingException(errorMessage);
                }
                searchHit.score(rescoreResultItem.getScore());
                maxScore = Math.max(maxScore, rescoreResultItem.getScore());
                newSearchHits.add(searchHit);
            }
            // Add remaining hits to response, which are already sorted by OpenSearch score
            for (int i = numberOfHitsToRerank; i < originalHits.size(); ++i) {
                newSearchHits.add(originalHits.get(i));
            }
            return new SearchHits(newSearchHits.toArray(new SearchHit[0]), hits.getTotalHits(), maxScore);
        } catch (Exception ex) {
            logger.error("Failed to rescore. Returning original search results without rescore.", ex);
            return hits;
        }
    }

    private List<List<String>> getTopPassages(final String queryText, final List<List<String>> passages) {
        List<String> query = textTokenizer.tokenize(queryText);
        BM25Scorer bm25Scorer = new BM25Scorer(BM25_B_VALUE, BM25_K1_VALUE, passages);
        PriorityQueue<PassageScore> pq = new PriorityQueue<>(Comparator.comparingDouble(PassageScore::getScore));

        for (int i = 0; i < passages.size(); i++) {
            double score = bm25Scorer.score(query, passages.get(i));
            pq.offer(new PassageScore(score, i));
            if (pq.size() > TOP_K_PASSAGES) {
                // Maintain heap of top K passages
                pq.poll();
            }
        }

        List<List<String>> topPassages = new ArrayList<>();
        while (!pq.isEmpty()) {
            topPassages.add(passages.get(pq.poll().getIndex()));
        }
        Collections.reverse(topPassages); // reverse to order from highest to lowest score
        return topPassages;
    }
}
