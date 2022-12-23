Forcing a pr_stats review.

[![Build and Test Search Query & Request Transformers](https://github.com/opensearch-project/search-processor/actions/workflows/CI.yml/badge.svg)](https://github.com/opensearch-project/search-processor/actions/workflows/CI.yml)
[![codecov](https://codecov.io/gh/opensearch-project/search-processor/branch/main/graph/badge.svg?token=PYQO2GW39S)](https://codecov.io/gh/opensearch-project/search-processor)
![PRs welcome!](https://img.shields.io/badge/PRs-welcome!-success)

# Search Query & Request Transformers
- [Welcome!](#welcome)
- [Project Resources](#project-resources)
- [Credits and  Acknowledgments](#credits-and-acknowledgments)
- [Code of Conduct](#code-of-conduct)
- [License](#license)
- [Copyright](#copyright)

## Welcome!
This repository is the home of an evolving project that aims to create a pipeline of transformers to preprocess queries before search and post-process results after search. The first component here is a plugin to re-rank search results before returning them to the client inline. In the coming year, we will add hooks to configure other re-rankers and allow users to add their own components to the pipeline. Logging will also be a critical part of the pipeline in two ways: 
1. Logging information about the search experience (e.g. query, search results returned from the index, search results returned to the OpenSearch client) 
1. Logging debug information about the transformers

We will be publishing an RFC soon to give more detail and have a deeper conversation, but for now take a look at the code, open issues, comment, etc.

# History
This repository has also been used for discussion and ideas around search relevance. These discussions still exist here, however due to the relatively new standard of having one repo per plugin in OpenSearch and our implementations beginning to make it into the OpenSearch build, we have two repositories now. This repository will develop into a plugin that will allow OpenSearch users to rewrite search queries, rerank results, and log data about those actions. The other repository, [dashboards-search-relevance](https://www.github.com/opensearch-projects/dashboards-search-relevance), is where we will build front-end tooling to help relevance engineers and business users tune results. 


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
