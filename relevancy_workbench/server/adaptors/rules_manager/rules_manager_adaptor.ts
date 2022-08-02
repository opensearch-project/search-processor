import _ from 'lodash';
import { REWRITER_TYPE_MAP } from '../../../common';
import { ILegacyScopedClusterClient } from '../../../../../src/core/server';

export class RulesManagerAdaptor {
  // Fetch all rewriters
  fetchAllRewriters = async function (client: ILegacyScopedClusterClient) {
    try {
      console.log('reached the adaptor in backend ####################################');
      const response = await client.callAsCurrentUser('querqy.getRewriter');
      const filteredResponse = response.rewriters.hits.hits.map((doc: any) => {
        return {
          rewriterId: doc._id,
          rewriterType: _.findKey(REWRITER_TYPE_MAP, (v) => v === doc._source.class),
        };
      });
      return filteredResponse;
    } catch (error) {
      throw new Error('Fetch rewriters error:' + error);
    }
  };

  // Fetch a rewriter
  fetchRewriterById = async function (
    client: ILegacyScopedClusterClient,
    rewriterId: string
  ): Promise<{
    rewriterId: string;
    class: string | undefined;
    config: {};
  }> {
    try {
      const response = await client.callAsCurrentUser('querqy.getRewriterById', {
        rewriterId: rewriterId,
      });
      const resp = response.rewriters.hits.hits[0];
      return {
        rewriterId: resp._id,
        class: resp._source.class,
        config: JSON.parse(resp._source.config_v_003),
      };
    } catch (error) {
      throw new Error('Fetch rewriter by Id error:' + error);
    }
  };

  // Create a new rewriter
  createRewriter = async function (
    client: ILegacyScopedClusterClient,
    rewriterId: string,
    newRewriterObject: any
  ) {
    try {
      const response = await client.callAsCurrentUser('querqy.createRewriter', {
        rewriterId: rewriterId,
        body: newRewriterObject,
      });
      return {
        rewriterId: response.put._id,
        forced_refresh: response.forced_refresh,
        shards: response.put._shards,
        result: response.put.result,
        clearcache: response.clearcache,
      };
    } catch (error) {
      throw new Error('Create rewriter error:' + error);
    }
  };

  // Delete a rewriter
  deleteRewriter = async function (client: ILegacyScopedClusterClient, rewriterId: string) {
    try {
      const response = await client.callAsCurrentUser('querqy.deleteRewriter', {
        rewriterId: rewriterId,
      });
      console.log('reached the delete response in backend ####################################');
      return {
        rewriterId: response.delete._id,
        shards: response.delete._shards,
        result: response.delete.result,
        clearcache: response.clearcache,
      };
    } catch (error) {
      throw new Error('Delete rewriter error:' + error);
    }
  };

  // Rename a rewriter
  renameRewriter = async (
    client: ILegacyScopedClusterClient,
    currentRewriterId: string,
    newRewriterId: string
  ) => {
    try {
      const rewriterBody = await this.fetchRewriterById(client, currentRewriterId);
      const deleteResponse = await this.deleteRewriter(client, currentRewriterId);
      const putResponse = await this.createRewriter(
        client,
        newRewriterId,
        _.pickBy(rewriterBody, (v, k) => ['class', 'config'].includes(k))
      );
      return putResponse;

      // const;
    } catch (error) {
      throw new Error('Rename rewriter error:' + error);
    }
  };

  // Clone a rewriter
  cloneRewriter = async (
    client: ILegacyScopedClusterClient,
    currentRewriterId: string,
    newRewriterId: string
  ) => {
    try {
      const rewriterBody = await this.fetchRewriterById(client, currentRewriterId);
      const putResponse = await this.createRewriter(
        client,
        newRewriterId,
        _.pickBy(rewriterBody, (v, k) => ['class', 'config'].includes(k))
      );
      return putResponse;

      // const;
    } catch (error) {
      throw new Error('Rename rewriter error:' + error);
    }
  };
}
