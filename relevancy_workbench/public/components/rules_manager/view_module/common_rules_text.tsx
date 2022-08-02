import { EuiText } from '@elastic/eui';
import React from 'react';

export const CommonRulesText = () => {
  return (
    <div style={{ maxWidth: '400px', marginLeft: '25px', marginRight: '25px' }}>
      <EuiText>
        <h2>Common Rules</h2>
        <br />
        <p>
          The Common Rules Rewriter uses configurable rules to manipulate the matching and ranking
          of search results depending on the input query. In e-commerce search it is a powerful tool
          for merchandisers to fine-tune search results, especially for high-traffic queries.
        </p>
      </EuiText>
    </div>
  );
};
