import {
  EuiPage,
  EuiPageBody,
  EuiPageHeader,
  EuiTitle,
  EuiPageContentBody,
  EuiPanel,
  EuiText,
  EuiFieldText,
  EuiSpacer,
  EuiCodeEditor,
  EuiFlexGroup,
  EuiFlexItem,
  EuiButton,
  EuiCodeBlock,
} from '@elastic/eui';
import { CoreStart, ChromeBreadcrumb } from '../../../../../src/core/public';
import React, { useEffect, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { NavigationPublicPluginStart } from 'src/plugins/navigation/public';
import DSLService from '../../services/dsl';
import { RelevancySideBar } from '../common_utils/side_nav';
import { PLUGIN_EXPLORER_URL, PLUGIN_EXPLORER_NAME } from '../../../common';
import '../../ace-themes/sql_console';

interface QueryExplorerProps {
  parentBreadCrumbs: ChromeBreadcrumb[];
  notifications: CoreStart['notifications'];
  http: CoreStart['http'];
  navigation: NavigationPublicPluginStart;
  setBreadcrumbs: (newBreadcrumbs: ChromeBreadcrumb[]) => void;
  dslService: DSLService;
}

export const Home = ({
  parentBreadCrumbs,
  notifications,
  http,
  navigation,
  setBreadcrumbs,
  dslService,
}: QueryExplorerProps) => {
  // Use React hooks to manage state.
  const [querqyResult, setQuerqyResult] = useState('');
  const [queryString, setQueryString] = useState('');
  const [indexValue, setIndexValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setIndexValue(e.target.value);
  };

  const onClickHandler = () => {
    setIsLoading(true);
    let jsonQuery;
    try {
      jsonQuery = JSON.parse(queryString);
    } catch (error) {
      setQuerqyResult(error.message);
      console.error(error);
      setIsLoading(false);
      return;
    }

    const requestBody = { index: indexValue, ...jsonQuery };

    http
      .post('/api/relevancy/search', {
        body: JSON.stringify(requestBody),
      })
      .then((res) => {
        setQuerqyResult(res);
      })
      .catch((error: Error) => {
        setQuerqyResult(error.body.message);
        console.error(error);
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  useEffect(() => {
    setBreadcrumbs([
      ...parentBreadCrumbs,
      {
        text: `${PLUGIN_EXPLORER_NAME}`,
        href: `#${PLUGIN_EXPLORER_URL}`,
      },
    ]);
  }, []);
  return (
    <RelevancySideBar>
      <EuiPage>
        <EuiPageBody component="main">
          <EuiPageHeader>
            <EuiTitle size="l">
              <h1>
                <FormattedMessage
                  id="relevancyWorkbench.title"
                  defaultMessage="{name}"
                  values={{ name: `${PLUGIN_EXPLORER_NAME}` }}
                />
              </h1>
            </EuiTitle>
          </EuiPageHeader>
          <EuiPageContentBody>
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
                  <EuiButton
                    type="primary"
                    size="s"
                    onClick={onClickHandler}
                    isLoading={isLoading}
                    fill
                  >
                    <FormattedMessage id="relevancyWorkbench.buttonText1" defaultMessage="Run" />
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
            </EuiPanel>
            <EuiSpacer size="xl" />
            <EuiPanel>
              <EuiCodeBlock
                language="json"
                fontSize="m"
                paddingSize="m"
                overflowHeight={500}
                isCopyable
                style={{ minHeight: '50px' }}
              >
                {JSON.stringify(querqyResult, null, 4)}
              </EuiCodeBlock>
            </EuiPanel>
          </EuiPageContentBody>
        </EuiPageBody>
      </EuiPage>
    </RelevancySideBar>
  );
};
