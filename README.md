[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://finosfoundation.atlassian.net/wiki/display/FINOS/Incubating)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Symphony Workflow Developer Kit (WDK)

_Also known as Workflow API or Workflow bot_, check out the  [documentation](./docs).

:warning: _The WDK is very much in its inception and we are actually looking to get feedback on its usage. It also means
all the APIs and the SWADL format are subject to change._

## Getting started
Check out the [Getting starting guide](./docs/getting-started.md) for an introduction.

## Usage example

_For more examples and usage, please refer to the [docs/examples](./docs/examples)._

## Development setup

### Build
Java (JDK) 11 is required and Gradle is used to build the project.
```shell
./gradlew build
```

### Tests run
```sh
./gradlew check
```

## Contributing
In order to get in touch with the project team, please open a [GitHub Issue](https://github.com/SymphonyPlatformSolutions/symphony-wdk/issues).
Alternatively, you can email/subscribe to [symphony@finos.org](https://groups.google.com/a/finos.org/g/symphony).

1. Fork it (<https://github.com/SymphonyPlatformSolutions/symphony-wdk>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Read our [contribution guidelines](.github/CONTRIBUTING.md) and [Community Code of Conduct](https://www.finos.org/code-of-conduct)
4. Check your code quality (`./gradlew check`)
5. Commit your changes (`git commit -am 'Add some fooBar'`)
6. Push to the branch (`git push origin feature/fooBar`)
7. Create a new Pull Request

_NOTE:_ Commits and pull requests to FINOS repositories will only be accepted from those contributors with an active, executed Individual Contributor License Agreement (ICLA) with FINOS OR who are covered under an existing and active Corporate Contribution License Agreement (CCLA) executed with FINOS. Commits from individuals not covered under an ICLA or CCLA will be flagged and blocked by the FINOS Clabot tool (or [EasyCLA](https://github.com/finos/community/blob/master/governance/Software-Projects/EasyCLA.md)). Please note that some CCLAs require individuals/employees to be explicitly named on the CCLA.

*Need an ICLA? Unsure if you are covered under an existing CCLA? Email [help@finos.org](mailto:help@finos.org)*


## License

Copyright 2021 Symphony LLC

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
