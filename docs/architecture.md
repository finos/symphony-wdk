# Architecture

In this section we provide details on how the WDK is implemented. While we don't expect workflow developers to change
the workflow bot, knowing a bit more about its inner working helps understand its capabilities and limitations.

The WDK is made of:

- a definition language for workflows, SWADL. It comes with a JSON Schema to help with autocompletion, validation and
  documentation.
- a workflow bot, built on top of the Java BDK and running the workflow execution engine.

## Workflow engine

The WDK relies on Camunda as its execution engine for workflows.
[Camunda](https://camunda.com/products/camunda-platform/bpmn-engine/) is an open-source BPMN engine that is often used
for process automation, workflows and orchestration use cases. Reusing a battle-tested engine felt safer than
re-implementing our own. Camunda can be embedded in a Java application, with a Spring Boot integration so making it work
with the BDK Spring Boot starter was quite easy.

We made sure that the usage of Camunda stays an implementation detail and that another execution engine could be used
instead without impacting workflow developers. Hence, SWADL, a simpler and higher level to define workflows (compared to
BPMN).

To put it simply, deploying a workflow means taking a SWADL file translating it to BPMN and have Camunda run it. The
workflow bot also takes care of dispatching datafeed events as events understood by Camunda to trigger workflow
executions.

So what is inside the WDK in the end? A SpringBoot application made the BDK starter, embedding Camunda, validating SWADL
based on a JSON Schema definition and translating it to BPMN. Activities are implemented using the BDK services to call
Symphony APIs.

### Datafeed event dispatching and workflow execution

Since the BDK Spring Boot starter is used, a thread is started to listen to datafeed events. Events are then dispatched
as Camunda signals. Deployed workflows are waiting for the signals in an asynchronous manner. Camunda runs
a [job executor](https://docs.camunda.org/manual/7.15/user-guide/process-engine/the-job-executor/) in separate threads
that is polling for workflows ready to be executed.

It means that when an event is received or between activity transitions there might be a short delay (the Camunda job
executor is configured to run frequently by default to be as reactive as possible).

## Known limitations

The WDK, especially in its first version, obviously comes with trade-offs and limitations. The main one being that for
now an in-memory database is used to persist workflows. This means that restarting the workflow bot stops running
workflows and that they won't recover on restart. Executions history is lost on restart too. Camunda supports many
databases so in the future we might use a persistent one.

Related to the way persistence is implemented right now, high availability that can be achieved by running multiple
instances of the workflow bot is not possible. A unique database, shared by the bot instances would have to be used for
that.

## SWADL Conventions

To ensure the consistency of the SWADL format, below are the used conventions and principles.

In SWADL, the `kebab-case` is used to ease the readability of workflows. For instance activities or events are named
`send-message` or `message-received`.

For values such as activity ids or variables we recommend using the `camelCase` to distinguish those. Especially for
variables where the _-_ would have to be escaped.

### Activities

Activities are named following the format _do-something_, the first part of the name being a verb. _Get_ is commonly
used for activities returning results without side effects.

Plurals are used (such as `get-users`) when the activity returns a list of results.

### Events

Events are named following the format _something-happened_, the past is used to indicate that the event already
occurred. For instance: `message-received` or `user-joined-room`.

### Variables

To make sure that variables can be used anywhere in SWADL, the supported types for any attributes are usually a
primitive type + the string type.

For instance for the `get-user` activity:

```yaml
name: get-user
variables:
  userId: 123
activities:
  - get-user:
      id: get
        on:
          message-received:
            content: /get-user
      user-id: ${variables.userId}
```

While the API to get a user takes a number as the user id, in the JSON schema we still declare `user-id` to be of types
number **and** string to support the usage of variables as shown above.
