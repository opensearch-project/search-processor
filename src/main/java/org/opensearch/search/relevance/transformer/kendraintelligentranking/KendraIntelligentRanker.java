/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking;

import static org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.Constants.BODY_FIELD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.common.settings.Setting;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.SearchService;
import org.opensearch.search.relevance.configuration.ResultTransformerConfiguration;
import org.opensearch.search.relevance.transformer.ResultTransformer;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraHttpClient;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankingConfiguration;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.KendraIntelligentRankingException;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.PassageScore;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.Document;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreRequest;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreResult;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto.RescoreResultItem;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.BM25Scorer;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.QueryParser;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.QueryParser.QueryParserResult;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.SlidingWindowTextSplitter;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.preprocess.TextTokenizer;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankerSettings;

public class KendraIntelligentRanker implements ResultTransformer {

  private static final int PASSAGE_SIZE_LIMIT = 600;
  private static final int SLIDING_WINDOW_STEP = PASSAGE_SIZE_LIMIT - 50;
  private static final int MAXIMUM_PASSAGES = 10;
  private static final double BM25_B_VALUE = 0.75;
  private static final double BM25_K1_VALUE = 1.6;
  private static final int TOP_K_PASSAGES = 3;
  private static final int KENDRA_DOC_LIMIT = 25;

  private static final Logger logger = LogManager.getLogger(KendraIntelligentRanker.class);

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

  /**
   * Check if search request is eligible for rescore
   * @param request Search Request
   * @return boolean decision on whether to re-rank
   */
  @Override
  public boolean shouldTransform(final SearchRequest request, final ResultTransformerConfiguration configuration) {
    if (request.source() == null) {
      return false;
    }

    // Skip if there is scroll, sorting, or the start of the page is greater than the document limit for Kendra Ranking
    if (request.scroll() != null ||
        (request.source().sorts() != null && !request.source().sorts().isEmpty()) ||
        request.source().from() >= KENDRA_DOC_LIMIT) {
      return false;
    }
    return true;
  }

  @Override
  public SearchRequest preprocessRequest(final SearchRequest request,
      final SearchRequest originalSearchRequest,
      final ResultTransformerConfiguration configuration) {
    // Source is returned in response hits by default. If disabled by the user, overwrite and enable
    // in order to access document contents for reranking, then suppress at response time.
    if (request.source().fetchSource() != null && !request.source().fetchSource().fetchSource()) {
      originalSearchRequest.source().fetchSource(request.source().fetchSource());
      request.source().fetchSource(true);
    }

    int from = request.source().from() == -1 ? SearchService.DEFAULT_FROM : request.source().from();
    int size = request.source().size() == -1 ? SearchService.DEFAULT_SIZE : request.source().size();
    originalSearchRequest.source().from(from);
    originalSearchRequest.source().size(size);

    int sizeOverride = Math.max(KENDRA_DOC_LIMIT, from + size);
    request.source().from(0);
    request.source().size(sizeOverride);
    return request;
  }

  /**
   *
   * @param hits Search hits to rerank with respect to query
   * @param request Search request
   * @return SearchHits reranked search hits
   */
  @Override
  public SearchHits transform(final SearchHits hits,
      final SearchRequest request,
      final ResultTransformerConfiguration configuration) {
    KendraIntelligentRankingConfiguration kendraConfig = (KendraIntelligentRankingConfiguration) configuration;
    QueryParserResult queryParserResult = queryParser.parse(
        request.source().query(),
        kendraConfig.getProperties().getBodyFields(),
        kendraConfig.getProperties().getTitleFields());
    if (queryParserResult == null) {
      return hits;
    }
    try {
      List<SearchHit> originalHits = Arrays.asList(hits.getHits());
      final int numberOfHitsToRerank = Math.min(originalHits.size(), KENDRA_DOC_LIMIT);
      List<Document> originalHitsAsDocuments = new ArrayList<>();
      Map<String, SearchHit> idToSearchHitMap = new HashMap<>();
      for (int j = 0; j < numberOfHitsToRerank; ++j) {
        Map<String, Object> docSourceMap = originalHits.get(j).getSourceAsMap();
        SlidingWindowTextSplitter textSplitter = new SlidingWindowTextSplitter(PASSAGE_SIZE_LIMIT, SLIDING_WINDOW_STEP, MAXIMUM_PASSAGES);
        String bodyFieldName = queryParserResult.getBodyFieldName();
        String titleFieldName = queryParserResult.getTitleFieldName();
        if (docSourceMap.get(bodyFieldName) == null) {
          String errorMessage = String.format(Locale.ENGLISH,
              "Kendra Intelligent Ranking cannot be applied when documents are missing %s [%s]. Document ID [].",
              BODY_FIELD, bodyFieldName, originalHits.get(j).getId());
          logger.error(errorMessage);
          throw new KendraIntelligentRankingException(errorMessage);
        }
        List<String> splitPassages = textSplitter.split(docSourceMap.get(bodyFieldName).toString());
        List<List<String>> topPassages = getTopPassages(queryParserResult.getQueryText(), splitPassages);
        List<String> tokenizedTitle = null;
        if (titleFieldName != null && docSourceMap.get(titleFieldName) != null) {
          tokenizedTitle = textTokenizer.tokenize(docSourceMap.get(queryParserResult.getTitleFieldName()).toString());
          // If tokens list is empty, use null
          if (tokenizedTitle.isEmpty()) {
            tokenizedTitle = null;
          }
        }
        for (int i = 0; i < topPassages.size(); ++i) {
          originalHitsAsDocuments.add(
              new Document(originalHits.get(j).getId() + "@" + (i + 1), originalHits.get(j).getId(), tokenizedTitle, topPassages.get(i), originalHits.get(j).getScore())
          );
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
      for (int i = KENDRA_DOC_LIMIT; i < originalHits.size(); ++i) {
        newSearchHits.add(originalHits.get(i));
      }
      return new SearchHits(newSearchHits.toArray(new SearchHit[newSearchHits.size()]), hits.getTotalHits(), maxScore);
    } catch (Exception ex) {
      logger.error("Failed to rescore. Returning original search results without rescore.", ex);
      return hits;
    }
  }

  private List<List<String>> getTopPassages(final String queryText, final List<String> splitPassages) {
    List<String> query = textTokenizer.tokenize(queryText);
    List<List<String>> passages = textTokenizer.tokenize(splitPassages);
    BM25Scorer bm25Scorer = new BM25Scorer(BM25_B_VALUE, BM25_K1_VALUE, passages);
    PriorityQueue<PassageScore> pq = new PriorityQueue<>(Comparator.comparingDouble(x -> x.getScore()));

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
