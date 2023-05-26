/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.transformer.personalizeintelligentranking.requestparameter;

import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PersonalizeRequestParametersExtBuilderTests extends OpenSearchTestCase {

    public void testXContentRoundTrip() throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("contextKey", "contextValue");
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters("28", context);
        PersonalizeRequestParametersExtBuilder personalizeExtBuilder = new PersonalizeRequestParametersExtBuilder();
        personalizeExtBuilder.setRequestParameters(requestParameters);
        XContentType xContentType = randomFrom(XContentType.values());
        BytesReference serialized = XContentHelper.toXContent(personalizeExtBuilder, xContentType, true);

        XContentParser parser = createParser(xContentType.xContent(), serialized);

        PersonalizeRequestParametersExtBuilder deserialized =
                PersonalizeRequestParametersExtBuilder.parse(parser);

        assertEquals(personalizeExtBuilder, deserialized);
    }

    public void testStreamRoundTrip() throws IOException {
        PersonalizeRequestParameters requestParameters = new PersonalizeRequestParameters();
        requestParameters.setUserId("28");
        requestParameters.setContext(new HashMap<>());
        PersonalizeRequestParametersExtBuilder personalizeExtBuilder = new PersonalizeRequestParametersExtBuilder();
        personalizeExtBuilder.setRequestParameters(requestParameters);
        BytesStreamOutput bytesStreamOutput = new BytesStreamOutput();
        personalizeExtBuilder.writeTo(bytesStreamOutput);

        PersonalizeRequestParametersExtBuilder deserialized =
                new PersonalizeRequestParametersExtBuilder(bytesStreamOutput.bytes().streamInput());
        assertEquals(personalizeExtBuilder, deserialized);
    }


}
