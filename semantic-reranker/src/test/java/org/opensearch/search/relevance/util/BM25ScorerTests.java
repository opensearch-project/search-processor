/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.opensearch.search.relevance.utils.BM25Scorer;
import org.opensearch.test.OpenSearchTestCase;

public class BM25ScorerTests extends OpenSearchTestCase {

  public void testBM25Scorer() {
    List<String> document1 = Arrays.asList(
        "OpenSearch is a distributed, community-driven, Apache 2.0-licensed, 100% open-source search and analytics suite used for a broad set of use cases like real-time application monitoring, log analytics, and website search. OpenSearch provides a highly scalable system for providing fast access and response to large volumes of data with an integrated visualization tool, OpenSearch Dashboards, that makes it easy for users to explore their data. OpenSearch is powered by the Apache Lucene search library, and it supports a number of search and analytics capabilities such as k-nearest neighbors (KNN) search, SQL, Anomaly Detection, Machine Learning Commons, Trace Analytics, full-text search, and more."
            .split(" "));
    List<String> document2 = Arrays.asList(
        "OpenSearch enables you to easily ingest, secure, search, aggregate, view, and analyze data for a number of use cases such as log analytics, application search, enterprise search, and more. With OpenSearch, you benefit from having a 100% open source product you can use, modify, extend, monetize, and resell however you want. There are a growing number of OpenSearch Project partners that offer a variety of services such as professional support, enhanced features, and managed OpenSearch services. The OpenSearch Project continues to provide a secure, high-quality search and analytics suite with a rich roadmap of new and innovative functionality."
            .split(" "));
    List<String> document3 = Arrays.asList(
        "The sky is blue".split(
            " "));

    BM25Scorer bm25Scorer = new BM25Scorer(0.75, 1.6, Arrays.asList(document1, document2, document3));

    List<String> query1 = Arrays.asList("Apache Lucene search library".split(" "));
    double doc1Score1 = bm25Scorer.score(query1, document1);
    double doc2Score1 = bm25Scorer.score(query1, document2);
    double doc3Score1 = bm25Scorer.score(query1, document3);
    Assert.assertTrue(doc1Score1 > doc2Score1);
    Assert.assertTrue(doc2Score1 > doc3Score1);
    Assert.assertEquals(0, doc3Score1, 0);

    List<String> query2 = Arrays.asList("sky color".split(" "));
    double doc1Score2 = bm25Scorer.score(query2, document1);
    double doc2Score2 = bm25Scorer.score(query2, document2);
    double doc3Score2 = bm25Scorer.score(query2, document3);
    Assert.assertTrue(doc3Score2 > doc1Score2);
    Assert.assertEquals(0, doc1Score2, 0);
    Assert.assertEquals(0, doc2Score2, 0);
  }
}
