## Version 2.4.0 Release Notes

This is the initial release of the search-processor plugin for OpenSearch 2.4.0.
This plugin serves as an extension point to customize search behavior by preprocessing search requests and post-processing search results.
In this first release, the only implementation is a search result post-processor (reranker) that passes results to AWS Kendra Intelligent Ranking.
More implementations and integrations will be added in future releases. Contributions are encouraged!

### What's Changed

- Search/relevancy ([#13](https://github.com/opensearch-project/search-processor/pull/13))
- adding MAINTAINERS.md ([#16](https://github.com/opensearch-project/search-processor/pull/16))
- Initial code for plugin based on opensearch-plugin-template-java ([#3](https://github.com/opensearch-project/search-processor/pull/3))
- Merge all changes to 2.x branch before cutting 2.4 branch ([#34](https://github.com/opensearch-project/search-processor/pull/34))

### New Contributors
- @YANG-DB made their first contribution in ([#13](https://github.com/opensearch-project/search-processor/pull/13))
- @kevinawskendra made their first contribution in ([#3](https://github.com/opensearch-project/search-processor/pull/3))
- @mahitamahesh made their first contribution in ([#23](https://github.com/opensearch-project/search-processor/pull/23))

Full Changelog: https://github.com/opensearch-project/search-relevance/commits/2.4.0