[![Build and Test Search Query & Request Transformers](https://github.com/opensearch-project/search-processor/actions/workflows/CI.yml/badge.svg)](https://github.com/opensearch-project/search-processor/actions/workflows/CI.yml)
[![codecov](https://codecov.io/gh/opensearch-project/search-processor/branch/main/graph/badge.svg?token=PYQO2GW39S)](https://codecov.io/gh/opensearch-project/search-processor)
![PRs welcome!](https://img.shields.io/badge/PRs-welcome!-success)

# Search Rerankers: AWS Kendra & AWS Personalize
- [Welcome!](#welcome)
- [Project Resources](#project-resources)
- [Code of Conduct](#code-of-conduct)
- [License](#license)
- [Copyright](#copyright)

## Welcome!
This repository hosts the code for two self-install re-rankers that integrate into [Search Pipelines](https://opensearch.org/docs/latest/search-plugins/search-pipelines/index/). User documentation for the Personalize Reranker is [here](https://opensearch.org/docs/latest/search-plugins/search-pipelines/personalize-search-ranking/). For Kendra, it is [here](https://opensearch.org/docs/latest/search-plugins/search-relevance/index/#reranking-results-with-kendra-intelligent-ranking-for-opensearch). 

# Search Processors: Where Do They Go?
The current guideline for developing processors is that if you are developing a processor that would introduce new dependencies in [OpenSearch Core](https://github.com/opensearch-project/OpenSearch) (e.g. new libraries, makes a network connection outside of OpenSearch), it should be in a separate repository. Please consider creating it in a standalone repository since each processor should be thought of like a \*NIX command with input and output connected by pipes (i.e. a Search Pipeline). Each processor should do one thing and do it well. Otherwise, it could go into the OpenSearch repository under [org.opensearch.search.pipeline.common](https://github.com/opensearch-project/OpenSearch/tree/a08d588691c3b232e65d73b0a0c2fc5c72c870cf/modules/search-pipeline-common). If you have doubts, just create an issue in OpenSearch Core and, if you have one, a new PR. Maintainers will help guide you.


# History
This repository has also been used for discussion and ideas around search relevance. These discussions still exist here, however due to the relatively new standard of having one repo per plugin in OpenSearch and our implementations beginning to make it into the OpenSearch build, we have two repositories now. This repository will develop into a plugin that will allow OpenSearch users to rewrite search queries, rerank results, and log data about those actions. The other repository, [dashboards-search-relevance](https://www.github.com/opensearch-projects/dashboards-search-relevance), is where we will build front-end tooling to help relevance engineers and business users tune results. 

## Project Resources

* [OpenSearch Project Website](https://opensearch.org/)
* [Downloads](https://opensearch.org/downloads.html)
* [Project Principles](https://opensearch.org/#principles)
* [Search Pipelines](https://opensearch.org/docs/latest/search-plugins/search-pipelines/index/)
* [Contributing to OpenSearch Search Request Processor](CONTRIBUTING.md)
* [Search Relevance](RELEVANCE.md)
* [Maintainer Responsibilities](MAINTAINERS.md)
* [Release Management](RELEASING.md)
* [Admin Responsibilities](ADMINS.md)
* [Security](SECURITY.md)


## Code of Conduct

This project has adopted the [Amazon Open Source Code of Conduct](CODE_OF_CONDUCT.md). For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq), or contact [opensource-codeofconduct@amazon.com](mailto:opensource-codeofconduct@amazon.com) with any additional questions or comments.

## License

This project is licensed under the [Apache v2.0 License](LICENSE).

## Copyright

Copyright OpenSearch Contributors. See [NOTICE](NOTICE) for details.
