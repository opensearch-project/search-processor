/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.transformer.kendraintelligentranking.model;

import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class KendraIntelligentRankingExceptionTests extends OpenSearchTestCase {

    public void testSerializationRoundtrip() throws IOException {
        KendraIntelligentRankingException expected = new KendraIntelligentRankingException("This is an error message");
        BytesStreamOutput bytesStreamOutput = new BytesStreamOutput();
        expected.writeTo(bytesStreamOutput);

        KendraIntelligentRankingException deserialized =
                new KendraIntelligentRankingException(bytesStreamOutput.bytes().streamInput());
        assertEquals(expected.getMessage(), deserialized.getMessage());
    }
}