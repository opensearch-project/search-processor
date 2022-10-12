/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.control;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.opensearch.common.ParseField;
import org.opensearch.common.ParsingException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.ObjectParser;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.search.SearchExtBuilder;
import org.opensearch.search.relevance.constants.Constants;

public class KendraSearchExtBuilder extends SearchExtBuilder {
  public static final String NAME = Constants.PLUGIN_NAME;

  private static final ObjectParser<KendraSearchExtBuilder, Void> PARSER;
  private static final ParseField RANKER_ENABLED = new ParseField(Constants.ENABLED);
  private static final ParseField BODY_FIELD = new ParseField(Constants.BODY_FIELD);

  static {
    PARSER = new ObjectParser<>(NAME, KendraSearchExtBuilder::new);
    PARSER.declareBoolean(KendraSearchExtBuilder::setRankerEnabled, RANKER_ENABLED);
    PARSER.declareStringArray(KendraSearchExtBuilder::setBodyField, BODY_FIELD);
  }

  private boolean rankerEnabled;
  private List<String> bodyField;

  public KendraSearchExtBuilder() {}

  public KendraSearchExtBuilder(StreamInput input) throws IOException {
    this.rankerEnabled = input.readBoolean();
    this.bodyField = input.readStringList();
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    out.writeBoolean(rankerEnabled);
    out.writeStringCollection(bodyField);
  }

  @Override
  public String getWriteableName() {
    return NAME;
  }

  public static KendraSearchExtBuilder parse(XContentParser parser) throws IOException {
    try {
      KendraSearchExtBuilder ext = PARSER.parse(parser, null);
      if ((ext.bodyField != null) && ext.bodyField.size() > 1) {
        throw new IllegalArgumentException("[" + BODY_FIELD + "] can have at most 1 element");
      }
      return ext;
    } catch(IllegalArgumentException iae) {
      throw new ParsingException(parser.getTokenLocation(), iae.getMessage(), iae);
    }
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject(NAME);
    builder.field(RANKER_ENABLED.getPreferredName(), this.rankerEnabled);
    builder.field(BODY_FIELD.getPreferredName(), this.bodyField);
    return builder.endObject();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof KendraSearchExtBuilder)) {
      return false;
    }
    KendraSearchExtBuilder o = (KendraSearchExtBuilder) obj;
    return Objects.equals(this.rankerEnabled, o.rankerEnabled) && Objects.equals(this.bodyField, o.bodyField);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getClass(), this.rankerEnabled);
  }

  public boolean isRankerEnabled() {
    return this.rankerEnabled;
  }

  public void setRankerEnabled(boolean rankerEnabled) {
    this.rankerEnabled = rankerEnabled;
  }

  public List<String> bodyField() { return this.bodyField; }

  public void setBodyField(final List<String> bodyField) { this.bodyField = bodyField; }
}
