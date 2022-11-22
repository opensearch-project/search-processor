/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.configuration;

import static org.opensearch.search.relevance.configuration.Constants.SEARCH_CONFIGURATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.ParseField;
import org.opensearch.common.ParsingException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.search.SearchExtBuilder;
import org.opensearch.search.relevance.transformer.ResultTransformerType;
import org.opensearch.search.relevance.transformer.TransformerType;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankingConfiguration;

public class SearchConfigurationExtBuilder extends SearchExtBuilder {
  public static final String NAME = SEARCH_CONFIGURATION;

  private static final ParseField RESULT_TRANSFORMER = new ParseField(TransformerType.RESULT_TRANSFORMER.toString());
  private static final ParseField KENDRA_INTELLIGENT_RANKING = new ParseField(ResultTransformerType.KENDRA_INTELLIGENT_RANKING.toString());

  private List<ResultTransformerConfiguration> resultTransformerConfigurations = new ArrayList<>();

  public SearchConfigurationExtBuilder() {}

  public SearchConfigurationExtBuilder(StreamInput input) throws IOException {
    ResultTransformerConfiguration cfg1 = input.readOptionalWriteable(KendraIntelligentRankingConfiguration::new);
    resultTransformerConfigurations.add(cfg1);
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    for (ResultTransformerConfiguration config : resultTransformerConfigurations) {
      out.writeOptionalWriteable(config);
    }
  }

  @Override
  public String getWriteableName() {
    return NAME;
  }

  public static SearchConfigurationExtBuilder parse(XContentParser parser) throws IOException {
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
          while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
              currentFieldName = parser.currentName();
            }
            if (KENDRA_INTELLIGENT_RANKING.match(currentFieldName,
                parser.getDeprecationHandler())) {
              extBuilder.addResultTransformer(
                  KendraIntelligentRankingConfiguration.parse(parser, null));
            } else {
              throw new IllegalArgumentException(
                  "Unrecognized Result Transformer type [" + currentFieldName + "]");
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
    builder.startObject(NAME);
    for (ResultTransformerConfiguration config : resultTransformerConfigurations) {
      builder.field(config.getType().toString(), config);
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
    return (this.resultTransformerConfigurations.size() == o.resultTransformerConfigurations.size() &&
        this.resultTransformerConfigurations.containsAll(o.resultTransformerConfigurations) &&
        o.resultTransformerConfigurations.containsAll(this.resultTransformerConfigurations)) ;
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

  public void addResultTransformer(final ResultTransformerConfiguration resultTransformerConfiguration) {
    this.resultTransformerConfigurations.add(resultTransformerConfiguration);
  }
}
