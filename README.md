[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://finosfoundation.atlassian.net/wiki/display/FINOS/Incubating)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Symphony Workflow Developer Kit (WDK)

📺 [Video introduction of the WDK](https://goto.symphony.com/WorkflowDevKit-Recording.html)

The Symphony Workflow Developer Kit (WDK) is a way to build bots on the Symphony platform with minimal coding efforts. A
standard execution engine, provided as a bot is running workflows written in a declarative manner.

Workflow executions are triggered by events (such [Real-Time Events](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events)) and run activities. Activities are small building
blocks often calling REST API endpoints of the Symphony platform or can even be custom-made to tailor your needs.

- [Getting started](./docs/getting-started.md): a beginner's guide to run your first workflow
- [Concepts](./docs/concepts.md): high-level view of the key concepts behind workflows
- [Architecture](./docs/architecture.md): technical bits on the execution of workflows
- [Deployment](./docs/deployment.md): how to run and configure the workflow bot
- [Custom activities](./docs/custom-activities.md): step-by-step guide on how to implement your own activities
- [Examples](./docs/examples): learning about workflows through examples
- Concrete and advanced examples in the [gallery](https://github.com/finos/symphony-wdk-gallery)
- [SWADL Reference](./docs/reference.md): syntax reference for the language used to write workflows

<hr />

## Usage example
![](./docs/gifs/wdk-demo.gif)
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
In order to get in touch with the project team, please open a [GitHub Issue](https://github.com/finos/symphony-wdk/issues).
Alternatively, you can email/subscribe to [symphony@finos.org](https://groups.google.com/a/finos.org/g/symphony).

1. Fork it (<https://github.com/finos/symphony-wdk>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Read our [contribution guidelines](.github/CONTRIBUTING.md) and [Community Code of Conduct](https://www.finos.org/code-of-conduct)
4. Check your code quality (`./gradlew check`)
5. Commit your changes (`git commit -am 'Add some fooBar'`)
6. Push to the branch (`git push origin feature/fooBar`)
7. Create a new Pull Request

_NOTE:_ Commits and pull requests to FINOS repositories will only be accepted from those contributors with an active, executed Individual Contributor License Agreement (ICLA) with FINOS OR who are covered under an existing and active Corporate Contribution License Agreement (CCLA) executed with FINOS. Commits from individuals not covered under an ICLA or CCLA will be flagged and blocked by the FINOS Clabot tool (or [EasyCLA](https://github.com/finos/community/blob/master/governance/Software-Projects/EasyCLA.md)). Please note that some CCLAs require individuals/employees to be explicitly named on the CCLA.

*Need an ICLA? Unsure if you are covered under an existing CCLA? Email [help@finos.org](mailto:help@finos.org)*

### Thanks to all the people who have contributed
[![contributors](https://contributors-img.web.app/image?repo=finos/symphony-wdk)](https://github.com/finos/symphony-wdk/graphs/contributors)

## Troubleshooting
To help you troubleshoot your workflows, you can generate BPMN in  ".bpmn" files and as a diagram in a png image by setting the _logging:level:com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder_ level to DEBUG in [application.yaml](./workflow-bot-app/src/main/resources/application.yaml). 

You firstly need to install [https://github.com/bpmn-io/bpmn-to-image](https://github.com/bpmn-io/bpmn-to-image) with `npm install -g bpmn-to-image`.

The BPMN can also be visualized on [https://demo.bpmn.io/](https://demo.bpmn.io/) by uploading your .bpmn file.

## Roadmap

Checkout the [open issues](https://github.com/finos/symphony-wdk/issues).

## License

Copyright 2022 Symphony LLC

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
