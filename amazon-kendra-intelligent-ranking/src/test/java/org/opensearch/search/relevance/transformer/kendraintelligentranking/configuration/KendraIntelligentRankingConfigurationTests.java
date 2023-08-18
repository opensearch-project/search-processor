/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration;

import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.search.relevance.configuration.Constants;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.List;

public class KendraIntelligentRankingConfigurationTests extends OpenSearchTestCase {
    public void testParseWithNullParserAndContext() throws IOException {
        expectThrows(NullPointerException.class, () -> KendraIntelligentRankingConfiguration.parse(null));
    }

    public void testSerializeToXContentRoundtrip() throws IOException {
        KendraIntelligentRankingConfiguration expected = getKendraIntelligentRankingConfiguration();

        XContentType xContentType = randomFrom(XContentType.values());
        BytesReference serialized = XContentHelper.toXContent(expected, xContentType, true);

        XContentParser parser = createParser(xContentType.xContent(), serialized);

        KendraIntelligentRankingConfiguration deserialized =
                KendraIntelligentRankingConfiguration.parse(parser);
        assertEquals(expected, deserialized);
    }

    public void testSerializeToStreamRoundtrip() throws IOException {
        KendraIntelligentRankingConfiguration expected = getKendraIntelligentRankingConfiguration();
        BytesStreamOutput bytesStreamOutput = new BytesStreamOutput();
        expected.writeTo(bytesStreamOutput);

        KendraIntelligentRankingConfiguration deserialized =
                new KendraIntelligentRankingConfiguration(bytesStreamOutput.bytes().streamInput());
        assertEquals(expected, deserialized);
    }

    private static KendraIntelligentRankingConfiguration getKendraIntelligentRankingConfiguration() {
        int order = randomInt(10) + 1;
        int docLimit = randomInt( Integer.MAX_VALUE - 25) + 25;
        KendraIntelligentRankingConfiguration.KendraIntelligentRankingProperties properties =
                new KendraIntelligentRankingConfiguration.KendraIntelligentRankingProperties(List.of("body1"),
                        List.of("title1"), docLimit);
        return new KendraIntelligentRankingConfiguration(order, properties);
    }

    public void testReadFromSettings() throws IOException {
        int order = randomInt(10);
        int docLimit = randomInt(100) + 25;
        String bodyField = "body1";
        String titleField = "title1";
        Settings settings = Settings.builder()
                .put(Constants.ORDER,  order)
                .put("properties.doc_limit", docLimit)
                .put("properties.body_field", bodyField)
                .putList("properties.title_field", titleField)
                .build();

        KendraIntelligentRankingConfiguration expected = new KendraIntelligentRankingConfiguration(order,
                new KendraIntelligentRankingConfiguration.KendraIntelligentRankingProperties(List.of(bodyField),
                        List.of(titleField), docLimit));

        KendraIntelligentRankingConfiguration actual = new KendraIntelligentRankingConfiguration(settings);

        assertEquals(expected, actual);
    }
}