import {
  EuiPanel,
  EuiText,
  EuiFieldText,
  EuiSpacer,
  EuiCodeEditor,
  EuiFlexGroup,
  EuiFlexItem,
  EuiButton,
  EuiCodeBlock,
  EuiFlyoutHeader,
  EuiTitle,
  EuiFlyoutFooter,
  EuiFieldSearch,
} from '@elastic/eui';
import React, { useEffect, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { FlyoutContainers } from '../common_utils/flyoutContainers';

interface ConfigureFlyoutProps {
  indexValue: string;
  onChange: (e) => void;
  queryString: string;
  setQueryString: React.Dispatch<React.SetStateAction<string>>;
  onClickHandler: (value: string) => void;
  isLoading: boolean;
  setIndexValue: React.Dispatch<React.SetStateAction<string>>;
  setQuerqyResult: React.Dispatch<React.SetStateAction<{}>>;
  queryResult: any;
  closeFlyout: () => void;
  setToast: (title: string, color?: string, text?: any, side?: string) => void;
  // setSearchToken: React.Dispatch<React.SetStateAction<string>>;
}

export const ConfigureFlyout = ({
  indexValue,
  onChange,
  queryString,
  setQueryString,
  onClickHandler,
  isLoading,
  setIndexValue,
  setQuerqyResult,
  queryResult,
  closeFlyout,
  setToast,
}: // setSearchToken,
ConfigureFlyoutProps) => {
  const [previewValue, setPreviewValue] = useState('');

  const onChangePreview = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPreviewValue(e.target.value);
  };

  const onClickPreview = () => {
    if (previewValue === '') setToast('Search token cannot be empty', 'danger', undefined, 'right');
    else onClickHandler(previewValue);
  };
  const flyoutHeader = (
    <EuiFlyoutHeader hasBorder>
      <EuiTitle size="m">
        <h2 id="searchConfig">Configure Search</h2>
      </EuiTitle>
    </EuiFlyoutHeader>
  );

  const flyoutFooter = (
    <EuiFlyoutFooter>
      <EuiFlexGroup gutterSize="s" justifyContent="spaceBetween">
        <EuiFlexItem grow={false}>
          <EuiButton onClick={closeFlyout}>Cancel</EuiButton>
        </EuiFlexItem>
        <EuiFlexItem grow={false}>
          <EuiButton onClick={() => {}} fill>
            Save
          </EuiButton>
        </EuiFlexItem>
      </EuiFlexGroup>
    </EuiFlyoutFooter>
  );

  const flyoutBody = (
    <EuiPanel>
      <EuiText>
        <h3>Search Index</h3>
      </EuiText>
      <EuiFieldText
        placeholder="Index name"
        value={indexValue}
        onChange={(e) => onChange(e)}
        aria-label="Use aria labels when no actual label is in use"
      />
      <EuiSpacer size="m" />
      <EuiText>
        <h3> Querqy editor</h3>
      </EuiText>
      <EuiCodeEditor
        theme="sql_console"
        width="100%"
        height="200px"
        value={queryString}
        onChange={setQueryString}
        showPrintMargin={false}
        setOptions={{
          fontSize: '14px',
          showLineNumbers: true,
          showGutter: false,
        }}
        aria-label="Code Editor"
      />
      <EuiSpacer size="l" />
      <EuiFlexGroup>
        <EuiFlexItem grow={false}>
          <EuiFieldSearch
            placeholder="Enter Search Token"
            value={previewValue}
            isClearable={true}
            onChange={onChangePreview}
          />
        </EuiFlexItem>
        <EuiFlexItem grow={false}>
          <EuiButton type="primary" size="s" onClick={onClickPreview} isLoading={isLoading} fill>
            <FormattedMessage id="relevancyWorkbench.buttonText1" defaultMessage="Preview" />
          </EuiButton>
        </EuiFlexItem>
        <EuiFlexItem grow={false}>
          <EuiButton
            type="primary"
            size="s"
            isDisabled={isLoading}
            onClick={() => {
              setQuerqyResult('');
              setIndexValue('');
              setQueryString('');
            }}
          >
            <FormattedMessage id="relevancyWorkbench.buttonText2" defaultMessage="Clear" />
          </EuiButton>
        </EuiFlexItem>
      </EuiFlexGroup>
      <EuiSpacer size="m" />
      <EuiCodeBlock
        language="json"
        fontSize="m"
        paddingSize="m"
        isCopyable
        style={{ maxHeight: '20vh' }}
      >
        {JSON.stringify(queryResult, null, 4)}
      </EuiCodeBlock>
    </EuiPanel>
  );

  // useEffect(() => {
  //   setSearchToken(previewValue);
  // }, [previewValue]);

  return (
    <FlyoutContainers
      closeFlyout={closeFlyout}
      flyoutHeader={flyoutHeader}
      flyoutBody={flyoutBody}
      flyoutFooter={flyoutFooter}
      ariaLabel="searchConfigFlyout"
    />
  );
};
