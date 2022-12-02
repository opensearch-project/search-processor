/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.configuration;

import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class SearchConfigurationExtBuilderTests extends OpenSearchTestCase {
    private static final String TRANSFORMER_NAME = "mock_transformer";

    private static class MockResultTransformerConfiguration extends ResultTransformerConfiguration {
        private final String configuredValue;

        public MockResultTransformerConfiguration(String configuredValue) {
            this.configuredValue = configuredValue;
        }

        @Override
        public String getTransformerName() {
            return TRANSFORMER_NAME;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(configuredValue);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field("configuredValue", configuredValue);
            return builder.endObject();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MockResultTransformerConfiguration that = (MockResultTransformerConfiguration) o;
            return configuredValue.equals(that.configuredValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(configuredValue);
        }

        @Override
        public String toString() {
            return "MockResultTransformerConfiguration{" +
                    "configuredValue='" + configuredValue + '\'' +
                    '}';
        }
    }

    private static ResultTransformerConfigurationFactory MOCK_RESULT_TRANSFORMER_CONFIGURATION_FACTORY = new ResultTransformerConfigurationFactory() {
        @Override
        public String getName() {
            return TRANSFORMER_NAME;
        }

        @Override
        public ResultTransformerConfiguration configureFromIndexSettings(Settings indexSettings) {
            return null;
        }

        @Override
        public ResultTransformerConfiguration configureFromSearchRequest(XContentParser parser) throws IOException {
            XContentParser.Token token = parser.nextToken();
            assertSame(XContentParser.Token.FIELD_NAME, token);
            assertEquals("configuredValue", parser.currentName());
            token = parser.nextToken();
            assertSame(XContentParser.Token.VALUE_STRING, token);
            String configuredValue = parser.text();
            return new MockResultTransformerConfiguration(configuredValue);
        }

        @Override
        public ResultTransformerConfiguration configureFromStream(StreamInput streamInput) throws IOException {
            return new MockResultTransformerConfiguration(streamInput.readString());
        }
    };
    public static final Map<String, ResultTransformerConfigurationFactory> RESULT_TRANSFORMER_CONFIGURATION_FACTORY_MAP = Map.of(TRANSFORMER_NAME, MOCK_RESULT_TRANSFORMER_CONFIGURATION_FACTORY);


    public void testXContentRoundTrip() throws IOException {
        MockResultTransformerConfiguration configuration = new MockResultTransformerConfiguration(randomUnicodeOfLength(10));
        SearchConfigurationExtBuilder searchConfigurationExtBuilder = new SearchConfigurationExtBuilder()
                .addResultTransformer(configuration);
        XContentType xContentType = randomFrom(XContentType.values());
        BytesReference serialized = XContentHelper.toXContent(searchConfigurationExtBuilder, xContentType, true);

        XContentParser parser = createParser(xContentType.xContent(), serialized);

        SearchConfigurationExtBuilder deserialized =
                SearchConfigurationExtBuilder.parse(parser, RESULT_TRANSFORMER_CONFIGURATION_FACTORY_MAP);
        assertEquals(searchConfigurationExtBuilder, deserialized);
    }

    public void testStreamRoundTrip() throws IOException {
        MockResultTransformerConfiguration configuration = new MockResultTransformerConfiguration(randomUnicodeOfLength(10));
        SearchConfigurationExtBuilder searchConfigurationExtBuilder = new SearchConfigurationExtBuilder()
                .addResultTransformer(configuration);
        BytesStreamOutput bytesStreamOutput = new BytesStreamOutput();
        searchConfigurationExtBuilder.writeTo(bytesStreamOutput);

        SearchConfigurationExtBuilder deserialized = new SearchConfigurationExtBuilder(bytesStreamOutput.bytes().streamInput(),
                RESULT_TRANSFORMER_CONFIGURATION_FACTORY_MAP);
        assertEquals(searchConfigurationExtBuilder, deserialized);
    }
}