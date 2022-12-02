/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.configuration;

import org.opensearch.common.ParseField;
import org.opensearch.common.ParsingException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.search.SearchExtBuilder;
import org.opensearch.search.relevance.transformer.TransformerType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.opensearch.search.relevance.configuration.Constants.SEARCH_CONFIGURATION;

public class SearchConfigurationExtBuilder extends SearchExtBuilder {
    public static final String NAME = SEARCH_CONFIGURATION;

    private static final ParseField RESULT_TRANSFORMER = new ParseField(TransformerType.RESULT_TRANSFORMER.toString());

    private List<ResultTransformerConfiguration> resultTransformerConfigurations = new ArrayList<>();

    public SearchConfigurationExtBuilder() {
    }

    public SearchConfigurationExtBuilder(StreamInput input, Map<String, ResultTransformerConfigurationFactory> resultTransformerMap) throws IOException {
        int numTransformers = input.readInt();
        for (int i = 0; i < numTransformers; i++) {
            String transformerName = input.readString();
            ResultTransformerConfigurationFactory transformer = resultTransformerMap.get(transformerName);
            if (transformer == null) {
                throw new IllegalStateException("Unknown result transformer " + transformerName);
            }
            resultTransformerConfigurations.add(transformer.configureFromStream(input));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeInt(resultTransformerConfigurations.size());
        for (ResultTransformerConfiguration config : resultTransformerConfigurations) {
            out.writeString(config.getTransformerName());
            config.writeTo(out);
        }
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    public static SearchConfigurationExtBuilder parse(XContentParser parser,
                                                      Map<String, ResultTransformerConfigurationFactory> resultTransformerMap) throws IOException {
        SearchConfigurationExtBuilder extBuilder = new SearchConfigurationExtBuilder();
        XContentParser.Token token = parser.currentToken();
        String currentFieldName = null;
        if (token != XContentParser.Token.START_OBJECT && (token = parser.nextToken()) != XContentParser.Token.START_OBJECT) {
            throw new ParsingException(
                    parser.getTokenLocation(),
                    "Expected [" + XContentParser.Token.START_OBJECT + "] but found [" + token + "]",
                    parser.getTokenLocation()
            );
        }
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                if (RESULT_TRANSFORMER.match(currentFieldName, parser.getDeprecationHandler())) {
                    currentFieldName = null;
                    while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                        if (token == XContentParser.Token.FIELD_NAME) {
                            currentFieldName = parser.currentName();
                        } else if (currentFieldName != null) {
                            if (resultTransformerMap.containsKey(currentFieldName)) {
                                ResultTransformerConfiguration configuration =
                                        resultTransformerMap.get(currentFieldName).configureFromSearchRequest(parser);
                                extBuilder.addResultTransformer(configuration);
                            } else {
                                throw new IllegalArgumentException(
                                        "Unrecognized Result Transformer type [" + currentFieldName + "]");
                            }
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Unrecognized Transformer type [" + currentFieldName + "]");
                }
            } else {
                throw new ParsingException(
                        parser.getTokenLocation(),
                        "Unknown key for a " + token + " in [" + currentFieldName + "].",
                        parser.getTokenLocation()
                );
            }
        }
        return extBuilder;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(RESULT_TRANSFORMER.getPreferredName());
        for (ResultTransformerConfiguration config : resultTransformerConfigurations) {
            builder.field(config.getTransformerName(), config);
        }
        return builder.endObject();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SearchConfigurationExtBuilder)) {
            return false;
        }
        SearchConfigurationExtBuilder o = (SearchConfigurationExtBuilder) obj;
        HashSet<ResultTransformerConfiguration> myConfigurations = new HashSet<>(this.resultTransformerConfigurations);
        HashSet<ResultTransformerConfiguration> otherConfigurations = new HashSet<>(o.resultTransformerConfigurations);
        return (this.resultTransformerConfigurations.size() == o.resultTransformerConfigurations.size() &&
                myConfigurations.equals(otherConfigurations));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass(), this.resultTransformerConfigurations);
    }

    public SearchConfigurationExtBuilder setResultTransformers(final List<ResultTransformerConfiguration> resultTransformerConfigurations) {
        this.resultTransformerConfigurations = resultTransformerConfigurations;
        return this;
    }

    public List<ResultTransformerConfiguration> getResultTransformers() {
        return this.resultTransformerConfigurations;
    }

    public SearchConfigurationExtBuilder addResultTransformer(final ResultTransformerConfiguration resultTransformerConfiguration) {
        this.resultTransformerConfigurations.add(resultTransformerConfiguration);
        return this;
    }
}
