[![Build and Test Search Reranker](https://github.com/opensearch-project/search-processor/actions/workflows/CI.yml/badge.svg)](https://github.com/opensearch-project/search-processor/actions/workflows/CI.yml)
[![codecov](https://codecov.io/gh/opensearch-project/search-processor/branch/main/graph/badge.svg?token=PYQO2GW39S)](https://codecov.io/gh/opensearch-project/search-processor)
![PRs welcome!](https://img.shields.io/badge/PRs-welcome!-success)

# OpenSearch Search Relevance - Reranker
- [Welcome!](#welcome)
- [Project Resources](#project-resources)
- [Credits and  Acknowledgments](#credits-and-acknowledgments)
- [Code of Conduct](#code-of-conduct)
- [License](#license)
- [Copyright](#copyright)

## Welcome!
This repository currently hosts a plugin to rerank search results before returning them to the client inline. The current plan is to expand this functionality to allow users to add rewriters, rerankers, and log what those components do. 

# History
This repository has also been used for discussion and ideas around search relevance. These discussions still exist here, however due to the relatively new standard of having one repo per plugin in OpenSearch and our implementations beginning to make it into the OpenSearch build, we have two repositories now. This repository will develop into a plugin that will allow OpenSearch users to rewrite search queries, rerank results, and log data about those actions. The other repository, [dashboards-search-relevance](https://www.github.com/opensearch-projects/dashboards-search-relevance), is where we will build front-end tooling to help relevance engineers and business users tune results. 

I don't know. Why not?


## Project Resources

* [Project Website](https://opensearch.org/)
* [Downloads](https://opensearch.org/downloads.html).
* [Project Principles](https://opensearch.org/#principles)
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
