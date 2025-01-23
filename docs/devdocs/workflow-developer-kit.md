---
layout:
  title:
    visible: true
  description:
    visible: true
  tableOfContents:
    visible: true
  outline:
    visible: true
  pagination:
    visible: false
---

# Workflow Developer Kit

## Overview

The WDK is Symphony's low-code kit to accelerate building and evolving workflows. It is built on top of the BDK for Java Spring Starter but requires no Java development or compilation to get started. It also features an optional graphical user interface called _Studio_ to make workflow building even more intuitive.

* The WDK Github repo can be found here: [@finos/symphony-wdk](https://github.com/finos/symphony-wdk)
* The Workflow Certification course can be found here: [learn.symphony.com/bundles/workflow-developer-certification](https://learn.symphony.com/bundles/workflow-developer-certification)

## Getting Started
* Get Started Here: [Getting Started](./getting-started.md)

## Modes

WDK can operate in 2 modes: **File mode** and **API mode**. Both modes support the instant deployment and updating of workflows without requiring a restart, as well as the same set of Symphony actions/events and a monitoring API suite.

**File mode** is default, where a file directory is watched and any changes to files deploy automatically in real-time. This mode can be used either during development when only the workflow engine and a text editor are desired, or in production where file-based deployments are preferred over API-based deployments.

**API mode** uses APIs rather than files to deploy or update workflows, so definitions are stored in a database rather than in files.

There are other feature differences between the two modes, so pick the appropriate mode for each environment based on your requirements.

| Feature            | File Mode | API Mode |
| ------------------ | --------- | -------- |
| File Watcher       | ✅         | ❌        |
| Management APIs    | ❌         | ✅        |
| Version Control    | ❌         | ✅        |
| Secrets Management | ❌         | ✅        |

To toggle to **API mode**, set `wdk.workflows.path` in configuration to an empty string and set `wdk.properties.management-token` to a non-empty string.

> [!NOTE]
> WDK Studio is entirely API-driven so only **API mode** is supported

## Authentication

Authenticating your bot is made simple when using the WDK. Once you have your bot and Symphony environment properly configured, the WDK provides an out of the box implementation for authenticating your bot. You just need to ensure your `application.yaml` is valid.  The WDK loads in your config and authenticates your workflow bot. Once authenticated, your bot is ready to leverage the REST APIs in order to create rich automations and workflows on Symphony.

> [!NOTE]
> Note: You must have a corresponding service or bot account setup on your Symphony instance before authenticating. For more information navigate to the [Creating a Bot User](../bots/getting-started/creating-a-bot-user.md) guide.

#### On-Behalf-Of

WDK also supports OBO (On-Behalf-Of) pattern of authentication, allowing an authenticated bot + extension application to perform operations on behalf of a given user. The WDK's implementation makes it easy to perform supported operations on behalf of a given user.

> [!NOTE]
> Please follow our **Getting Started with OBO** guide using the link [here](https://docs.developers.symphony.com/building-extension-applications-on-symphony/app-authentication/obo-authentication#getting-started).\
\
The guide will cover all of the prerequisites needed for OBO and how to enable & upload the OBO extension application, the required permissions and how to ensure the OBO authentication process will work successfully.

Your activity that will be performing OBO actions will also need to include the `obo:`_`username`_ or `obo:`_`user-id`_ that the actions will be performed as.

```yaml
id: create-room-obo-workflow
activities:
- create-room:
      id: createRoomObo
      room-name: OBO created room
      room-description: Example of a room created with obo
      user-ids:
        - 734583310035744
        - 625588317732700
      obo:
        username: username@symphony.com
```

## Workflows

A Symphony workflow can be thought of as a sequence of operations or a repeatable pattern of activities that are organized together in order to transform data, provide a service, or process information. Each of these operations or activities may be completed by a single user, shared between a bot and a user, or shared between multiple actors including bots, users and even third-party systems.

A workflow _instance_ is created whenever a workflow's initiating event is triggered. Each workflow instance runs independently with its own set of instance data including variables and can be monitored separately.

## SWADL

The Symphony Workflow Automation Definition Language (SWADL) is the syntax used to write workflows in WDK. SWADL is based on YAML so the same rules on syntax, structure and indentation apply. This allows workflows to be defined in a declarative manner, providing access to most Symphony APIs in order to trigger on events and perform actions in response.

Workflows are executed by the embedded workflow engine. As a workflow developer, you simply write workflows in SWADL and provide them to the bot for execution.

SWADL is composed of activities, events, conditions and variables. An activity represents an action to be taken upon an event occuring subject to a condition. If an event is not defined, an activity simply executes right after completion of the previous activity. It is mandatory to declare an event only for the first activity in a workflow. Activities can also make use of variables which can be global, instance-specific or event-specific.

SWADL is part of the [JSON Schema Store](https://www.schemastore.org/json/), so code auto-completion and validation is readily available on Visual Studio Code, IntelliJ IDEA and all other supported editors (usually with the YAML extension or plugin installed).

Here's an example of a simple workflow that triggers upon receiving a `/hello` message and responds with `Hi There`.

```yaml
id: hello-workflow
variables:
  greeting: Hi There
activities:
  - send-message:
      id: sendHello
      on:
        message-received:
          content: /hello
      content: ${variables.greeting}
```

You can find more example workflows [here](https://github.com/finos/symphony-wdk/tree/master/docs/examples) and in the [Symphony WDK Gallery](https://github.com/finos/symphony-wdk-gallery).

## Activities

Activities are the building blocks of workflows, representing actions to be performed. The most commonly-used activity for a bot interacting with end-users is probably the `send-message` activity.

An activity usually takes some inputs, for instance the `send-message` activity will take the content of the message as an input.  Depending on the performed action it can generate [outputs](https://github.com/finos/symphony-wdk/blob/master/docs/concepts.md#outputs) such as details of the sent message for the `send-message` activity. Activities also have common properties such as an `id` which is used to reference it elsewhere in the workflow (e.g. to access the activity's outputs). Note that an activity's `id` needs to be an alphanumeric string and cannot contain symbols like dashes.

In a workflow, activities are often defined sequentially, one after another. Once an activity completes, the next one is executed. As a workflow developer, you might want to implement your own logic and reuse it in workflows.

#### Symphony Activities

Most activities that ship with WDK are Symphony activities that are bound to a specific Symphony REST API endpoint, for example `send-message` or `create-room`.

#### Utility Activities

There are also some activities that perform custom actions:

* `execute-script` allows custom Groovy script to be executed - mostly for manipulation of variables or data processing
* `execute-request` calls a third-party REST endpoint - mostly to fetch data from an external source or submit collated data to a downstream system

#### Custom Activities

Beyond the activities that ship with the WDK, you can also create [custom activities](https://github.com/finos/symphony-wdk/blob/master/docs/custom-activities.md) to extend the SWADL vocabulary ino performing custom actions. This requires the use of the [Project](../bots/getting-started/wdk.md#project) option when getting started.

## Events

A workflow requires at least one starting event, used to create a workflow instance from a deployed workflow and to start executing activities. This means the first activity of a workflow must define at least one event.

Workflow activities are executed sequentially by default, meaning the default event (if no others are defined) for an activity is the `activity-completed` one with completed activity id being the activity declared before.

```yaml
id: hello-bye-workflow
activities:
  - send-message:
      id: sendHello
      on:
        message-received:
          content: /hello
      content: Hello

  - send-message:
      id: sendBye
      content: Bye
```

In the example above, `sendHello` is executed first when a `/hello` message is sent, then `sendBye`.

Intermediate events can be defined too, for instance for a workflow when the user has to provide multiple inputs to move through the activities or if the workflow sent a form and is waiting for a reply.

```yaml
id: hello-bye2-workflow
activities:
  - send-message:
      id: sendHello
      on:
        message-received:
          content: /hello
      content: Hello
 
  - send-message:
      id: sendBye
      on:
        message-received:
          content: /bye
      content: Bye
```

In the example above, `sendHello` is executed first when a `/hello` message is sent, then the workflow waits for another message (`/bye`) to execute `sendBye`.

Most of the events a workflow will react on are datafeed events such as message received, user joined room, connection requested etc. The workflow bot is listening for any datafeed events its service account can receive and then dispatch them to the appropriate workflows. Other Event types can be found [here](https://github.com/finos/symphony-wdk/blob/master/docs/concepts.md#events).

### Forks and Joins

You can split workflow execution into parallel paths by defining multiple activities that trigger upon the completion of the same activity. To join the paths back, use the `all-of` construct to execute an activity after completion of all listed events. Alternatively, use the `one-of` construct if the workflow just needs to wait for any one of the listed events to complete execution rather than all of them.

```yaml
id: fork-workflow
activities:
  - send-message:
      id: hello
      on:
        message-received:
          content: /hello
      content: Hello!

  - send-message:
      id: forkOne
      on:
        activity-completed:
          activity-id: hello
      content: Fork One!
      
  - send-message:
      id: forkTwo
      on:
        activity-completed:
          activity-id: hello
      content: Fork Two!

  - send-message:
      id: forkJoin
      on:
        all-of: # or one-of
          - activity-completed:
              activity-id: forkOne
          - activity-completed:
              activity-id: forkTwo
      content: Joined!
```

### Webhooks

Webhooks are built-in to WDK by defining a `request-received` event as the starting event to any workflow.

```yaml
id: webhook-flow
activities:
  - send-message:
      id: greeting
      on:
        request-received:
          token: myToken
      to:
        stream-id: abc123
      content: ${event.args.content}
```

The workflow above can be triggered by firing a `POST` call to `/wdk/v1/workflows/webhook-flow/execute` with the `X-Workflow-Token` header matching the defined token value. Optionally, arguments can be supplied via a top-level `args` object.

<pre class="language-bash"><code class="lang-bash">curl --request POST 'http://127.0.0.1:8080/wdk/v1/workflows/webhook-flow/execute' \
<strong>  --header 'X-Workflow-Token: myToken' \
</strong>  --header 'Content-Type: application/json' \
  --data-raw '{ "args": { "content": "Hola from api" } }'
</code></pre>

## Variables

SWADL variables can be defined either at a workflow instance scope or shared scope to be accessible across instances and other workflows. Variables are also set by events and created as activity outputs for reference. Variables can be referenced directly in `execute-script` or by enclosing the reference with a dollar prefix and braces like so: `${variableName}`.

### Instance Variables

Instance variables exist within the execution of a single workflow instance and are not accessible by other workflows or persist across instance executions. They are prefixed with the variables scope e.g. `${variables.greeting}` and can be initialized with a value using the top-level `variables` section.

```yaml
id: instance-variable-workflow
variables:
  greeting: Hi There # Define and initialize greeting variable
activities:
  - send-message:
      id: sendHello
      on:
        message-received:
          content: /hello
      content: ${variables.greeting} # Reference greeting variable

  - execute-script:
      id: changeValue
      script: variables.greeting = "Goodbye" # Modify greeting variable value

  - send-message:
      id: sendGoodbye
      content: ${variables.greeting} # Reference revised greeting variable
```

### Shared Variables

Shared variables are organized into arbitrary namespaces, which need to be referenced whenever reading or writing a shared variable. The utility functions `readShared()` and `writeShared()` are used to read and write shared variables respectively. Note that the functions need to be prefixed with `wdk.` when calling them from `execute-script`.

```yaml
id: shared-variable-workflow
activities:
  - execute-script:
      id: init
      on:
        message-received:
          content: hi
      script: wdk.writeShared('hello-space', 'greeting', 'Hullo')

  - send-message:
      id: welcome
      content: ${readShared('hello-space', 'greeting')}
```

Once the above workflow has been executed _at least once_, the function `readShared('hello-space', 'greeting')` can be called from other workflows to retrieve the persisted value.

### Event Variables

Event variables differ in structure depending on the event type. Every time an awaited event is triggered, the variable named `event` gets replaced with the structure and contents of the latest event.

```yaml
id: event-variable-workflow
activities:
  - send-message:
      id: hello
      on:
        message-received:
          content: hi
      content: Hello ${event.initiator.user.displayName}!
```

To refer to an older event after a newer one has occured, give it an optional `id` and use that id to refer to the older event instead of the `event` variable.

```yaml
id: event-named-variable-workflow
activities:
  - send-message:
      id: hello
      on:
        message-received:
          id: init
          content: hi
      content: Hello First!

  - send-message:
      id: helloAgain
      on:
        message-received:
          content: again
      content: Hello Again, ${init.initiator.user.displayName}!
```

### Activity Outputs

After every activity completes execution, its outputs are appended to the object named after its `id` under the outputs field. You can then make use of outputs as inputs to other activities to pass the context around your workflow.

```yaml
id: activity-outputs-workflow
activities:
  - create-room:
      id: createRoom
      on:
        message-received:
          content: /create-room
      room-name: Brilliant Room
      room-description: Brilliant Room Description
      user-ids:
        - ${event.initiator.user.userId}

  - send-message:
      id: welcome
      to:
        stream-id: ${createRoom.outputs.roomId}
      content: Welcome to this new room!
```

### Secrets

Sensitive variables like API keys can also be stored using secrets. Secrets management is only supported in [API Mode](wdk.md#modes) as the Management APIs are used to manage secrets.

To create a new secret, make a POST request to `/v1/workflows/secrets`

```bash
curl --request POST 'http://127.0.0.1:8080/wdk/v1/workflows/secrets' \
  --header 'X-Management-Token: my-management-token' \
  --header 'Content-Type: application/json' \
  --data-raw '{ "key": "servicetoken", "secret": "supersecrettoken" }'
```

To reference a secret in SWADL, use the `secret()` utility function.

```yaml
id: secret-flow
activities:
  - execute-request:
      id: request
      headers:
        Authorization: ${secret('servicetoken')}
      url: https://some-url
```

## Conditions

Conditions can be defined either at the activity level or at the event level for `activity-completed` to either halt execution or perform a conditional fork.

#### Halt Execution

```yaml
id: halt-workflow
variables:
  proceed: no
activities:
  - send-message:
      id: sendHello
      on:
        message-received:
          content: /hello
      content: This gets sent

  - send-message:
      id: sendAgain
      if: ${variables.proceed == 'yes'}
      content: This does not get sent
```

This workflow terminates after the first activity completes since `variables.proceed` is always `no`.

#### Conditional Fork

```yaml
id: conditional-fork-workflow
activities:
  - send-message:
      id: hello
      on:
        message-received:
          id: choose
          content: /hello {proceed}
      content: Hello!

  - send-message:
      id: sendProceed
      on:
        activity-completed:
          activity-id: choose
      if: ${choose.args.proceed == 'yes'}
      content: Proceed!
      
  - send-message:
      id: sendNoProceed
      on:
        activity-completed:
          activity-id: choose
      if: ${choose.args.proceed == 'no'}
      content: No!
```

This workflow first sends `Hello!` in response to `/hello argument`, then either sends `Proceed!` if the argument was `yes`, `No!` if the argument was `no`, or sends nothing and terminates if the argument was anything else.

## Loops

Loops can be created by referencing the completion of a future activity along with a past activity using the `one-of` construct:

```yaml
id: loop-flow
variables:
  index: 0
activities:
  - send-message:
      id: one
      content: Hi There
      on:
        one-of:
          - message-received:
              content: /hello
          - activity-completed:
              activity-id: two
              if: ${variables.index < 5}

  - execute-script:
      id: two
      script: |
        variables.index++
```

This workflow initiates with a `/hello` message, sending the content `Hi There` for 5 times before terminating. It does this by progressing from `one` to `two`, which increments the `index` variable before looping back to `one`. The execution terminates once the `if` condition becomes unsatisfied when `index` increments to a value of 5.

> [!NOTE]
> When designing loops in workflows, always use conditions to determine when to continue execution in order to prevent infinite loops

## Debugging

For ease of debuggging, there is a standard `debug` activity you can use to print out the contents of any object, including variables, events or activity outputs. The `debug` activity does not require an `id` field and is only meant to be transient.

```yaml
id: debug-flow
activities:
  - debug:
      object: ${event}
      on:
        message-received:
          content: /hello
```

## SWADL Reference

For the complete SWADL technical reference, please refer to the following page.

[https://github.com/finos/symphony-wdk/blob/master/docs/reference.md](https://github.com/finos/symphony-wdk/blob/master/docs/reference.md)