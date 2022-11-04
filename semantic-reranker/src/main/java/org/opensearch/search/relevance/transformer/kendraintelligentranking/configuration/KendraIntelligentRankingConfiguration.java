/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration;

import static org.opensearch.search.relevance.configuration.Constants.ORDER;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.opensearch.common.ParseField;
import org.opensearch.common.ParsingException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.ObjectParser;
import org.opensearch.common.xcontent.ToXContentObject;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.search.relevance.configuration.ResultTransformerConfiguration;
import org.opensearch.search.relevance.configuration.TransformerConfiguration;
import org.opensearch.search.relevance.transformer.ResultTransformerType;

public class KendraIntelligentRankingConfiguration extends ResultTransformerConfiguration {
  private static final ObjectParser<KendraIntelligentRankingConfiguration, Void> PARSER;

  static {
    PARSER = new ObjectParser<KendraIntelligentRankingConfiguration, Void>("kendra_intelligent_ranking_configuration", KendraIntelligentRankingConfiguration::new);
    PARSER.declareInt(TransformerConfiguration::setOrder, TRANSFORMER_ORDER);
    PARSER.declareObject(KendraIntelligentRankingConfiguration::setProperties,
        (p, c) -> KendraIntelligentRankingProperties.parse(p, c),
        TRANSFORMER_PROPERTIES);
  }

  private KendraIntelligentRankingProperties properties;

  public KendraIntelligentRankingConfiguration() {}

  public KendraIntelligentRankingConfiguration(final int order, final KendraIntelligentRankingProperties properties) {
    this.order = order;
    this.properties = properties;
  }

  public KendraIntelligentRankingConfiguration(StreamInput input) throws IOException {
    this.order = input.readInt();
    this.properties = new KendraIntelligentRankingProperties(input);
  }

  public KendraIntelligentRankingConfiguration(Settings settings) {
    this.order = settings.getAsInt(ORDER, 0);
    this.properties = new KendraIntelligentRankingProperties(
        settings.getAsList("properties.body_field"),
        settings.getAsList("properties.title_field"));
  }

  @Override
  public ResultTransformerType getType() {
    return ResultTransformerType.KENDRA_INTELLIGENT_RANKING;
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    out.writeInt(this.order);
    this.properties.writeTo(out);
  }

  public static ResultTransformerConfiguration parse(XContentParser parser, Void context) throws IOException {
    try {
      KendraIntelligentRankingConfiguration configuration = PARSER.parse(parser, null);
      if (configuration != null && configuration.getOrder() <= 0) {
        throw new ParsingException(parser.getTokenLocation(),
            "Failed to parse value [" + configuration.getOrder() + "] for Transformer order, must be >= 1");
      }
      return configuration;
    } catch (IllegalArgumentException iae) {
      throw new ParsingException(parser.getTokenLocation(), iae.getMessage(), iae);
    }
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject();
    builder.field(TRANSFORMER_ORDER.getPreferredName(), this.order);
    builder.field(TRANSFORMER_PROPERTIES.getPreferredName(), this.properties);
    return builder.endObject();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    KendraIntelligentRankingConfiguration config = (KendraIntelligentRankingConfiguration) o;

    if (order != config.order) return false;
    return properties.equals(config.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(order, properties);
  }

  public void setProperties(KendraIntelligentRankingProperties properties) {
    this.properties = properties;
  }

  public KendraIntelligentRankingProperties getProperties() {
    return this.properties;
  }

  public static class KendraIntelligentRankingProperties implements Writeable, ToXContentObject {
    protected static final ParseField BODY_FIELD = new ParseField(Constants.BODY_FIELD);
    protected static final ParseField TITLE_FIELD = new ParseField(Constants.TITLE_FIELD);

    private static final ObjectParser<KendraIntelligentRankingProperties, Void> PARSER;

    static {
      PARSER = new ObjectParser<>("kendra_intelligent_ranking_configuration", KendraIntelligentRankingProperties::new);
      PARSER.declareStringArray(KendraIntelligentRankingProperties::setBodyFields, BODY_FIELD);
      PARSER.declareStringArray(KendraIntelligentRankingProperties::setTitleFields, TITLE_FIELD);
    }

    private List<String> bodyFields;
    private List<String> titleFields;

    public KendraIntelligentRankingProperties() {}

    public KendraIntelligentRankingProperties(final List<String> bodyFields, final List<String> titleFields) {
      this.bodyFields = bodyFields;
      this.titleFields = titleFields;
    }

    public KendraIntelligentRankingProperties(StreamInput input) throws IOException {
      this.bodyFields = input.readStringList();
      this.bodyFields = input.readStringList();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
      out.writeStringCollection(this.bodyFields);
      out.writeStringCollection(this.titleFields);
    }

    public static KendraIntelligentRankingProperties parse(XContentParser parser, Void context) throws IOException {
      try {
        KendraIntelligentRankingProperties properties = PARSER.parse(parser, null);
        if (properties != null) {
          if (properties.getBodyFields() != null && !properties.getBodyFields().isEmpty() && properties.getBodyFields().size() > 1) {
            throw new ParsingException(parser.getTokenLocation(),
                "[" + BODY_FIELD + "] can have at most 1 element");
          }
          if (properties.getTitleFields() != null && !properties.getTitleFields().isEmpty() && properties.getTitleFields().size() > 1) {
            throw new ParsingException(parser.getTokenLocation(),
                "[" + TITLE_FIELD + "] can have at most 1 element");
          }
        }
        return properties;
      } catch (IllegalArgumentException iae) {
        throw new ParsingException(parser.getTokenLocation(), iae.getMessage(), iae);
      }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
      builder.startObject();
      builder.field(BODY_FIELD.getPreferredName(), this.bodyFields);
      builder.field(TITLE_FIELD.getPreferredName(), this.titleFields);
      return builder.endObject();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      KendraIntelligentRankingProperties properties = (KendraIntelligentRankingProperties) o;

      if (bodyFields != properties.bodyFields) return false;
      return (titleFields == properties.titleFields);
    }

    @Override
    public int hashCode() {
      return Objects.hash(bodyFields, titleFields);
    }

    public List<String> getBodyFields() {
      return this.bodyFields;
    }

    public void setBodyFields(final List<String> bodyFields) {
      this.bodyFields = bodyFields;
    }

    public List<String> getTitleFields() {
      return this.titleFields;
    }

    public void setTitleFields(final List<String> titleFields) {
      this.titleFields = titleFields;
    }
  }
}
