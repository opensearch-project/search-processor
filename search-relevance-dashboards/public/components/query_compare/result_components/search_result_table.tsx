import { EuiSpacer, EuiSplitPanel, EuiText } from '@elastic/eui';
import React from 'react';
import { ResultGridComponent } from './result_grid_component';

interface SearchResultTableProps {
  querqyResult1: any;
  querqyResult2: any;
}

export const SearchResultTable = ({ querqyResult1, querqyResult2 }: SearchResultTableProps) => {
  return (
    <EuiSplitPanel.Outer direction="row" hasShadow={false} hasBorder={false}>
      <EuiSplitPanel.Inner style={{ minHeight: '500px' }}>
        <EuiText>
          <h3>Search Result 1</h3>
        </EuiText>
        <EuiSpacer size="l" />
        <ResultGridComponent querqyResult={querqyResult1} />
      </EuiSplitPanel.Inner>
      <EuiSplitPanel.Inner style={{ minHeight: '500px' }}>
        <EuiText>
          <h3>Search Result 2</h3>
        </EuiText>
        <EuiSpacer size="l" />
        <ResultGridComponent querqyResult={querqyResult2} />
      </EuiSplitPanel.Inner>
    </EuiSplitPanel.Outer>
    // <EuiSplitPanel.Outer direction="row">
    //   <EuiSplitPanel.Inner
    //     color="subdued"
    //     style={{ marginLeft: '5vw', marginRight: '5vw', minHeight: '500px' }}
    //   >
    //     <EuiText>
    //       <h3>Search Reuslt 1</h3>
    //       <p>Left panel</p>
    //       <p>Has more content</p>
    //     </EuiText>
    //   </EuiSplitPanel.Inner>
    //   <EuiSplitPanel.Inner
    //     color="subdued"
    //     style={{ marginLeft: '5vw', marginRight: '5vw', minHeight: '500px' }}
    //   >
    //     <EuiText>
    //       <h3>Search Reuslt 2</h3>
    //       <p>Right panel</p>
    //     </EuiText>
    //   </EuiSplitPanel.Inner>
    // </EuiSplitPanel.Outer>
  );
};
