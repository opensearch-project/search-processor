/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration;

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
import org.opensearch.search.relevance.transformer.kendraintelligentranking.KendraIntelligentRanker;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.opensearch.search.relevance.configuration.Constants.ORDER;
import static org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.Constants.KENDRA_DEFAULT_DOC_LIMIT;
import static org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankerSettings.BODY_FIELD_VALIDATOR;
import static org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankerSettings.DOC_LIMIT_VALIDATOR;
import static org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankerSettings.TITLE_FIELD_VALIDATOR;

public class KendraIntelligentRankingConfiguration extends ResultTransformerConfiguration {
  private static final ObjectParser<KendraIntelligentRankingConfiguration, Void> PARSER;

  static {
    PARSER = new ObjectParser<KendraIntelligentRankingConfiguration, Void>("kendra_intelligent_ranking_configuration", KendraIntelligentRankingConfiguration::new);
    PARSER.declareInt(TransformerConfiguration::setOrder, TRANSFORMER_ORDER);
    PARSER.declareObject(KendraIntelligentRankingConfiguration::setProperties,
            KendraIntelligentRankingProperties::parse,
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
        settings.getAsList("properties.title_field"),
        settings.getAsInt("properties.doc_limit", KENDRA_DEFAULT_DOC_LIMIT));
  }

  @Override
  public String getTransformerName() {
    return KendraIntelligentRanker.NAME;
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    out.writeInt(this.order);
    this.properties.writeTo(out);
  }

  public static ResultTransformerConfiguration parse(XContentParser parser) throws IOException {
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
    protected static final ParseField DOC_LIMIT = new ParseField(Constants.DOC_LIMIT);

    private static final ObjectParser<KendraIntelligentRankingProperties, Void> PARSER;

    static {
      PARSER = new ObjectParser<>("kendra_intelligent_ranking_configuration", KendraIntelligentRankingProperties::new);
      PARSER.declareStringArray(KendraIntelligentRankingProperties::setBodyFields, BODY_FIELD);
      PARSER.declareStringArray(KendraIntelligentRankingProperties::setTitleFields, TITLE_FIELD);
      PARSER.declareInt(KendraIntelligentRankingProperties::setDocLimit, DOC_LIMIT);
    }

    private List<String> bodyFields;
    private List<String> titleFields;
    private int docLimit;

    public KendraIntelligentRankingProperties() {
      bodyFields = Collections.emptyList();
      titleFields = Collections.emptyList();
      docLimit = KENDRA_DEFAULT_DOC_LIMIT;
    }

    public KendraIntelligentRankingProperties(final List<String> bodyFields,
        final List<String> titleFields, final int docLimit) {
      this.bodyFields = bodyFields;
      this.titleFields = titleFields;
      this.docLimit = docLimit;
    }

    public KendraIntelligentRankingProperties(StreamInput input) throws IOException {
      this.bodyFields = input.readStringList();
      this.bodyFields = input.readStringList();
      this.docLimit = input.readInt();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
      out.writeStringCollection(this.bodyFields);
      out.writeStringCollection(this.titleFields);
      out.writeInt(this.docLimit);
    }

    public static KendraIntelligentRankingProperties parse(XContentParser parser, Void context) throws IOException {
      try {
        KendraIntelligentRankingProperties properties = PARSER.parse(parser, null);
        if (properties != null) {
          BODY_FIELD_VALIDATOR.validate(properties.getBodyFields());
          TITLE_FIELD_VALIDATOR.validate(properties.getTitleFields());
          DOC_LIMIT_VALIDATOR.validate(properties.getDocLimit());
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
      builder.field(DOC_LIMIT.getPreferredName(), this.docLimit);
      return builder.endObject();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      KendraIntelligentRankingProperties properties = (KendraIntelligentRankingProperties) o;

      return (bodyFields == properties.bodyFields) && (titleFields == properties.titleFields) &&
          (docLimit == properties.docLimit);
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

    public int getDocLimit() {
      return this.docLimit;
    }

    public void setDocLimit(final int docLimit) {
      this.docLimit = docLimit;
    }
  }
}
