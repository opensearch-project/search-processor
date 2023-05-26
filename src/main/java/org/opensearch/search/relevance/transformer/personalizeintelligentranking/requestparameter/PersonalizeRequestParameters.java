/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.core.ParseField;
import org.opensearch.core.xcontent.ObjectParser;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.Constants.USER_ID_PARAMETER;
import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.Constants.CONTEXT_PARAMETER;
import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.Constants.PERSONALIZE_REQUEST_PARAMETERS;

public class PersonalizeRequestParameters implements Writeable, ToXContentObject {

    private static final ObjectParser<PersonalizeRequestParameters, Void> PARSER;
    private static final ParseField USER_ID = new ParseField(USER_ID_PARAMETER);
    private static final ParseField CONTEXT = new ParseField(CONTEXT_PARAMETER);

    static {
        PARSER = new ObjectParser<>(PERSONALIZE_REQUEST_PARAMETERS, PersonalizeRequestParameters::new);
        PARSER.declareString(PersonalizeRequestParameters::setUserId, USER_ID);
        PARSER.declareObject(PersonalizeRequestParameters::setContext,(XContentParser p, Void c) -> {
            try {
                return p.map();
            } catch (IOException e) {
                throw new IllegalArgumentException("Error parsing Personalize context from request parameters", e);
            }
        }, CONTEXT);
    }

    private String userId;

    private Map<String, Object> context;

    public PersonalizeRequestParameters() {}

    public PersonalizeRequestParameters(String userId, Map<String, Object> context) {
        this.userId = userId;
        this.context = context;
    }

    public PersonalizeRequestParameters(StreamInput input) throws IOException {
        this.userId = input.readString();
        this.context = input.readMap();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(this.userId);
        out.writeMap(this.context);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(USER_ID.getPreferredName(), this.userId);
        return builder.field(CONTEXT.getPreferredName(), this.context);
    }

    public static PersonalizeRequestParameters parse(XContentParser parser) throws IOException {
        PersonalizeRequestParameters requestParameters = PARSER.parse(parser, null);
        return requestParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PersonalizeRequestParameters config = (PersonalizeRequestParameters) o;

        if (!userId.equals(config.userId)) return false;
        if (context.size() != config.getContext().size()) return false;
        return userId.equals(config.userId) && context.equals(config.getContext());
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, context);
    }
}
