export const PLUGIN_ID = 'relevancyWorkbench';
export const PLUGIN_NAME = 'Relevancy Workbench';
export const PLUGIN_EXPLORER_NAME = 'Query Explorer';
export const PLUGIN_EXPLORER_URL = '/querqy_explorer';
export const PLUGIN_RULES_MANAGER_NAME = 'Rules Manager';
export const PLUGIN_RULES_MANAGER_URL = '/rules_manager';

const BASE_RELEVANCY_URI = '/_plugins/_querqy';
export const OPENSEARCH_RELEVANCY_API = {
  REWRITER: `${BASE_RELEVANCY_URI}/rewriter`,
};

const BASE_RELEVENCY_WORKBENCH_URI = '/api/relevancy_workbench';
export const BASE_RELEVENCY_WORKBENCH_API = {
  RULES_MANAGER: `${BASE_RELEVENCY_WORKBENCH_URI}/rules_manager`,
  RULES_MANAGER_RENAME: `${BASE_RELEVENCY_WORKBENCH_URI}/rules_manager/rename`,
  RULES_MANAGER_CLONE: `${BASE_RELEVENCY_WORKBENCH_URI}/rules_manager/clone`,
  RULES_MANAGER_CREATE: `${BASE_RELEVENCY_WORKBENCH_URI}/rules_manager/create`,
};

export const pageStyles: CSS.Properties = {
  float: 'left',
  width: '100%',
  maxWidth: '1130px',
};

export const QUERQY_DOCUMENTATION_LINK = 'https://docs.querqy.org/querqy/rewriters.html';

export const CREATE_REWRITER_MESSAGE =
  'Enter a name to describe the purpose of this rewriter panel.';

export const REWRITER_TYPE_MAP = {
  'Common Rules': 'querqy.opensearch.rewriter.SimpleCommonRulesRewriterFactory',
  Replace: 'querqy.opensearch.rewriter.ReplaceRewriterFactory',
  'Word Break': 'querqy.opensearch.rewriter.WordBreakCompoundRewriterFactory',
  'Number-Unit': 'querqy.opensearch.rewriter.NumberUnitRewriterFactory',
};

export interface PARSED_COMMON_RULES_TYPE {
  searchToken: string;
  unweightedSynonyms: {
    synonymInput: string;
  }[];
  weightedSynonyms: {
    weight: string;
    synonymInput: string;
  }[];
  upBoosts: {
    weight: string;
    upDownInput: string;
  }[];
  downBoosts: {
    weight: string;
    upDownInput: string;
  }[];
  filters: {
    filterInput: string;
  }[];
  deletes: {
    deleteInput: string;
  }[];
}
