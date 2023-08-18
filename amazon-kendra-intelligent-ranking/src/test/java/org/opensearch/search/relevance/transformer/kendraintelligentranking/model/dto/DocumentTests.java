/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.transformer.kendraintelligentranking.model.dto;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensearch.test.OpenSearchTestCase;

import java.util.List;

public class DocumentTests extends OpenSearchTestCase {


    public static final String JSON_DOCUMENT = "{" +
            "\"Id\":\"docId\"," +
            "\"GroupId\":\"groupIp\"," +
            "\"TokenizedTitle\":[\"this\",\"is\",\"a\",\"title\"]," +
            "\"TokenizedBody\":[\"here\",\"lies\",\"the\",\"body\"]," +
            "\"OriginalScore\":2.71828" +
            "}";
    public static final Document TEST_DOCUMENT = new Document("docId",
            "groupIp",
            List.of("this", "is", "a", "title"),
            List.of("here", "lies", "the", "body"),
            2.71828f);

    public void testSerialization() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonForm = objectMapper.writeValueAsString(TEST_DOCUMENT);
        assertEquals(JSON_DOCUMENT, jsonForm);
    }

    public void testDeserialization() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Document doc = objectMapper.readValue(JSON_DOCUMENT, Document.class);
        assertEquals(TEST_DOCUMENT, doc);
    }

}