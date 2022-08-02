import {
  EuiFormRow,
  EuiFieldText,
  EuiFlexGroup,
  EuiFlexItem,
  EuiFieldNumber,
  EuiButtonIcon,
} from '@elastic/eui';
import { PARSED_COMMON_RULES_TYPE } from '../../../../common';
import React from 'react';

const onDeleteInput = (
  type: string,
  index: number,
  rules: PARSED_COMMON_RULES_TYPE,
  setRewriterRules: React.Dispatch<React.SetStateAction<PARSED_COMMON_RULES_TYPE>>
) => {
  let updatedRules = { ...rules };
  updatedRules[type].splice(index, 1);
  setRewriterRules(updatedRules);
};

const onChangeInput = (
  e: React.ChangeEvent<HTMLInputElement>,
  type: string,
  index: number,
  rules: PARSED_COMMON_RULES_TYPE,
  setRewriterRules: React.Dispatch<React.SetStateAction<PARSED_COMMON_RULES_TYPE>>
) => {
  let updatedRules = { ...rules };
  switch (type) {
    case 'unweightedSynonyms':
      updatedRules.unweightedSynonyms[index].synonymInput = e.target.value;
      break;

    case 'weightedSynonyms':
      updatedRules.weightedSynonyms[index].synonymInput = e.target.value;
      break;

    case 'weightedSynonymsWeight':
      updatedRules.weightedSynonyms[index].weight = e.target.value;
      break;

    case 'upBoosts':
      updatedRules.upBoosts[index].upDownInput = e.target.value;
      break;

    case 'upBoostsWeight':
      updatedRules.upBoosts[index].weight = e.target.value;
      break;

    case 'downBoosts':
      updatedRules.downBoosts[index].upDownInput = e.target.value;
      break;

    case 'downBoostsWeight':
      updatedRules.downBoosts[index].weight = e.target.value;
      break;

    case 'filters':
      updatedRules.filters[index].filterInput = e.target.value;
      break;

    case 'deletes':
      updatedRules.deletes[index].deleteInput = e.target.value;
      break;

    default:
      break;
  }

  setRewriterRules(updatedRules);
};

export const createRuleTable = (
  rules: PARSED_COMMON_RULES_TYPE,
  setRuleTable: React.Dispatch<React.SetStateAction<JSX.Element[]>>,
  setRewriterRules: React.Dispatch<React.SetStateAction<PARSED_COMMON_RULES_TYPE>>
) => {
  let unweightedSynonymsForm: JSX.Element[] = [];
  let weightedSynonymsForm: JSX.Element[] = [];
  let upBoostForm: JSX.Element[] = [];
  let downBoostForm: JSX.Element[] = [];
  let filterForm: JSX.Element[] = [];
  let deleteForm: JSX.Element[] = [];

  if (rules.hasOwnProperty('unweightedSynonyms')) {
    unweightedSynonymsForm = rules.unweightedSynonyms.map((token, id) => {
      return (
        <EuiFlexGroup>
          <EuiFlexItem grow={false} style={{ width: '400px', maxWidth: '400px' }}>
            <EuiFormRow label="Synonym" helpText="I am some friendly help text.">
              <EuiFieldText
                key={'synonym_' + id}
                id={'synonym_' + id}
                value={token.synonymInput}
                onChange={(e) =>
                  onChangeInput(e, 'unweightedSynonyms', id, rules, setRewriterRules)
                }
              />
            </EuiFormRow>
          </EuiFlexItem>
          <EuiFlexItem>
            <EuiFormRow hasEmptyLabelSpace={true}>
              <EuiButtonIcon
                display="base"
                iconType="trash"
                aria-label="Delete"
                color="danger"
                size="s"
                onClick={() => onDeleteInput('unweightedSynonyms', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
          </EuiFlexItem>
        </EuiFlexGroup>
      );
    });
  }

  if (rules.hasOwnProperty('weightedSynonyms')) {
    weightedSynonymsForm = rules.weightedSynonyms.map((token, id) => {
      return (
        <EuiFlexGroup>
          <EuiFlexItem style={{ width: '400px', maxWidth: '400px' }}>
            <EuiFormRow label="Weighted Synonym" helpText="I am some friendly help text.">
              <EuiFieldText
                key={'weighted_synonym_' + id}
                id={'weighted_synonym_' + id}
                value={token.synonymInput}
                onChange={(e) => onChangeInput(e, 'weightedSynonyms', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
          </EuiFlexItem>
          <EuiFlexItem style={{ width: 100 }} grow={false}>
            <EuiFormRow label="Weight">
              <EuiFieldNumber
                key={'weighted_synonym_weight' + id}
                id={'weighted_synonym_weight' + id}
                max={1.0}
                step={0.01}
                value={token.weight}
                onChange={(e) =>
                  onChangeInput(e, 'weightedSynonymsWeight', id, rules, setRewriterRules)
                }
              />
            </EuiFormRow>
          </EuiFlexItem>
          <EuiFlexItem>
            <EuiFormRow display="center" hasEmptyLabelSpace={true}>
              <EuiButtonIcon
                display="base"
                iconType="trash"
                aria-label="Delete"
                color="danger"
                size="s"
                onClick={() => onDeleteInput('weightedSynonyms', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
          </EuiFlexItem>
        </EuiFlexGroup>
      );
    });
  }

  if (rules.hasOwnProperty('upBoosts')) {
    upBoostForm = rules.upBoosts.map((token, id) => {
      return (
        <EuiFlexGroup>
          <EuiFlexItem style={{ width: '400px', maxWidth: '400px' }}>
            <EuiFormRow label="Up Boost" helpText="I am some friendly help text.">
              <EuiFieldText
                key={'up_boost_' + id}
                id={'up_boost_' + id}
                value={token.upDownInput}
                onChange={(e) => onChangeInput(e, 'upBoosts', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
          </EuiFlexItem>
          <EuiFlexItem style={{ width: 100 }} grow={false}>
            <EuiFormRow label="Weight">
              <EuiFieldNumber
                key={'up_boost_weight' + id}
                id={'up_boost_weight' + id}
                max={500}
                step={10}
                value={token.weight}
                onChange={(e) => onChangeInput(e, 'upBoostsWeight', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
          </EuiFlexItem>
          <EuiFlexItem>
            <EuiFormRow display="center" hasEmptyLabelSpace={true}>
              <EuiButtonIcon
                display="base"
                iconType="trash"
                aria-label="Delete"
                color="danger"
                size="s"
                onClick={() => onDeleteInput('upBoosts', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
          </EuiFlexItem>
        </EuiFlexGroup>
      );
    });
  }

  if (rules.hasOwnProperty('downBoosts')) {
    downBoostForm = rules.downBoosts.map((token, id) => {
      return (
        <EuiFlexGroup key={'down_boost_' + id}>
          <EuiFlexItem style={{ width: '400px', maxWidth: '400px' }}>
            <EuiFormRow label="Down Boost" helpText="I am some friendly help text.">
              <EuiFieldText
                key={'down_boost_' + id}
                id={'down_boost_' + id}
                value={token.upDownInput}
                onChange={(e) => onChangeInput(e, 'downBoosts', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
          </EuiFlexItem>
          <EuiFlexItem style={{ width: 100 }} grow={false}>
            <EuiFormRow label="Weight">
              <EuiFieldNumber
                id={'down_boost_weight' + id}
                max={500}
                step={10}
                value={token.weight}
                onChange={(e) => onChangeInput(e, 'downBoostsWeight', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
          </EuiFlexItem>
          <EuiFlexItem>
            <EuiFormRow display="center" hasEmptyLabelSpace={true}>
              <EuiButtonIcon
                display="base"
                iconType="trash"
                aria-label="Delete"
                color="danger"
                size="s"
                onClick={() => onDeleteInput('downBoosts', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
          </EuiFlexItem>
        </EuiFlexGroup>
      );
    });
  }

  if (rules.hasOwnProperty('filters')) {
    filterForm = rules.filters.map((token, id) => {
      return (
        <EuiFlexGroup>
          <EuiFlexItem style={{ width: '400px', maxWidth: '400px' }}>
            <EuiFormRow label="Filter" helpText="I am some friendly help text.">
              <EuiFieldText
                key={'filter_' + id}
                id={'filter_' + id}
                value={token.filterInput}
                onChange={(e) => onChangeInput(e, 'filters', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
          </EuiFlexItem>
          <EuiFlexItem>
            <EuiFormRow display="center" hasEmptyLabelSpace={true}>
              <EuiButtonIcon
                display="base"
                iconType="trash"
                aria-label="Delete"
                color="danger"
                size="s"
                onClick={() => onDeleteInput('filters', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
          </EuiFlexItem>
        </EuiFlexGroup>
      );
    });
  }

  if (rules.hasOwnProperty('deletes')) {
    deleteForm = rules.deletes.map((token, id) => {
      return (
        <EuiFlexGroup>
          <EuiFlexItem style={{ width: '400px', maxWidth: '400px' }}>
            <EuiFormRow label="Delete" helpText="I am some friendly help text.">
              <EuiFieldText
                key={'delete_' + id}
                id={'delete_' + id}
                value={token.deleteInput}
                onChange={(e) => onChangeInput(e, 'deletes', id, rules, setRewriterRules)}
              />
            </EuiFormRow>
            <EuiFlexItem>
              <EuiFormRow display="center" hasEmptyLabelSpace={true}>
                <EuiButtonIcon
                  display="base"
                  iconType="trash"
                  aria-label="Delete"
                  color="danger"
                  size="s"
                  onClick={() => onDeleteInput('deletes', id, rules, setRewriterRules)}
                />
              </EuiFormRow>
            </EuiFlexItem>
          </EuiFlexItem>
        </EuiFlexGroup>
      );
    });
  }

  setRuleTable([
    ...unweightedSynonymsForm,
    ...weightedSynonymsForm,
    ...upBoostForm,
    ...downBoostForm,
    ...filterForm,
    ...deleteForm,
  ]);
};

const synonymParser1 = (synonymTokens: string[]) => {
  const unweightedSynonyms = synonymTokens.filter((token) => token.startsWith('SYNONYM:'));
  const parsedUnweightedSynonyms = unweightedSynonyms.map((token) => {
    const trimmedToken = token.trim();
    return { synonymInput: trimmedToken.replace('SYNONYM:', '').trim() };
  });
  const weightedSynonyms = synonymTokens.filter((token) => token.startsWith('SYNONYM('));
  const parsedWeightedSynonyms = weightedSynonyms.map((token) => {
    const trimmedToken = token.trim();
    return {
      weight: trimmedToken
        .match(/SYNONYM\((.*?)\):/g)[0]
        .match(/\((.*?)\)/g)[0]
        .replace(/[\(\)']+/g, ''),
      synonymInput: trimmedToken.replace(/SYNONYM\((.*?)\):/g, '').trim(),
    };
  });
  return {
    unwieghted: parsedUnweightedSynonyms,
    weighted: parsedWeightedSynonyms,
  };
};

const upDownBoostParser = (updownBoostTokens: string[], tokenKeyword: string) => {
  const upDownBoosts = updownBoostTokens.filter((token) => token.startsWith(tokenKeyword + '('));

  const parsedUpDownTokens = upDownBoosts.map((token) => {
    const trimmedToken = token.trim();
    return {
      weight: trimmedToken
        .match(new RegExp('/' + tokenKeyword + '((.*?)):/g'))[0]
        .match(/\((.*?)\)/g)[0]
        .replace(/[\(\)']+/g, ''),
      upDownInput: trimmedToken.replace(new RegExp('/' + tokenKeyword + '((.*?)):/g'), '').trim(),
    };
  });

  return parsedUpDownTokens;
};

const filterParser = (filterTokens: string[]) => {
  const parsedFilterTokens = filterTokens.map((token) => {
    const trimmedToken = token.trim();
    return {
      filterInput: trimmedToken.replace('FILTER:', '').trim(),
    };
  });

  return parsedFilterTokens;
};

const deleteParser = (deleteTokens: string[]) => {
  const parsedDeleteTokens = deleteTokens.map((token) => {
    const trimmedToken = token.trim();
    return {
      deleteInput: trimmedToken.replace('DELETE:', '').trim(),
    };
  });

  return parsedDeleteTokens;
};

export const commonRulesParser = (rules: string) => {
  try {
    const searchToken = rules.substring(0, rules.indexOf('=>')).trim();
    const splitByDelimiter = rules.split('\n');
    const synonymTokens = splitByDelimiter.filter((token) => token.startsWith('SYNONYM'));
    const upTokens = splitByDelimiter.filter((token) => token.startsWith('UP('));
    const downTokens = splitByDelimiter.filter((token) => token.startsWith('DOWN'));
    const filterTokens = splitByDelimiter.filter((token) => token.startsWith('FILTER:'));
    const deleteTokens = splitByDelimiter.filter((token) => token.startsWith('DELETE:'));

    const parsedSynonyms = synonymParser1(synonymTokens);
    const parsedUpBoosts = upDownBoostParser(upTokens, 'UP');
    const parsedDownBoosts = upDownBoostParser(downTokens, 'DOWN');
    const parsedFilters = filterParser(filterTokens);
    const deleteFilters = deleteParser(deleteTokens);

    return {
      searchToken: searchToken,
      unweightedSynonyms: parsedSynonyms.unwieghted,
      weightedSynonyms: parsedSynonyms.weighted,
      upBoosts: parsedUpBoosts,
      downBoosts: parsedDownBoosts,
      filters: parsedFilters,
      deletes: deleteFilters,
    };
  } catch (error) {
    throw new Error('Cannot parse rules string', error);
  }
};

export const CommonRulesToString = (rules: PARSED_COMMON_RULES_TYPE) => {
  let ruleString = '';

  if (rules.searchToken !== '') ruleString += rules.searchToken + ' =>';

  if (rules.unweightedSynonyms.length !== 0)
    ruleString +=
      '\n' +
      rules.unweightedSynonyms
        .map((token) => {
          return 'SYNONYM: ' + token.synonymInput;
        })
        .join('\n');

  if (rules.weightedSynonyms.length !== 0)
    ruleString +=
      '\n' +
      rules.weightedSynonyms
        .map((token) => {
          return 'SYNONYM(' + token.weight + '): ' + token.synonymInput;
        })
        .join('\n');

  if (rules.upBoosts.length !== 0)
    ruleString +=
      '\n' +
      rules.upBoosts
        .map((token) => {
          return 'UP(' + token.weight + '): ' + token.upDownInput;
        })
        .join('\n');

  if (rules.downBoosts.length !== 0)
    ruleString +=
      '\n' +
      rules.downBoosts
        .map((token) => {
          return 'DOWN(' + token.weight + '): ' + token.upDownInput;
        })
        .join('\n');

  if (rules.filters.length !== 0)
    ruleString +=
      '\n' +
      rules.filters
        .map((token) => {
          return 'FILTER: ' + token.filterInput;
        })
        .join('\n');

  if (rules.deletes.length !== 0)
    ruleString +=
      '\n' +
      rules.deletes
        .map((token) => {
          return 'DELETE: ' + token.deleteInput;
        })
        .join('\n');

  return ruleString;
};
