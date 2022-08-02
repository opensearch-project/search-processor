/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import { CoreStart } from '../../../../src/core/public';

export default class DSLService {
  private http;
  constructor(http: CoreStart['http']) {
    this.http = http;
  }
  fetch = async (url: string, request: any) => {
    return this.http
      .post(`${url}`, {
        body: JSON.stringify(request),
      })
      .catch((error) => console.error(error));
  };

  fetchIndices = async () => {
    return this.http
      .get(`${DSL_BASE}${DSL_CAT}`, {
        query: {
          format: 'json',
        },
      })
      .catch((error) => console.error(error));
  };

  fetchFields = async (index: string) => {
    return this.http
      .get(`${DSL_BASE}${DSL_MAPPING}`, {
        query: {
          index,
        },
      })
      .catch((error) => console.error(error));
  };
}
