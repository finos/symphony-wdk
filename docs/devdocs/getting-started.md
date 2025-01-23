---
description: >-
  The Workflow Developer Kit (WDK) accelerates building of workflows using a
  low-code environment with an optional graphical interface
---

# Getting Started with WDK

## Deployment Options

WDK offers a number of deployment options depending on your organization requirements and preferences:

### 1. Docker <a href="#docker" id="docker"></a>

The easiest way to get started is by using the [WDK Docker image](https://hub.docker.com/r/finos/symphony-wdk/tags). This will require [Docker Desktop](https://www.docker.com/products/docker-desktop/) to be installed but no other tooling is necessary.

If you wish to run WDK Studio, the graphical interface for WDK, then the [WDK Studio Docker image](https://hub.docker.com/r/finos/symphony-wdk-studio/tags) is the only deployment option supported.

### 2. JAR <a href="#jar" id="jar"></a>

You can also download the latest JAR from the [Releases](https://github.com/finos/symphony-wdk/releases) page on the WDK repository. This will require a local Java runtime (11+) to be installed for launching the JAR, but no compilation is necessary.

### 3. Project <a href="#project" id="project"></a>

The last option involves a complete project where gradle is used to obtain the JAR and also to build custom activities. This approach is only for advanced use where WDK needs to be extended with custom behaviour.

## Generate Project

The [Symphony Generator](../../dev-tools/generator.md) offers a fast way to bootstrap your Symphony WDK project.

> [!NOTE]
> **Prerequisite**: Install NodeJS first, either [directly](https://nodejs.org) or via [nvm](https://github.com/nvm-sh/nvm)

```bash
$ npm i -g yo @finos/generator-symphony
$ mkdir wdk-bot && cd $_
$ yo @finos/symphony
```

This will prompt you with a number of questions about your bot and pod configuration. Type in your bot's information, using arrow keys to scroll and press enter to move on to the next prompt.

<details>
  <summary>Docker</summary>
```
 __   __     ___                 _
 \ \ / /__  / __|_  _ _ __  _ __| |_  ___ _ _ _  _
  \ V / _ \ \__ \ || | '  \| '_ \ ' \/ _ \ ' \ || |
   |_|\___/ |___/\_, |_|_|_| .__/_||_\___/_||_\_, |
                 |__/      |_|                |__/


Welcome to Symphony Generator v2.7.1
Application files will be generated in folder: /home/user/code/wdk-bot
______________________________________________________________________________________________________
? Enter your pod host mycompany.symphony.com
? Enter your bot username wdk-bot
? Select your type of application Workflow Application (WDK) Docker

Generating RSA keys...
   create symphony/publickey.pem
   create symphony/privatekey.pem
   create symphony/application.yaml
   create startup.sh

No change to package.json was detected. No package manager install will be executed.

You can now update the service account wdk-bot with the following public key on https://mycompany.symphony.com/admin-console :

-----BEGIN RSA PUBLIC KEY-----
MIICCgKCAgEA7wBwCGQm30vU7krseefqhwRkENQFk6dtL12pmxZ91d+IIBwVioUj
...
LqPq1P4cmTqyKeVphuQ3B2vPpEJoqr1XTykg2B/k67+nat+gEGFZVbkCAwEAAQ==
-----END RSA PUBLIC KEY-----

Your workflow bot has been successfully generated !

</details>

<details>
  <summary>JAR</summary>
1. Use Docker instructions
2. Download latest JAR from WDK GitHub repository's [Releases page](https://github.com/finos/symphony-wdk/releases)

</details>
<details>
  <summary>Project</summary>
```
 __   __     ___                 _
 \ \ / /__  / __|_  _ _ __  _ __| |_  ___ _ _ _  _
  \ V / _ \ \__ \ || | '  \| '_ \ ' \/ _ \ ' \ || |
   |_|\___/ |___/\_, |_|_|_| .__/_||_\___/_||_\_, |
                 |__/      |_|                |__/


Welcome to Symphony Generator v2.7.1
Application files will be generated in folder: /home/user/code/wdk-bot
______________________________________________________________________________________________________
? Enter your pod host mycompany.symphony.com
? Enter your bot username wdk-bot
? Select your type of application Workflow Application (WDK)

Generating RSA keys...
   create rsa/publickey.pem
   create rsa/privatekey.pem
   create gradle/wrapper/gradle-wrapper.jar
   create gradle/wrapper/gradle-wrapper.properties
   create lib/Readme.txt
   create src/main/java/org/acme/workflow/MyActivity.java
   create src/main/java/org/acme/workflow/MyActivityExecutor.java
   create gradlew
   create gradlew.bat
   create README.md
   create workflows/Readme.txt
   create workflows/ping.swadl.yaml
   create build.gradle
   create application.yaml

Running ./gradlew botJar in your project
> Task :botJar

BUILD SUCCESSFUL in 992ms
1 actionable task: 1 executed

You can now update the service account wdk-bot with the following public key:

-----BEGIN RSA PUBLIC KEY-----
MIICCgKCAgEA2bwlI1o0RI4Kl4hEicZlQcaxWOqTCc/K7+YGVYl8x/HA2wrqYuAI
..
SDiOiG6q7NK1h7e3/sMrh4U/sf5SO9CFzYHcQP3M38XPfn8UMn0ovi0CAwEAAQ==
-----END RSA PUBLIC KEY-----

Please submit these details to your pod administrator.
If you are a pod administrator, visit https://mycompany.symphony.com/admin-console

Your workflow bot has been successfully generated !

</details>

## Configuration

The WDK is built on top of the BDK for Java Spring Starter, so the BDK configuration options will all still apply to WDK instances.

[BDK Configuration](https://docs.developers.symphony.com/bots/getting-started/config)

There is also WDK-specific configuration:

```yaml
wdk:
  workflows.path: .
  encrypt.passphrase: my-secret-passphrase
  properties:
    monitoring-token: my-monitoring-token
    management-token: my-management-token
  studio:
    github-token: my-github-token
    admins: 123456789,234567890

spring:
  datasource:    
    camunda.jdbc-url: jdbc:h2:file:./data/camunda;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    wdk.jdbc-url: jdbc:h2:file:./data/wdk;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

<table><thead><tr><th width="282">WDK Property</th><th>Description</th></tr></thead><tbody><tr><td>workflows.path</td><td>Path to directory where SWADL files are stored</td></tr><tr><td>encrypt.passphrase</td><td>Passphrase used to encrypt secrets</td></tr><tr><td>properties.monitoring-token</td><td>Token used to make calls to the Monitoring APIs</td></tr><tr><td>properties.management-token</td><td>Token used to make calls to the Management APIs</td></tr><tr><td>studio.github-token</td><td><a href="https://github.com/settings/tokens">GitHub token</a> used to browse <a href="https://github.com/finos/symphony-wdk-gallery">WDK Gallery</a> in WDK Studio</td></tr><tr><td>studio.admins</td><td>Comma-separated list of user IDs who will have administrative permissions in WDK Studio</td></tr></tbody></table>

<table><thead><tr><th width="279">Spring Property</th><th>Description</th></tr></thead><tbody><tr><td>datasource.camunda.jdbc-url</td><td>Connection string for workflow engine database</td></tr><tr><td>datasource.wdk.jdbc-url</td><td>Connection string for metadata database</td></tr></tbody></table>

## Create Service Account

For any bot to work, it requires a service account with a matching username and public key. The Symphony Generator creates a configuration file based on the answers supplied, including the bot username and the path to the generated key pair. These can be changed by modifying the `config.yaml` file. If you do not already have a service account set up, follow the instructions on this page to continue.

[Creating a Service account](https://docs.developers.symphony.com/bots/getting-started/creating-a-bot-user)

## Test your Bot

Create a sample workflow in `./workflows/ping.swadl.yaml`

```yaml
id: ping
activities:
  - send-message:
      id: pong
      on:
        message-received:
          content: /ping
      content: Pong
```

Launch the WDK bot using the respective command and wait for startup to complete

<details>
  <summary>Docker</summary>

```bash
$ ./startup.sh
```

</details>

<details>
  <summary>JAR</summary>

```bash
$ java -jar workflow-bot-app-1.6.3.jar # Update to acquired version
```
</details>

<details>
  <summary>Project</summary>

```bash
$ ./gradlew botJar
$ java -jar workflow-bot-app.jar
```
</details>

Launch Symphony, send a `/ping` message to the bot and ensure it replies with `Pong`.

## Build your Workflows

Now that you have a basic workflow project ready, you can proceed to find out more about building workflows using the WDK:

[Worfklow Developer Kit](./workflow-developer-kit.md)