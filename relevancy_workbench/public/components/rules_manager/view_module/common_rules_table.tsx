import { PARSED_COMMON_RULES_TYPE } from '../../../../common';
import React, { useEffect, useState } from 'react';
import {
  EuiButton,
  EuiContextMenu,
  EuiContextMenuPanelDescriptor,
  EuiFieldNumber,
  EuiFieldText,
  EuiFlexGroup,
  EuiFlexItem,
  EuiForm,
  EuiFormRow,
  EuiPopover,
  EuiSelect,
  EuiSpacer,
  EuiText,
  EuiToolTip,
} from '@elastic/eui';
import { createRuleTable } from './common_rules_utils';
import { CommonRulesText } from './common_rules_text';

interface CommonRulesTablePorps {
  rewriterRules: PARSED_COMMON_RULES_TYPE;
  setRewriterRules: React.Dispatch<React.SetStateAction<PARSED_COMMON_RULES_TYPE>>;
  onSave: () => void;
}

export const CommonRulesTable = ({
  rewriterRules,
  setRewriterRules,
  onSave,
}: CommonRulesTablePorps) => {
  const [searchToken, setSearchToken] = useState('');

  const [ruleTable, setRuleTable] = useState<JSX.Element[]>([]);
  const [addCommonRulePopover, setAddCommonRulePopover] = useState(false);

  useEffect(() => {
    setSearchToken(rewriterRules.searchToken);
    createRuleTable(rewriterRules, setRuleTable, setRewriterRules);
  }, [rewriterRules]);

  const addRuleMenu: EuiContextMenuPanelDescriptor[] = [
    {
      id: 0,
      title: 'Add rule',
      items: [
        {
          name: 'Synonym',
          onClick: () => {
            setAddCommonRulePopover(false);
            const updatedRules = { ...rewriterRules };
            updatedRules.unweightedSynonyms.push({ synonymInput: '' });
            setRewriterRules(updatedRules);
          },
        },
        {
          name: 'Weighted Synonym',
          onClick: () => {
            setAddCommonRulePopover(false);
            const updatedRules = { ...rewriterRules };
            updatedRules.weightedSynonyms.push({ synonymInput: '', weight: '1.0' });
            setRewriterRules(updatedRules);
          },
        },
        {
          name: 'Up Boost',
          onClick: () => {
            setAddCommonRulePopover(false);
            const updatedRules = { ...rewriterRules };
            updatedRules.upBoosts.push({ upDownInput: '', weight: '10' });
            setRewriterRules(updatedRules);
          },
        },
        {
          name: 'Down Boost',
          onClick: () => {
            setAddCommonRulePopover(false);
            const updatedRules = { ...rewriterRules };
            updatedRules.downBoosts.push({ upDownInput: '', weight: '10' });
            setRewriterRules(updatedRules);
          },
        },
        {
          name: 'Filter',
          onClick: () => {
            setAddCommonRulePopover(false);
            const updatedRules = { ...rewriterRules };
            updatedRules.filters.push({ filterInput: '' });
            setRewriterRules(updatedRules);
          },
        },
        {
          name: 'Delete',
          onClick: () => {
            setAddCommonRulePopover(false);
            const updatedRules = { ...rewriterRules };
            updatedRules.deletes.push({ deleteInput: '' });
            setRewriterRules(updatedRules);
          },
        },
      ],
    },
  ];

  const commonRulesAddButton = (
    <EuiButton
      iconType="arrowDown"
      iconSide="right"
      onClick={() => setAddCommonRulePopover(!addCommonRulePopover)}
    >
      Add
    </EuiButton>
  );

  return (
    <>
      <EuiFlexGroup>
        <EuiFlexItem grow={false}>
          <CommonRulesText />
        </EuiFlexItem>
        <EuiFlexItem grow={true}>
          <EuiForm component="form">
            <EuiFormRow style={{ marginBottom: '25px' }}>
              <EuiFieldText
                placeholder="Enter the search token"
                value={searchToken}
                onChange={(e) => setSearchToken(e.target.value)}
                prepend="Search Token"
                aria-label="search token input"
              />
            </EuiFormRow>
            {ruleTable.map((FormElement: JSX.Element) => {
              return (
                <>
                  {FormElement}
                  <EuiSpacer size="m" />
                </>
              );
            })}

            <EuiFlexGroup>
              <EuiFlexItem grow={false}>
                <EuiPopover
                  panelPaddingSize="none"
                  button={commonRulesAddButton}
                  isOpen={addCommonRulePopover}
                  closePopover={() => setAddCommonRulePopover(false)}
                >
                  <EuiContextMenu initialPanelId={0} panels={addRuleMenu} />
                </EuiPopover>
              </EuiFlexItem>
              <EuiFlexItem grow={false}>
                <EuiButton fill iconType="save" onClick={onSave}>
                  Save
                </EuiButton>
              </EuiFlexItem>
            </EuiFlexGroup>
          </EuiForm>
        </EuiFlexItem>
      </EuiFlexGroup>
    </>
  );
};
