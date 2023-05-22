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
import java.util.Objects;

import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.Constants.PERSONALIZE_REQUEST_PARAMETERS;
import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.Constants.USER_ID_PARAMETER;

public class PersonalizeRequestParameters implements Writeable, ToXContentObject {

    private static final ObjectParser<PersonalizeRequestParameters, Void> PARSER;
    private static final ParseField USER_ID = new ParseField(USER_ID_PARAMETER);

    static {
        PARSER = new ObjectParser<>(PERSONALIZE_REQUEST_PARAMETERS, PersonalizeRequestParameters::new);
        PARSER.declareString(PersonalizeRequestParameters::setUserId, USER_ID);
    }

    private String userId;

    public PersonalizeRequestParameters() {}

    public PersonalizeRequestParameters(String userId) {
        this.userId = userId;
    }

    public PersonalizeRequestParameters(StreamInput input) throws IOException {
        this.userId = input.readString();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(this.userId);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.field(USER_ID.getPreferredName(), this.userId);
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
        return userId.equals(config.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
