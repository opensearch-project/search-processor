/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.configuration;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentParser;

import java.io.IOException;

public interface ResultTransformerConfigurationFactory {
    String getName();

    /**
     * Build configuration based on index settings
     * @param indexSettings a set of index settings under a group scoped based on this result transformer's name.
     * @return a transformer configuration based on the passed settings.
     */
    ResultTransformerConfiguration configureFromIndexSettings(Settings indexSettings);

    /**
     * Build configuration from search request.
     * @param parser a search request parser pointing to a child node under result transformers scoped to this result
     *               transformer's name.
     * @return a transformer configuration based on the parameters specified in the search request.
     */
    ResultTransformerConfiguration configureFromSearchRequest(XContentParser parser) throws IOException;

    /**
     * Build configuration from a serialized stream.
     * @param streamInput a serialized representation of transformer configuration.
     * @return configuration the deserialized transformer configuration.
     */
    ResultTransformerConfiguration configureFromStream(StreamInput streamInput) throws IOException;

}
