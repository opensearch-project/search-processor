import {
  EuiPage,
  EuiPageBody,
  EuiPageHeader,
  EuiTitle,
  EuiPageContentBody,
  EuiPageHeaderSection,
  EuiCode,
  EuiPanel,
  EuiSpacer,
  EuiFlexItem,
  EuiButtonIcon,
  EuiFlexGroup,
  EuiSplitPanel,
  EuiText,
} from '@elastic/eui';
import { CoreStart, ChromeBreadcrumb } from '../../../../../src/core/public';
import React, { useEffect, useState } from 'react';
import { NavigationPublicPluginStart } from 'src/plugins/navigation/public';
import DSLService from '../../services/dsl';
import { gridObjectType } from '../../../common';
import '../../ace-themes/sql_console';
import { ConfigureFlyout } from './configure_flyout';
import { SearchConfigsPanel } from './search_components/search_configs';
import { SearchInputBar } from './search_components/search_bar';
import { SearchResultTable } from './result_components/search_result_table';

interface QueryExplorerProps {
  parentBreadCrumbs: ChromeBreadcrumb[];
  notifications: CoreStart['notifications'];
  http: CoreStart['http'];
  navigation: NavigationPublicPluginStart;
  setBreadcrumbs: (newBreadcrumbs: ChromeBreadcrumb[]) => void;
  dslService: DSLService;
  setToast: (title: string, color?: string, text?: any, side?: string) => void;
  chrome: CoreStart['chrome'];
}

export const Home = ({
  parentBreadCrumbs,
  notifications,
  http,
  navigation,
  setBreadcrumbs,
  dslService,
  setToast,
  chrome,
}: QueryExplorerProps) => {
  const defaultQuery = JSON.stringify({
    query: {
      term: { title: '%searchInput%' },
    },
  });
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [searchIndex1, setSearchIndex1] = useState('chorus-ecommerce-data');
  const [searchIndex2, setSearchIndex2] = useState('chorus-ecommerce-data');
  const [queryString1, setQueryString1] = useState(defaultQuery);
  const [queryString2, setQueryString2] = useState(defaultQuery);
  const [querqyResult1, setQuerqyResult1] = useState({});
  const [querqyResult2, setQuerqyResult2] = useState({});
  const [searchBarValue, setSearchBarValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Use React hooks to manage state.
  const [querqyResult, setQuerqyResult] = useState({});
  const [queryString, setQueryString] = useState(
    JSON.stringify({
      query: {
        term: { title: '%searchToken%' },
      },
    })
  );
  const [indexValue, setIndexValue] = useState('chorus-ecommerce-data');

  const [isFlyoutVisible, setIsFlyoutVisible] = useState(false);
  // const [searchToken, setSearchToken] = useState('');
  const [toggleIdSelected, setToggleIdSelected] = useState('view-mode-btn');
  const [isEditMode, setIsEditMode] = useState(false);
  const [gridObjects, setGridObjects] = useState<gridObjectType[]>([]);

  const onChange = (e: React.ChangeEvent<HTMLInputElement>, setter: any) => {
    setter(e.target.value);
  };

  const onClickSearch = () => {
    setIsLoading(true);
    const jsonQuery1 = JSON.parse(queryString1.replace(/%searchInput%/g, searchBarValue));
    const jsonQuery2 = JSON.parse(queryString2.replace(/%searchInput%/g, searchBarValue));
    http
      .post('/api/relevancy/search', {
        body: JSON.stringify({ index: searchIndex1, ...jsonQuery1 }),
      })
      .then((res) => {
        setQuerqyResult1(res);
      })
      .catch((error: Error) => {
        setQuerqyResult1(error.body.message);
        console.error(error);
      });

    http
      .post('/api/relevancy/search', {
        body: JSON.stringify({ index: searchIndex2, ...jsonQuery2 }),
      })
      .then((res) => {
        setQuerqyResult2(res);
      })
      .catch((error: Error) => {
        setQuerqyResult2(error.body.message);
        console.error(error);
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  const onClickHandler = (searchToken: string) => {
    setIsLoading(true);
    let jsonQuery;
    try {
      console.log('queryString', queryString);
      if (queryString === '' || searchToken === '') setToast('Query/Search token cannot be empty');
      else {
        jsonQuery = JSON.parse(queryString.replace(/%searchToken%/g, searchToken));
      }
    } catch (error) {
      setQuerqyResult(error.message);
      console.error(error);
      setIsLoading(false);
      return;
    }

    http
      .post('/api/relevancy/search', {
        body: JSON.stringify({ index: indexValue, ...jsonQuery }),
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

  const onChangeEdit = (optionId: string) => {
    if (optionId === 'edit-mode-btn') setIsEditMode(true);
    else setIsEditMode(false);
    setToggleIdSelected(optionId);
  };

  const closeFlyout = () => {
    setIsFlyoutVisible(false);
  };

  const showFlyout = () => {
    setIsFlyoutVisible(true);
  };

  let flyout;
  if (isFlyoutVisible) {
    flyout = (
      <ConfigureFlyout
        indexValue={indexValue}
        onChange={onChange}
        queryString={queryString}
        setQueryString={setQueryString}
        onClickHandler={onClickHandler}
        isLoading={isLoading}
        setIndexValue={setIndexValue}
        setQuerqyResult={setQuerqyResult}
        queryResult={querqyResult}
        closeFlyout={closeFlyout}
        setToast={setToast}
        // setSearchToken={setSearchToken}
      />
    );
  }

  const toggleButtons = [
    {
      id: `edit-mode-btn`,
      label: 'Edit mode',
    },
    {
      id: `view-mode-btn`,
      label: 'View mode',
    },
  ];

  useEffect(() => {
    setQuerqyResult({});
  }, [isFlyoutVisible]);

  useEffect(() => {
    console.log('query result changed in home');
  }, [querqyResult]);

  useEffect(() => {
    setBreadcrumbs([...parentBreadCrumbs]);

    setGridObjects([
      { id: '0', objectType: 'searchBar', x: 3, y: 0, w: 5, h: 1 },
      { id: '1', objectType: 'resultGrid', x: 1, y: 1, w: 9, h: 1 },
    ]);
  }, []);

  return (
    <>
      {
        <EuiPage id="searchView">
          <EuiPageBody>
            <EuiPageHeader>
              <>
                <EuiPageHeaderSection>
                  <EuiTitle size="l">
                    <h1>Search Relevance</h1>
                  </EuiTitle>
                </EuiPageHeaderSection>
              </>
            </EuiPageHeader>
            <EuiPageContentBody>
              <EuiSpacer size="l" />

              <EuiFlexGroup>
                {/* <EuiFlexItem grow={false}>
                  <EuiSpacer size="m" />
                  <EuiButtonIcon
                    color="text"
                    display="base"
                    iconType={isCollapsed ? 'arrowDown' : 'arrowRight'}
                    iconSize="m"
                    size="s"
                    aria-label="Next"
                    onClick={() => setIsCollapsed(!isCollapsed)}
                  />
                </EuiFlexItem> */}
                <EuiFlexItem>
                  <SearchConfigsPanel
                    isCollapsed={isCollapsed}
                    onChange={onChange}
                    searchIndex1={searchIndex1}
                    searchIndex2={searchIndex2}
                    setSearchIndex1={setSearchIndex1}
                    setSearchIndex2={setSearchIndex2}
                    queryString1={queryString1}
                    queryString2={queryString2}
                    setQueryString1={setQueryString1}
                    setQueryString2={setQueryString2}
                    setIsCollapsed={setIsCollapsed}
                  />
                </EuiFlexItem>
              </EuiFlexGroup>
              <EuiSpacer size="l" />

              <EuiPanel style={{ width: '100%' }}>
                <EuiSpacer size="l" />
                <EuiSpacer size="l" />
                <div style={{ width: '100%', display: 'flex', justifyContent: 'center' }}>
                  <SearchInputBar
                    searchBarValue={searchBarValue}
                    setSearchBarValue={setSearchBarValue}
                    isLoading={isLoading}
                    onClickSearch={onClickSearch}
                    setIsCollapsed={setIsCollapsed}
                  />
                </div>
                <EuiSpacer size="l" />
                <EuiSpacer size="l" />
                <EuiSpacer size="l" />
                <SearchResultTable querqyResult1={querqyResult1} querqyResult2={querqyResult2} />
              </EuiPanel>

              <></>
            </EuiPageContentBody>
          </EuiPageBody>
        </EuiPage>

        /* <EuiPage>
        <EuiPageBody component="main">
          <EuiPageHeader>
            <EuiFlexGroup>
              <EuiFlexItem>
                <EuiTitle size="l">
                  <h1>
                    {isEditMode && (
                      <FormattedMessage
                        id="relevancyWorkbench.title"
                        defaultMessage="{name}"
                        values={{ name: `${PLUGIN_EXPLORER_NAME}` }}
                      />
                    )}
                  </h1>
                </EuiTitle>
              </EuiFlexItem>
              <EuiFlexItem grow={false}>
                {isEditMode && <EuiButton onClick={showFlyout}>Configure</EuiButton>}
              </EuiFlexItem>
              <EuiFlexItem grow={false}>
                <EuiButtonGroup
                  style={{ marginTop: '5px' }}
                  legend="edit button selector"
                  options={toggleButtons}
                  idSelected={toggleIdSelected}
                  onChange={(id) => onChangeEdit(id)}
                />
              </EuiFlexItem>
            </EuiFlexGroup>
          </EuiPageHeader>
          <EuiPageContentBody>
            <ExplorerGrid
              chrome={chrome}
              gridObjects={gridObjects}
              editMode={isEditMode}
              onClickHandler={onClickHandler}
              querqyResult={querqyResult}
            />
          </EuiPageContentBody>
        </EuiPageBody>
      </EuiPage> */
      }
      {flyout}
    </>
  );
};
