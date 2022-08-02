import {
  EuiCodeEditor,
  EuiFlexGroup,
  EuiFlexItem,
  EuiIcon,
  EuiPanel,
  EuiSpacer,
  EuiTabbedContent,
  EuiTabbedContentTab,
  EuiText,
} from '@elastic/eui';
import { PARSED_COMMON_RULES_TYPE } from '../../../../common';
import React, { Fragment, useEffect, useState } from 'react';
import { commonRulesParser, CommonRulesToString } from './common_rules_utils';
import { CommonRulesTable } from './common_rules_table';
import { CommonRulesText } from './common_rules_text';
import _ from 'lodash';

interface CommonRulesViewProps {
  contentLoading: boolean;
  rewriterContent: any;
  setconfigBody: React.Dispatch<React.SetStateAction<{}>>;
  onSave: () => void;
}

export const CommonRulesView = ({
  contentLoading,
  rewriterContent,
  setconfigBody,
  onSave,
}: CommonRulesViewProps) => {
  const [rewriterRules, setRewriterRules] = useState<PARSED_COMMON_RULES_TYPE>(
    {} as PARSED_COMMON_RULES_TYPE
  );

  const [queryString, setQueryString] = useState('');
  const [selectedTab, setSelectedTab] = useState<EuiTabbedContentTab>();

  const tabs = [
    {
      id: 'rule-table',
      name: 'Rule Table',
      content: (
        <Fragment>
          <EuiSpacer />
          <CommonRulesTable
            rewriterRules={rewriterRules}
            setRewriterRules={setRewriterRules}
            onSave={onSave}
          ></CommonRulesTable>
        </Fragment>
      ),
    },
    {
      id: 'rule-raw-string',
      name: 'Rule Raw String',
      content: (
        <Fragment>
          <EuiSpacer />
          <EuiFlexGroup>
            <EuiFlexItem grow={false}>
              <CommonRulesText />
            </EuiFlexItem>
            <EuiFlexItem>
              <EuiText>
                <EuiCodeEditor
                  //   theme="sql_console"
                  width="100%"
                  height="400px"
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
              </EuiText>
            </EuiFlexItem>
          </EuiFlexGroup>
        </Fragment>
      ),
    },
  ];

  const updateRewriterData = (newTabId: string) => {
    if (newTabId === 'rule-table') {
      let updatedRules = {} as PARSED_COMMON_RULES_TYPE;
      try {
        updatedRules = commonRulesParser(queryString);
        setRewriterRules(updatedRules);
        setSelectedTab(tabs[0]);
      } catch (error) {
        console.log('Cannot parse rules string');
        setSelectedTab(tabs[1]);
      }
    } else {
      // setQueryString(CommonRulesToString(rewriterRules));
      setSelectedTab(tabs[1]);
    }
  };

  useEffect(() => {
    if (!_.isEmpty(rewriterRules) && queryString !== '') {
      if (selectedTab?.id === 'rule-table') {
        setconfigBody({ rules: CommonRulesToString(rewriterRules) });
      } else {
        setconfigBody({ rules: queryString });
      }
    }
  }, [rewriterRules, queryString]);

  useEffect(() => {
    if (!_.isEmpty(rewriterRules)) {
      const ruleString = CommonRulesToString(rewriterRules);
      setQueryString(ruleString);
    }
  }, [rewriterRules]);

  useEffect(() => {
    setRewriterRules(commonRulesParser(rewriterContent.config.rules));
    // setQueryString(rewriterContent.config.rules);
    setSelectedTab(tabs[0]);
  }, []);

  return (
    <EuiPanel>
      <EuiTabbedContent
        tabs={tabs}
        initialSelectedTab={tabs[0]}
        // selectedTab={selectedTab}
        onTabClick={(tab) => {
          updateRewriterData(tab.id);
        }}
      />
    </EuiPanel>
  );
};
