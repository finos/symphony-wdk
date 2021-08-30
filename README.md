# Symphony Workflow Developer Kit (WDK)

_Also known as Workflow API or Workflow bot_, check out the [documentation on Confluence](https://perzoinc.atlassian.net/wiki/spaces/DevX/pages/1455556023/10.+Workflow+API)

## Usage

The bot reacts to direct messages, executing the workflow attached to the message.

## How to build

Java 11 is required and Gradle is used to build the project.

```shell
./gradlew build
``` 

## CI/CD

Pull requests are built on [Warpdrive](https://warpdrive.dev.symphony.com/jenkins/job/SymphonyOSF/job/workflow-bot).

Changes on the `master` branch trigger a deployment on 
[GCP](https://console.cloud.google.com/kubernetes/deployment/us-central1/devx-autopilot-private-cluster/dev/workflow-bot/overview?project=sym-dev-plat)
where an instance of the bot runs against devx3 pod.
