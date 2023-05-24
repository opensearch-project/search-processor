/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.search.SearchExtBuilder;

import java.io.IOException;
import java.util.Objects;

import static org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter.Constants.PERSONALIZE_REQUEST_PARAMETERS;

public class PersonalizeRequestParametersExtBuilder extends SearchExtBuilder {
    private static final Logger logger = LogManager.getLogger(PersonalizeRequestParametersExtBuilder.class);
    public static final String NAME = PERSONALIZE_REQUEST_PARAMETERS;
    private PersonalizeRequestParameters requestParameters;

    public PersonalizeRequestParametersExtBuilder() {}

    public PersonalizeRequestParametersExtBuilder(StreamInput input) throws IOException {
        requestParameters = new PersonalizeRequestParameters(input);
    }

    public PersonalizeRequestParameters getRequestParameters() {
        return requestParameters;
    }

    public void setRequestParameters(PersonalizeRequestParameters requestParameters) {
        this.requestParameters = requestParameters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass(), this.requestParameters);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PersonalizeRequestParametersExtBuilder)) {
            return false;
        }
        PersonalizeRequestParametersExtBuilder o = (PersonalizeRequestParametersExtBuilder) obj;
        return this.requestParameters.equals(o.requestParameters);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        requestParameters.writeTo(out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.value(requestParameters);
    }

    public static PersonalizeRequestParametersExtBuilder parse(XContentParser parser) throws IOException{
        PersonalizeRequestParametersExtBuilder extBuilder = new PersonalizeRequestParametersExtBuilder();
        PersonalizeRequestParameters requestParameters = PersonalizeRequestParameters.parse(parser);
        extBuilder.setRequestParameters(requestParameters);
        return extBuilder;
    }
}
