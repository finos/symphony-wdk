# SWADL reference

This reference documentation covers the Symphony Workflow Automation Definition Language (SWADL)
defined as a [JSON schema](../workflow-language/src/main/resources/swadl-schema-1.0.json).

SWADL is based on YAML, written in files usually suffixed with _.swadl.yaml_. If you are not familiar with YAML, you can
read about its syntax on [Wikipedia](https://en.wikipedia.org/wiki/YAML#Syntax).

In SWADL, at the top level, you mainly define the activities part of the workflow.

Key     | Type    | Required |
--------------|---------|----------| 
[id](#id)            | String  | Yes    |
[variables](#variables)     | Map     | No     |
[activities](#activities)    | List   | Yes     |

Example:

```yaml
id: myWorkflow
variables:
  myVar: "aValue"
activities:
  - send-message:
      id: myActivity
```

## id

Workflow's id should start with a letter and should not contain empty spaces. It is required. The id will appear in logs and audit trails.

## variables

Variables are accessible and editable within the entire workflow. A map of key/value entries is expected. Simple types
such as numbers, string and booleans as well as lists and maps are supported.

Variables can be accessed elsewhere in the workflow with the `${variables.VARIABLE_NAME}` syntax. They are also
accessible from [custom activities](./custom-activities.md) and [script activities](#execute-script).

While variables can be defined in SWADL, each instance of a workflow holds its own copy of the variables. Activities can
change variables and those changes will be visible by other activities.

Examples:

```yaml
variables:
  var1: 123
  var2: "my text"
  var3: my text
  aMap:
    key1: value1
    key2: value2
  list1:
    - item1
    - item2
```

## activities

A list of activities to be executed by the workflow. By default, the execution is sequential but complex logic can be
orchestrated with if/else conditions ([if](#if)) as well as on/[activity-completed](#activity-completed)
events.

[Built-in activities](#built-in-activities) are provided to support most of the public APIs of Symphony.

Activities have common keys listed below:

Key | Type | Required |
------------ | -------| --- | 
[id](#activity-id) | String | Yes |
[on](#on) | Map | No |
[if](#if) | String | No |
[else](#else) | Map | No |

Activities also produce outputs that are accessible via `${ACTIVITY_ID.outputs.OUTPUT_NAME}`. For each activity
referenced [below](#built-in-activities), its outputs are described too with a reference to the underlying BDK object.
The [welcome-bot](./examples/welcome-bot.swadl.yaml) example shows how outputs can be used.

### <a name="activity-id"></a>id (activity)

Activity's identifier should be unique across workflows. As it can be used as a variable identifier only alphanumeric
characters are allowed (`_` can be used as a separator). _variables_ cannot be used as an activity id.

Example:

```yaml
activities:
  - send-message:
      id: myActivity
      # outputs of this activity can be referenced as ${myActivity.outputs.message}
```

### on

Events that can trigger the activity execution. **The first activity of a workflow is expected to have an event**.

`on` can contain either a specific event directly or a list of events as part of the `one-of` key.

[List of real-time events](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events)

Key     | Type    | Required |
--------------|---------|----------| 
[Typed Event](#events)    | Map   | No     |
[one-of](#one-of)     | List     | No     |
[timeout](#timeout)  | String  | No    |

Examples:

```yaml
activities:
  - send-message:
      id: myActivity
      on:
        # because there are no attributes for this event, an empty map {} is used
        user-joined-room: { }
```

```yaml
activities:
  - send-message:
      id: myActivity
      on:
        one-of:
          - user-joined-room: { }
          - user-left-room: { }
```

The last captured event coming from Datafeed in a workflow is bound to a variable accessible via `${event}`.
This `event` variable holds
the [initiator](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4Initiator.html)
of the event and the source, i.e. the payload
listed [here](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events).

Checkout the [welcome bot example](./examples/welcome-bot.swadl.yaml) to see how the `${event}` can be used.

#### timeout

Timeout while waiting for an event, expressed as
an [ISO 8601 duration](https://en.wikipedia.org/wiki/ISO_8601#Durations). Upon expiration, another activity can be
triggered with an [activity-expired](#activity-expired) event.

Default value for [form-replied](#form-replied) is 24 hours. No default value for other events.

Example: _PT60S_ for a 60 seconds timeout.

#### one-of

Used to receive at least one of the listed events. Multiple events can be listed but a given time only one will trigger
the activity. It can be either one of them.

### if

Conditional execution of the activity based on a boolean expression. Workflow variables can be used within the
expression. This condition applies to all the events that can start the activity.

The first activity of a workflow cannot have an `if` condition (to avoid triggering workflows and then have to evaluate
the first condition and stop the workflow without any activity being executed).

```yaml
activities:
  - send-message:
      id: myActivity
      if: ${variables.myVar == '123'}
```

### else

Default execution of the activity when `if` conditions are used for previous activities. This is an empty object. This
is combined with an `activity-completed` event to properly chain the `else` activity with the activity before the `if`
branch of the workflow.

Example:

```yaml
activities:
  - send-message:
      id: myActivity
      on:
        activity-completed:
          activity-id: BEFORE_IF_ACTIVITY_ID
      else: { }
```

## Events

Below are all the supported events under the `on` or the `one-of` keys.

### message-received

Generated when a message is sent in an IM, MIM, or chatroom of which the workflow bot is a member, including messages
sent by the user him/herself.

Key | Type | Required |
------------ | -------| --- | 
[content](#content) | String | No |
[requires-bot-mention](#requires-bot-mention) | Boolean | No |

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4MessageSent.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#message-sent)

Example:

```yaml
activities:
  - send-message:
      id: myActivity
      on:
        message-received:
          content: /run
```

#### content

Message content to listen to. Can be a simple string, usually a /command to trigger a workflow. It can also be a
template like `/run {myArg}` where `myArg` will be bound to `${event.args.myArg}`.

Templates are handled as Ant-matching expressions, similar to Spring Boot controllers. Multiple patterns can be used,
for instance `/run {myArg1} {myArg2}` with whitespace being used as a separator. User mentions or tags can be captured
too.

Example:

```yaml
activities:
  - send-message:
      id: myActivity
      on:
        message-received:
          # dash for the hashtag is escaped otherwise it is a comment in YAML
          # $ for the cash tag is escaped otherwise it is a variable handled as an expression by Camunda
          content: /go {arg1} @{user} \#{hash} \${cash}
          # to write it as a string with quotes, the escaping is a bit different
          # content: "/go {arg1} @{user} \\#{hash} \\${cash}"
      to:
        stream-id: A_STREAM
      content: Received ${event.args.arg1}, ${event.args.user}, ${event.args.hash}, ${event.args.cash}
```

The [contact sharing](./examples/contact-sharing.swadl.yaml) workflow from the examples uses templated `content` to
match messages. It uses a function named `mentions` to retrieve the mentioned user ids. Similar functions
from [MessageParser](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/core/service/message/util/MessageParser.html)
are exposed too.

Examples:

- `${mentions(event)}` returns the list of mentioned user ids from a `message-received` event
- `${mentions(event)[0]}` to get the first mentioned user id from a `message-received` event
- `${cashTags(event)}` returns the first cashtag from a `message-received` event
- `${hashTags(event)}` returns the first hashtag from a `message-received` event
- `${emojis(event)}` returns a map of the emojis from a `message-received` event

#### requires-bot-mention

If true, the event is only triggered if the bot is mentioned.

### form-replied

Generated when a user replies to a bot message that contains an interactive form with UX components such as text fields,
radio buttons, checkboxes, person selectors and more.

As multiple users can reply to a form sent to a room, the activity waiting for a `form-replied` can be executed multiple
times (it can be seen as a sub execution of the current workflow). This also means that every activity defined after
this activity or with a `activity-completed` event on this activity will run within its own sub execution.

A form can be dedicated to be replied only once. In this case, `unique` is set to `true`.

_nb: Loops are only supported with forms that require only one reply._


Key | Type | Required |
------------ | -------| --- | 
[form-id](#form-id) | String | Yes |
[unique](#unique) | String | No |

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4SymphonyElementsAction.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#symphony-elements-action)

Example:

```yaml
activities:
  - send-message:
      id: sendForm
      on:
        message-received:
          content: "/message"
      content: |
        <messageML>
          <form id="sendForm">
            <text-field name="aField" placeholder="Anything you want to say" required="true"/>
            <button name="send-answers" type="action">Send</button>
            <button type="reset">Clear</button>
          </form>
        </messageML>

  - send-message:
      id: pongReply
      on:
        form-replied:
          # form id is the same as the activity's id above and the same as the id in the original form's MessageML
          form-id: sendForm
      content: ${sendForm.aField}
```

An advanced usage of forms can be found in the examples: [simple poll bot](./examples/poll-bot.swadl.yaml).

#### form-id

The id should be the same as the activity's one that sent the form.

#### unique

Boolean specifying whether the form can be replied once or multiple replies are expected. It is `false` by default.

### message-suppressed

Generated when messages are suppressed.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4MessageSuppressed.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#messages-suppressed)

### post-shared

Generated when either the workflow bot shares a wall post written by another user or another user shares a wall post
written by the workflow bot.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4SharedPost.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#shared-wall-posts)

### im-created

Generated when an IM or MIM is created with the workflow bot as a member, initiated either by the workflow bot or
another user.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4InstantMessageCreated.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#im-mim-created)

### room-created

Generated when a room is created by the workflow bot.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4RoomCreated.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#room-created)

### room-updated

Generated when a room of which the workflow bot is a member is updated, including rooms updated by the user him/herself.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4RoomUpdated.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#room-updated-message)

### room-deactivated

Generated when a room of which the workflow bot is a member is deactivated, including rooms deactivated by the user
him/herself.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4RoomDeactivated.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#room-deactivated-message)

### room-reactivated

Generated when a room of which the workflow bot is a member is reactivated, including rooms reactivated by the user
him/herself.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4RoomReactivated.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#room-reactivated-message)

### room-member-promoted-to-owner

Generated when a user is promoted from a participant to an owner of a room of which the workflow bot is a member,
including when the user himself is promoted to an owner or promotes another user.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4RoomMemberPromotedToOwner.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#room-member-promoted-to-owner)

### room-member-demoted-from-owner

Generated when a user is demoted from an owner to a participant of a room of which the workflow bot is a member,
including when the user himself is demoted to a participant or demotes another user.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4RoomMemberDemotedFromOwner.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#room-member-demoted-from-owner)

### user-joined-room

Generated when a new user joins or is added to a room of which the workflow bot is a member, including when the user
himself joins or is added to a room.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4UserJoinedRoom.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#user-joined-room)

### user-left-room

Generated when a user leaves or is removed from a room of which the workflow bot is a member, including when the user
himself leaves or is removed from a room.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4UserLeftRoom.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#user-left-room)

### user-requested-join-room

Generated when a user requests to join a room. Only the user who requested to join the room and the owners of that room
will receive this event on their datafeeds.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4UserRequestedToJoinRoom.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#user-requested-to-join-room)

### connection-requested

Generated when a connection request is sent, either by the workflow bot to another user or to the workflow bot by
another user.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4ConnectionRequested.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#connection-requested)

### connection-accepted

Generated when a connection request is accepted, either sent by the workflow bot and accepted by another user or sent by
another user and accepted by the workflow bot.

[Payload reference](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4ConnectionAccepted.html)

[Payload example](https://docs.developers.symphony.com/building-bots-on-symphony/datafeed/real-time-events#connection-accepted)

### activity-expired

Generated when the given activity event [timeout](#timeout) has expired. **Note this is not a Datafeed real-time
event.**

This is usually used for forms when a specific activity is triggered upon expiration (to warn the user the form is no
longer valid or to collect results).

Key | Type | Required |
------------ | -------| --- | 
[activity-id](#expired-activity-id) | String | Yes |

Example:

```yaml
activities:
  - send-message:
      id: sendForm
      on:
        message-received:
          content: "/message"
      content: |
        <messageML>
          <form id="sendForm">
            <text-field name="aField" placeholder="Anything you want to say" required="true"/>
            <button name="send-answers" type="action">Send</button>
            <button type="reset">Clear</button>
          </form>
        </messageML>

  - send-message:
      id: pongReply
      on:
        # Short timeout to wait for a reply, 10 seconds only
        timeout: PT10S
        form-replied:
          form-id: sendForm
      content: ${sendForm.aField}

  - send-message:
      id: expiration
      on:
        activity-expired:
          activity-id: pongReply
      # If no reply is provided for the form after 10 seconds then this activity is executed
      content: Form expired!
```

#### <a name="expired-activity-id"></a>activity-id

The id of the activity which expiration's triggers the event.

### activity-completed

Generated when the given activity is completed. **Note this is not a Datafeed real-time event.**

This is used to connect activities together when a non-sequential ordering is required. For instance to build if/else
if/else branching workflows or loops.

If no event are set for a given activity this is the default event that will be used with the `activity-id` being the
previously declared activity (hence the sequential ordering by default).

Key | Type | Required |
------------ | -------| --- | 
[activity-id](#completed-activity-id) | String | Yes |
[if](#completed-if) | String | No |

Example:

```yaml
id: ifElseIfElse
variables:
  foo: fuzz
activities:
  - send-message:
      id: start
      on:
        message-received:
          content: /execute
  - send-message:
      id: if
      # will not be executed because of the if
      if: ${variables.foo == 'bar'}
      content: If
  - send-message:
      id: elseif
      on:
        activity-completed:
          activity-id: start
      # will be executed because of the if
      if: ${variables.foo == 'fuzz'}
      content: If else
  - send-message:
      id: else
      on:
        activity-completed:
          activity-id: start
      # will not be executed because the else if was executed
      else: { }
      content: Else
```

Example:

```yaml
id: loop
variables:
  execution: 0
activities:
  - send-message:
      id: start
      on:
        one-of:
          # a slash command is used to trigger the workflow 
          - message-received:
              content: /execute
          # but this activity (once the workflow is running) can also start after the loop activity is executed
          - activity-completed:
              activity-id: loop
              # loop 2 times
              if: ${variables.execution <= 1}
      content: Loop
  - execute-script:
      id: loop
      # we increment the loop counter
      script: |
        variables.execution++
```

#### <a name="completed-activity-id"></a>activity-id

The completed (without errors) activity triggering this event.

#### <a name="completed-if"></a>if

Similar to the [if](#if) at `on` level but applied to a single completed activity.

### activity-failed

Generated when the given activity has failed. **Note this is not a Datafeed real-time event.**

Exceptions raised by activities will trigger this event. This gives a way to let the user know about a failure.

Key | Type | Required |
------------ | -------| --- | 
[activity-id](#failed-activity-id) | String | Yes |

Example:

```yaml
activities:
  - send-message:
      id: sendHello
      on:
        message-received:
          content: /execute
      to:
        stream-id: NON_EXISTING_STREAM
      content: Hello
  - send-message:
      id: errorHandling
      on:
        activity-failed:
          activity-id: sendHello
      content: Sending a message failed
```

#### <a name="failed-activity-id"></a>activity-id

The failing activity triggering this event.

### timer-fired

Timer based event. It is either triggered at a given point in time using the keyword `at` or repeated using the
keyword `repeat`. **Note this is not a Datafeed real-time event.**

Key | Type | Required |
------------ | -------| --- | 
[at](#at) | String | Yes |
[repeat](#repeat) | String | Yes |

_Only one of the key `at` or `repeat` can be set._

#### at

[ISO 8601 date](https://en.wikipedia.org/wiki/ISO_8601#Times) representing a point in time when the current activity
should be executed.

Example: _2021-08-31T-15:50:00_

#### repeat

[ISO 8601 repeating intervals](https://en.wikipedia.org/wiki/ISO_8601#Repeating_intervals) to repeat the current
activity.

It can be used on the first activity of a workflow to make its execution periodic (like a cron job).

Examples:

- _R/PT10S_ to repeat an activity every 10 seconds
- _R/1970-01-01T00:00:00Z/P1D_ to repeat every day at midnight

### request-received

Generated when an HTTP request is received in order to trigger a workflow. The workflow bot exposes an API that can be
called by external users to manually trigger a workflow execution. This event can only be used on the first activity of
a workflow.

Key | Type | Required |
------------ | -------| --- | 
[token](#token) | String | Yes |

Example:

```yaml
id: myWorkflow
activities:
  - send-message:
      id: sendMsg
      on:
        request-received:
          token: myToken
      to:
        stream-id: A_STREAM
      content: ${event.args.content}
```

The workflow above is triggered when an HTTP request is sent to the bot. Arguments can be passed along the request and
retrieved in the `${event.args}` variable. For instance:

```
curl --request POST 'http://127.0.0.1:8080/wdk/v1/workflows/myWorkflow/execute' \
--header 'X-Workflow-Token: myToken' \
--header 'Content-Type: application/json' \
--data-raw '{
  "args": {
    "content": "Hola from api"
  }
}
'
```

#### token

Token to authorize incoming HTTP requests. This token should be passed when calling the HTTP API to trigger the workflow
in the `X-Workflow-Token` header. This is a shared secret between the workflow writer and the users that can trigger the
workflow via API.

## <a name="built-in-activities"></a>Built-in activities

Below are all the supported activities that can be listed under the `activities`
key. [Custom activities](./custom-activities.md) can be defined too.

### send-message

Posts a message to a stream. Probably the most commonly used activity to interact with end-users.

Key | Type | Required |
------------ | -------| --- | 
[to](#to) | Map | No |
[content](#send-message-content) | String/Object | Yes |
[attachments](#attachments) | List | No |

Output | Type |
----|----|
message | [V4Message](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4Message.html)
msgId | String

[API reference](https://developers.symphony.com/restapi/reference#create-message-v4)

Example:

```yaml
activities:
  - send-message:
      id: myActivity
      to:
        stream-id: ID_OF_A_STREAM
      content: Hello!
```

#### to

The recipient (conversation, IM, MIM or chatroom) in which the message should be posted.

If not set, the stream associated with the latest received event is used. This makes replying to a command sent by a
user easy.

Key | Type | Required |
------------ | -------| --- | 
[stream-id](#stream-id) | String | Yes |
[stream-ids](#stream-ids) | String | Yes |
[user-ids](#user-ids) | Map | Yes |

_If `to` is used then one of the key `stream-id` or `stream-ids` or `user-ids` must be set._

Example:

```yaml
activities:
  - send-message:
      id: myActivity
      on:
        message-received:
          content: /hello
      # to is not set, the message is sent to the same stream as the received /hello message (i.e. the bot simply replies to the user)
      content: Hello!
```

##### stream-id

Stream id to send the message to. Both url safe and base64 encoded urls are accepted.

##### stream-ids

Stream ids to send the blast message to. Both url safe and base64 encoded urls are accepted.

##### user-ids

Users to send the message to. An IM or MIM stream will be created or reused.

#### <a name="send-message-content"></a>content

The content of the message
in [MessageML](https://docs.developers.symphony.com/building-bots-on-symphony/messages/overview-of-messageml) format.
Must contain at least one space. **In case the content is a form, the latter's id should be the same as the send-message
activity one.**

Content can
be [MessageML](https://docs.developers.symphony.com/building-bots-on-symphony/messages/overview-of-messageml) with
the `<messageML>` tags or can be simple text too (<messageML> are automatically added if needed).

Content can either be set directly in the SWADL file as plain text (String) or it can be referenced from an external file (Object).
When using an external file the content has to be defined as follows:

Key | Type | Required |
------------ | -------| --- | 
template | String | Yes |

Both [Freemarker](https://freemarker.apache.org/) (.ftl) and mml.xml format are accepted as external files in the template field. 
By default, it will search for the file in the `./workflows` root folder. 
When [Freemarker](https://freemarker.apache.org/) is used, any workflow variable can be referenced in the external file, 
same format as it is for the any other activity in the SWADL file.

In case the content to send is PresentationML. The `text` function might come handy, it uses
the [PresentationMLParser](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/core/service/message/util/PresentationMLParser.html)
from the BDK to extract the text content of a PresentationML message.

Example:

```yaml
activities:
  - send-message:
      id: echo
      on:
        message-received:
          content: /echo
      content:
        ${text(event.source.message.message)}
```
Example using Freemarker:
```yaml
activities:
  variables:
    val: world
  - send-message:
      id: echo
      on:
        message-received:
          content: /echo
      content: 
        template: message-with-params.ftl
```
message-with-params.ftl 
```
<messageML>Hello ${variables.val}</messageML>
```

#### attachments

One or more attachments to be sent along with the message. It can be either an existing attachment from another message
or a file local to the bot. Previews are not supported.

Key | Type | Required |
------------ | -------| --- | 
[message-id](#message-id) | String | Yes |
[attachment-id](#attachment-id) | Map | No |

or

Key | Type | Required |
------------ | -------| --- |
[content-path](#content-path) | String | Yes |

Example: [forwarding attachments](./examples/forward-attachments.swadl.yaml)

##### message-id

Message id having the attachment to forward. Both url safe and base64 encoded urls are accepted.

##### attachment-id

Attachment id to forward. If not set, all attachments in the provided message are forwarded. Both url safe and base64
encoded urls are accepted.

##### content-path

Path to the file to be attached to the message. The path is relative to the workflows folder.

### update-message
Update an existing message into a stream. Returns the new updated message.

Key | Type | Required |
------------ | -------| --- |
[message-id](#update-message-id) | String | Yes |
[content](#content) | String/Object| Yes |

Output | Type |
----|----|
message | [V4Message](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4Message.html)
msgId | String

#### <a name="update-message-id"></a> message-id

Message id of the message to be updated. Both url safe and base64 encoded urls are accepted

### pin-message
Pin an existing message into the stream it belongs to. It works for both Instant Messages and Rooms.

Key | Type | Required |
------------ | -------| --- |
message-id | String | Yes |

Depending on the stream type the activity will either call the
[Update Room](https://developers.symphony.com/restapi/reference#update-room-v3) or the
[Update IM](https://developers.symphony.com/restapi/v20.13/reference#update-im) endpoint.

### unpin-message
Unpin any message (if present) from an existing stream. It works for both Instant Messages and Rooms.

Key | Type | Required |
------------ | -------| --- |
stream-id | String | Yes |

Depending on the stream type the activity will either call the
[Update Room](https://developers.symphony.com/restapi/reference#update-room-v3) or the
[Update IM](https://developers.symphony.com/restapi/v20.13/reference#update-im) endpoint.

### get-message

Returns a message found by id.

Key | Type | Required |
------------ | -------| --- |
message-id | String | Yes |

Output | Type |
----|----|
message | [V4Message](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4Message.html)

[API reference](https://developers.symphony.com/restapi/reference#get-message-v1)

### get-messages

Get messages from an existing stream (IM, MIM, or chatroom). Additionally, returns any attachments associated with the
message.

Key | Type | Required |
------------ | -------| --- |
stream-id | String | Yes |
[since](#since) | String | Yes |
[limit](#limit) | Number | No |
[skip](#skip) | Number | No |

Output | Type |
----|----|
rooms | List of [V4Message](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V4Message.html)

[API reference](https://developers.symphony.com/restapi/reference#messages-v4)

#### since

Date, in [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601#Times) format, of the earliest possible data of the first
message returned.

Example: _2021-08-31T15:50:00Z_

### get-attachment

Downloads an attachment to file system and returns its path. The stored file is suffixed with the executed activity id and stored under a folder with the executed process id.

An attachment named **_logo.png_** will be stored under _./workflows/**$PROCESS_ID**/**$ACTIVITY_ID**-logo.png_.

_nb: The stored files are not cleaned when the workflow stops and it is up to workflow's developer to do it._

Key | Type | Required |
------------ | -------| --- |
message-id | String | Yes |
attachment-id | String | Yes |

Output | Type |
----|----|
attachmentPath | String

[API reference](https://developers.symphony.com/restapi/reference#attachment) (Activity does not return the same outputs as the api response)

### create-room

Creates a new chatroom.

Key | Type | Required |
------------ | -------| --- | 
[room-name](#room-name) | String | Yes |
[room-description](#room-description) | String | Yes |
[user-ids](#create-room-user-ids) | List | Yes |
[public](#public) | Boolean | No |

_Either `room-name` and `room-description` are set, or `user-ids` are._

Output | Type |
----|----|
roomId | String

[API reference](https://developers.symphony.com/restapi/reference#create-room-v3)

Examples:

```yaml
activities:
  - create-room:
      id: createPublicRoom
      room-name: "A public room"
      room-description: "With a description"
      public: true
```

```yaml
activities:
  - create-room:
      id: createMim
      user-ids:
        - 123
        - 456
        - 789
```

#### room-name

Room's name. Room names will be considered the same if they only differ in capitalization and whitespace. E.g. "room1"
and "R O O M 1" are considered the same. Also, room names must be shorter than 50 characters.

#### room-description

Room's description.

#### <a name="create-room-user-ids"></a>user-ids

List of user ids as strings. Used to create an IM or MIM room (i.e. no name, nor description can be set).

#### public

If true, this is a public chatroom. If false, a private chatroom. Note: Once this value is set for a room, it is
read-only and can’t be updated.

### update-room

Updates the attributes of an existing chat room.

Key | Type | Required |
------------ | -------| --- | 
[stream-id](#update-room-stream-id) | String | Yes |
[room-name](#room-name) | String | Yes |
[room-description](#room-description) | String | Yes |
[keywords](#keywords) | Map | Yes |
[members-can-invite](#members-can-invite) | Boolean | No |
[discoverable](#discoverable) | Boolean | No |
[public](#public) | Boolean | No |
[read-only](#read-only) | Boolean | No |
[copy-protected](#copy-protected) | Boolean | No |
[cross-pod](#cross-pod) | Boolean | No |
[view-history](#view-history) | Boolean | No |
[multilateral-room](#multilateral-room) | Boolean | No |
[active](#active) | Boolean | No |

Output | Type |
----|----|
room | [V3RoomDetail](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V3RoomDetail.html)

[API reference](https://developers.symphony.com/restapi/reference#update-room-v3)

Example:

```yaml
activities:
  - update-room:
      id: activateRoom
      stream-id: A_STREAM_ID
      active: true
```

#### <a name="update-room-stream-id"></a>stream-id

Stream's id to update. Both url safe and base64 encoded urls are accepted.

#### keywords

A list of key-value pairs, describing additional properties of the room.

#### members-can-invite

If true, any chat room participant can add new participants. If false, only owners can add new participants.

#### discoverable

If true, this chat room (name, description and messages) non-participants can search for this room. If false, only
participants can search for this room.

#### read-only

If true, only stream owners can send messages. Note: Once this value is set for a room, it is read-only and can’t be
updated.

#### copy-protected

If true, users cannot copy content from this room. Note: Once this value is set to true for a room, it is read-only and
can’t be updated.

#### cross-pod

If true, this room is a cross-pod room.

#### view-history

If true, new members can view the room chat history of the room.

#### multilateral-room

If true, this is a multilateral room where users belonging to more than 2 companies can be found.

#### active

If false, the room is not active anymore.

### add-room-member

Adds new members to an existing room.

Key | Type | Required |
------------ | -------| --- | 
stream-id | String | Yes |
user-ids | List | Yes |

[API reference](https://developers.symphony.com/restapi/reference#add-member)

Example:

```yaml
activities:
  - add-room-member:
      id: addRoomMember
      stream-id: A_STREAM_ID
      user-ids:
        - 123
        - 456
```

### remove-room-member

Removes members from an existing room.

Key | Type | Required |
------------ | -------| --- | 
stream-id | String | Yes |
user-ids | List | Yes |

[API reference](https://developers.symphony.com/restapi/reference#remove-member)

### promote-room-owner

Promotes user to owner of the chat room.

Key | Type | Required |
------------ | -------| --- | 
stream-id | String | Yes |
user-ids | List | Yes |

[API reference](https://developers.symphony.com/restapi/reference#promote-owner)

### demote-room-owner

Demotes room owner to a participant in the chat room.

Key | Type | Required |
------------ | -------| --- | 
stream-id | String | Yes |
user-ids | List | Yes |

[API reference](https://developers.symphony.com/restapi/reference#demote-owner)

### get-stream

Returns information about a particular stream.

Key | Type | Required |
------------ | -------| --- |
stream-id | String | Yes |

Output | Type |
----|----|
stream | [V2StreamAttributes](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V2StreamAttributes.html)

[API reference](https://developers.symphony.com/restapi/reference#stream-info-v2)

Example:

- `${ACTIVITY_ID.outputs.stream.roomAttributes.name}` to access the stream's name.

### get-room

Returns information about a particular chat room.

Key | Type | Required |
------------ | -------| --- |
stream-id | String | Yes |

Output | Type |
----|----|
room | [V3RoomDetail](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V3RoomDetail.html)

[API reference](https://developers.symphony.com/restapi/reference#room-info-v3)

Example:

- `${ACTIVITY_ID.outputs.room.roomAttributes.name}` to access the room's name.

### get-streams

Returns a list of all the streams (IMs, MIMs, and chatrooms) for the calling user's company, sorted by creation date (
ascending – oldest to newest).

Key | Type | Required |
------------ | -------| --- |
[types](#get-streams-types) | List | No |
[scope](#scope) | String | No |
[origin](#origin) | String | No |
[privacy](#privacy) | String | No |
[status](#status) | String | No |
[start-date](#start-date) | String | No |
[end-date](#end-date) | String | No |
[limit](#limit) | Number | No |
[skip](#skip) | Number | No |

Output | Type |
----|----|
streams | [V2AdminStreamList](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V2AdminStreamList.html)

[API reference](https://developers.symphony.com/restapi/reference#list-streams-for-enterprise-v2)

#### <a name="get-streams-types"></a>types

A list of stream types that will be returned (IM, MIM, ROOM). If not specified, streams of all types are returned.

#### scope

The scope of the stream: INTERNAL (restricted to members of the calling user's company) or EXTERNAL (including members
of the calling user's company, as well as another firm). If not specified, returns streams of either scope.

#### origin

The origin of the room: INTERNAL (created by a user of the calling user's company) or EXTERNAL (created by a user of
another company). If not specified, returns streams of either origin. Only applies to chatrooms with External scope.

#### privacy

The privacy setting of the room: PRIVATE (members must be added) or PUBLIC (anyone can join). If not specified, returns
both private and public rooms. Only applies to chatrooms with internal scope.

#### status

The status of the room: ACTIVE or INACTIVE. If not specified, both active and inactive streams are returned.

#### start-date

Restricts result set to rooms that have been modified since this
date ([ISO 8601](https://en.wikipedia.org/wiki/ISO_8601#Times)). When specified along with end-date, enables the
developer to specify rooms modified within a given time range.

#### end-date

Restricts result set to rooms that have been modified prior to this
date ([ISO 8601](https://en.wikipedia.org/wiki/ISO_8601#Times)). When specified along with start-date, enables the
developer to specify rooms modified within a given time range.

### get-rooms

Search for rooms, querying name, description, and specified keywords.

Key | Type | Required |
------------ | -------| --- |
query | String | Yes |
[labels](#labels) | List | No |
[active](#get-rooms-active) | Boolean | No |
[private](#private) | Boolean | No |
[creator-id](#creator-id) | Number | No |
[owner-id](#owner-id) | Number | No |
[sort-order](#sort-order) | Number | No |
[limit](#limit) | Number | No |
[skip](#skip) | Number | No |

Output | Type |
----|----|
rooms | [V3RoomSearchResults](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V3RoomSearchResults.html)

[API reference](https://developers.symphony.com/restapi/reference#search-rooms-v3)

#### query

The query which is searched for in room name, description, and optionally keywords. Case-insensitive.

#### labels

A list of room keywords whose values will be queried.

#### <a name="get-rooms-active"></a>active

If true, it restricts the search to active rooms. If false, it restricts the search to inactive rooms. If not specified,
it includes both active and inactive rooms. Note that for inactive rooms, only rooms where the calling user is a member
will be in the search scope; read the box “Room Search Scope” for further details.

#### private

If true, it includes only private rooms in the search results. If false, only public rooms are returned. If not
specified, both public and private rooms are returned. Note that for inactive rooms, only discoverable rooms and rooms
where the calling user is a member will be in the search scope; read the box “Room Search Scope” for further details.

#### creator-id

If provided, restrict the search to rooms created by the specified user.

#### owner-id

If provided, restrict the search to rooms owned by the specified user.

#### member-id

If provided, restrict the search to rooms where the specified user is a member.

#### sort-order

Sort algorithm to be used. Supports two values: BASIC (legacy algorithm) and RELEVANCE (enhanced algorithm).

### get-user-streams

Returns a list of all the streams of which the requesting user is a member, sorted by creation date (ascending - oldest
to newest).

Key | Type | Required |
------------ | -------| --- |
[types](#types) | List | No |
[include-inactive-streams](#include-inactive-streams) | Boolean | No |
[limit](#limit) | Number | No |
[skip](#skip) | Number | No |

Output | Type |
----|----|
streams | List of [StreamAttributes](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/StreamAttributes.html)

[API reference](https://developers.symphony.com/restapi/reference#list-user-streams)

#### types

A list of stream types that will be returned. If not specified, all types of streams are returned. Allowed values:

- IM
- MIM
- ROOM
- POST

#### include-inactive-streams

Whether to include inactive conversations. A chatroom is inactive if it has been deactivated by an owner or admin. An IM
or MIM is inactive if one of the participating users has been deactivated by an admin. If not specified, only active
streams are returned.

### get-stream-members

Returns a list of all the current members of a stream (IM, MIM, or chatroom).

Key | Type | Required |
------------ | -------| --- |
stream-id | String | Yes |
[limit](#limit) | Number | No |
[skip](#skip) | Number | No |

Output | Type |
----|----|
members | [V2MembershipList](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V2MembershipList.html)

[API reference](https://developers.symphony.com/restapi/reference#stream-members)

#### limit

Maximum number of elements to be returned - used for pagination. Maximum allowed is 1000.

Paginating can be achieved with loops as shown in this [example](./examples/paginate-rooms.swadl.yaml). _Be careful
about the amount of data retrieved though! If you have to paginate over all the elements, the content export feature
might be more appropriate._

#### skip

Number of elements to be skipped during return - used for pagination.

### get-room-members

Lists the current members of an existing room.

Key | Type | Required |
------------ | -------| --- |
stream-id | String | Yes |

Output | Type |
----|----|
members | List of [MemberInfo](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/MemberInfo.html)

[API reference](https://developers.symphony.com/restapi/reference#room-members)

### create-user

Creates a new end user.

Key | Type | Required |
------------ | -------| --- | 
[email](#email) | String | Yes |
[firstname](#firstname) | String | Yes |
[lastname](#lastname) | String | Yes |
[display-name](#display-name) | String | Yes |
[username](#username) | String | Yes |
[password](#password) | Map | No |
[recommended-language](#recommended-language) | String | No |
[contact](#contact) | Map | No |
[business](#business) | Map | No |
[roles](#roles) | List | No |
[entitlements](#entitlements) | Map | No |
[status](#status) | String | No |

Output | Type |
----|----|
user | [V2UserDetail](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V2UserDetail.html)

[API reference](https://developers.symphony.com/restapi/reference#create-user-v2)

Examples:

- [create user workflow](./examples/create-user-account.swadl.yaml)
- `${ACTIVITY_ID.outputs.user.userAttributes.firstName}` to access the created user's firstname
- `${ACTIVITY_ID.outputs.user.userSystemInfo.id}` to access the created user's id

#### email

Email address, must be unique.

#### firstname

User's first name.

#### lastname

User's first name.

#### display-name

User's display name.

#### username

Unique identifier for the user.

#### password

User's password. The password object is optional. For example, if your organization utilizes SSO, you may not want to
specify the password.

Key | Type | Required |
------------ | -------| --- | 
hashed-password | String | Yes |
hashed-salt | String | Yes |
hashed-km-password | String | No |
hashed-km-salt | String | No |

_Passwords and salts are base64-encoded strings._

#### recommended-language

The recommended language. As an ISO 639-1 code.

Example: _en-US_

#### contact

Contact information.

Key | Type | Required |
------------ | -------| --- | 
work-phone-number | String | No |
mobile-phone-number| String | No |
two-factor-auth-number | String | No |
sms-number | String | No |

#### business

Business information.

Key | Type | Required |
------------ | -------| --- | 
company-name | String | No |
department | String | No |
division | String | No |
title | String | No |
location | String | No |
[job-function](#job-function) | String | No |
[asset-classes](#asset-classes) | List | No |
[industries](#industries) | List | No |
[functions](#functions) | List | No |
[market-coverages](#market-coverages) | List | No |
[responsibilities](#responsibilities) | List | No |
[instruments](#industries) | List | No |

##### job-function

Allowed value:

- Analyst
- Other
- Business Development Executive
- Corporate Access
- Developer
- Director
- Economist
- Portfolio Manager
- Project Manager
- Research Analyst
- Sales
- Strategist
- Trader

##### asset-classes

Allowed values:

- Equities
- Cash Equities
- Securities Lending
- Fixed Income
- Government Bonds
- Prime Brokerage
- Commodities
- Municipal Bonds
- Currencies
- Corporate Bonds

##### industries

Allowed values:

- Healthcare
- Consumer Non-Cyclicals
- Transportation
- Technology
- Real Estate
- Basic Materials
- Financials
- Energy & Utilities
- Conglomerates
- Consumer Cyclicals
- Services

##### functions

Allowed values:

- Collateral
- Confirmation
- Trade Processing
- Pre-Matching
- Margin
- Matching
- Claims Processing
- Middle Office
- Liquidity Management
- Allocation
- Trade Management
- Regulatory Outreach
- Settlements
- Post Trade Management

##### market-coverages

Allowed values:

- EMEA
- NA
- APAC
- LATAM

##### responsibilities

Allowed values:

- BAU
- Escalation

#### roles

Allowed values for normal users:

- INDIVIDUAL
- ADMINISTRATOR
- SUPER_ADMINISTRATOR
- L1_SUPPORT
- L2_SUPPORT
- COMPLIANCE_OFFICER,
- SUPER_COMPLIANCE_OFFICER

Allowed values for system users:

- INDIVIDUAL
- USER_PROVISIONING
- SCOPE_MANAGEMENT
- CONTENT_MANAGEMENT
- MALWARE_SCAN_MANAGER
- MALWARE_SCAN_STATE_USER
- AUDIT_TRAIL_MANAGEMENT

#### entitlements

A map with boolean values, possible (non-exhaustive) entries are:

- postReadEnabled
- postWriteEnabled
- delegatesEnabled
- isExternalIMEnabled
- canShareFilesExternally
- canCreatePublicRoom
- canUpdateAvatar
- isExternalRoomEnabled
- canCreatePushedSignals
- canUseCompactMode
- mustBeRecorded
- sendFilesEnabled
- canUseInternalAudio
- canProjectInternalScreenShare
- canViewInternalScreenShare
- canCreateMultiLateralRoom
- canJoinMultiLateralRoom
- canUseFirehose
- canUseInternalAudioMobile
- canUseInternalVideoMobile
- canProjectInternalScreenShareMobile
- canViewInternalScreenShareMobile
- canManageSignalSubscription
- canCreateDatahose
- canIntegrateEmail
- canReadDatahose
- canSuppressMessage
- canSwitchToClient20
- canUpdateRoomHistoryProperty

#### status

User status: ENABLED or DISABLED.

### update-user

Updates an existing end user.

Key | Type | Required |
------------ | -------| --- | 
user-id | String | Yes |
[email](#email) | String | No |
[firstname](#firstname) | String | No |
[lastname](#lastname) | String | No |
[display-name](#display-name) | String | No |
[username](#username) | String | No |
[password](#password) | Map | No |
[recommended-language](#recommended-language) | String | No |
[contact](#contact) | Map | No |
[business](#business) | Map | No |
[roles](#roles) | List | No |
[entitlements](#entitlements) | Map | No |
[status](#status) | String | No |

_Keys are similar to [user creation](#create-user) except they are all optionals expect the `user-id`._

Output | Type |
----|----|
user | [V2UserDetail](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V2UserDetail.html)

[API reference](https://developers.symphony.com/restapi/reference#update-user-v2)

### create-system-user

Creates a new service user.

Key | Type | Required |
------------ | -------| --- | 
[email](#email) | String | Yes |
[display-name](#display-name) | String | Yes |
[username](#username) | String | Yes |
[keys](#keys) | Map | No |
[business](#business) | Map | No |
[roles](#roles) | List | No |
[entitlements](#entitlements) | Map | No |
[status](#status) | String | No |

Output | Type |
----|----|
user | [V2UserDetail](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V2UserDetail.html)

[API reference](https://developers.symphony.com/restapi/reference#create-user-v2)

Example: [create system user workflow](./examples/create-service-account.swadl.yaml)

#### keys

For service users, to set up the RSA keys for authentication.

Key | Type | Required |
------------ | -------| --- | 
[current](#key) | String | Yes |
[previous](#key) | String | Yes |

##### current / previous (key)

Key | Type | Required |
------------ | -------| --- | 
[action](#action) | String | Yes |
[key](#key) | String | Yes |
[expiration](#expiration) | String | No |

###### action

A string indicating the action to be performed on the user's RSA.

The following actions can be performed on the user's active RSA key:

- SAVE
- REVOKE

The following actions can be performed onto the user's rotated RSA key:

- REVOKE
- EXTEND

###### key

A string containing the user's RSA public key. The key must be 4096 bits. Only PKCS8 format is allowed.

###### expiration

An [ISO 8601 timestamp](https://en.wikipedia.org/wiki/ISO_8601#Times) containing the RSA key expiration date. This value
is only set for rotated keys.

### update-system-user

Updates an existing service user.

Key | Type | Required |
------------ | -------| --- | 
user-id | String | Yes |
[email](#email) | String | No |
[display-name](#display-name) | String | No |
[username](#username) | String | No |
[keys](#keys) | Map | No |
[business](#business) | Map | No |
[roles](#roles) | List | No |
[entitlements](#entitlements) | Map | No |
[status](#status) | String | No |

Output | Type |
----|----|
user | [V2UserDetail](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V2UserDetail.html)

[API reference](https://developers.symphony.com/restapi/reference#update-user-v2)

### add-user-role

Adds roles to user accounts.

Key | Type | Required |
------------ | -------| --- |
user-ids | List | Yes |
[roles](#roles) | String | Yes |

[API reference](https://developers.symphony.com/restapi/reference#add-role)

### remove-user-role

Remove roles from user accounts.

Key | Type | Required |
------------ | -------| --- |
user-ids | List | Yes |
[roles](#roles) | String | Yes |

[API reference](https://developers.symphony.com/restapi/reference#remove-role)

### get-user

Returns details for a particular user.

Key | Type | Required |
------------ | -------| --- |
user-id | String | Yes |

Output | Type |
----|----|
user | [V2UserDetail](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/V2UserDetail.html)

[API reference](https://developers.symphony.com/restapi/reference#get-user-v2)

Example:

- `${ACTIVITY_ID.outputs.user.userAttributes.firstName}` to access the user's firstname

### get-users

Returns a list of users ID, including user metadata.

Key | Type | Required |
------------ | -------| --- |
[user-ids](#get-users-user-ids) | List | Yes |
[emails](#emails) | List | Yes |
[usernames](#usernames) | List | Yes |
[local](#local) | Boolean | No |
[active](#get-users-active) | Boolean | No |

_One (and only one) of `user-ids`, `emails` or `usernames` must be set._

Output | Type |
----|----|
users | List of [V2UserDetail](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/UserV2.html)

[API reference](https://developers.symphony.com/restapi/reference#users-lookup-v3)

#### <a name="get-users-user-ids"></a>user-ids

List of user ids. Note that for this activity, you can use either the user-ids, the emails or the usernames to make the
call, but only one at a time, you cannot mix and match them.

#### emails

List of email addresses. Note that for this activity, you can use either the user-ids, the emails or the usernames, but
only one at a time, you cannot mix and match them.

#### usernames

List of usernames. If username is specified, local must be set to true. Note that for this activity, you can use either
the user-ids, the emails or the usernames, but only one at a time, you cannot mix and match them.

#### local

If true then a local DB search will be performed and only local pod users will be returned. If absent or false then a
directory search will be performed and users from other pods who are visible to the calling user will also be returned.

#### <a name="get-users-active"></a>active

If true, it searches for active users only. If false, it searches for inactive users only. If not set, it searches for
all users regardless of their status.

### get-connection

Get connection status, i.e. check if the calling user is connected to the specified user.

Key | Type | Required |
------------ | -------| --- |
user-id | String | Yes |

Output | Type |
----|----|
connection | [UserConnection](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/UserConnection.html)

[API reference](https://developers.symphony.com/restapi/reference#get-connection)

### create-connection

Send a connection request to another user.

Key | Type | Required |
------------ | -------| --- |
user-id | String | Yes |

Output | Type |
----|----|
connection | [UserConnection](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/UserConnection.html)

[API reference](https://developers.symphony.com/restapi/reference#create-connection)

### accept-connection

Accept the connection request for a requesting user.

Key | Type | Required |
------------ | -------| --- |
user-id | String | Yes |

Output | Type |
----|----|
connection | [UserConnection](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/UserConnection.html)

[API reference](https://developers.symphony.com/restapi/reference#accept-connection)

### reject-connection

Reject the connection request from a requesting user.

Key | Type | Required |
------------ | -------| --- |
user-id | String | Yes |

Output | Type |
----|----|
connection | [UserConnection](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/UserConnection.html)

[API reference](https://developers.symphony.com/restapi/reference#reject-connection)

### remove-connection

Remove a connection with a user.

Key | Type | Required |
------------ | -------| --- |
user-id | String | Yes |

[API reference](https://developers.symphony.com/restapi/reference#remove-connection)

### get-connections

Get one or multiple connections statuses

Key | Type | Required |
------------ | -------| --- |
[user-ids](#get-connections-user-ids) | List | Yes |
[status](#status) | String | Yes |

Output | Type |
----|----|
connections | List of [UserConnection](https://javadoc.io/doc/org.finos.symphony.bdk/symphony-bdk-core/latest/com/symphony/bdk/gen/api/model/UserConnection.html)

[API reference](https://developers.symphony.com/restapi/reference#list-connections)

#### <a name="get-connections-user-ids"></a>user-ids

List of user ids that this activity results will be restricted to their connections.

#### <a name="get-connections-status"></a>status

Allowed values:

- PENDING_INCOMING
- PENDING_OUTGOING
- ACCEPTED
- REJECTED
- ALL

### execute-request

Executes an HTTP request.

Key | Type | Required |
------------ | -------| --- |
[url](#url) | String | Yes |
[method](#method) | String | No |
[body](#body) | Object/String | No |
[headers](#headers) | String | No |


Output | Type |
----|----|
body | Object/String
status | Integer

If the response body has a `application/json` content type then the `body` output is parsed into a
JSON object (if possible) otherwise it will a string. Please note that this approach comes with limitations
and that the `execute-request` activity should not be used to download large payloads.

Example:
```yaml
activities:
  - execute-request:
      id: myRequest
      headers:
        X-Workflow-Token: A_TOKEN
        Content-Type: application/json # optional as it is the default value
        Accept: application/json
      body:
        myName: Bob
      method: POST
      url: https://api.com/helloWorld # an API that returns {"message": "Hello Bob"}
  - send-message:
      id: sendMsg
      to:
        stream-id: A_STREAM
      content: ${myRequest.outputs.body.message} # Send a message with content "Hello Bob"
      
```

#### url
String that contains the host and the path to be targeted.

#### method
HTTP method to perform. GET is the default one.

Supported methods are:
- DELETE
- GET
- HEAD
- OPTIONS
- PATCH
- POST
- PUT

#### body
HTTP request body.
It can be provided as an object (for `application/json` or `multiplart/form content` type) or as a string.

When `multipart/form` content type is used, only key/value object is supported.

#### headers
HTTP request headers. A map of key/value entries is expected. Simple types
such as numbers, string and booleans as well as lists and maps are supported.

Unless set explicitly the `Content-Type` header will be `application/json` by default.

### execute-script

Executes a [Groovy](https://groovy-lang.org/) script.

Key | Type | Required |
------------ | -------| --- |
[script](#script) | String | Yes |

Example:

```yaml
activities:
  - execute-script:
      id: myScript
      script: |
        // variables can be accessed from a script
        variables.foo = "Hello"
        // BDK beans are accessible
        messageService.send("123", variables.foo)
        // output goes to the workflow bot logs
        println "Hello"
```

#### script

Script to execute (only [Groovy](https://groovy-lang.org/) is supported).

## Utility functions
WDK provides some utility functions that can be used to process data in SWADL.

### Object json(String string)
This method is used to convert a String in JSON format to an Object in order to be processed as a JSON.
It returns a String if the parameter is a simple String.

Example:
```java
${json("{\"result\": {\"code\": 200, \"message\": \"success\"}}").result.code} == 200
${json("This is a regular String")} == "This is a regular String"
```

### String text(String presentationMl)
This method is used to convert a PresentationML String to a text.

Example:
```java
${text(<div data-format="PresentationML" data-version="2.0">started</div>)} == "started"
```

### String escape(String string)
This method will escape text contents using JSON standard escaping, and return results as a String.

Example:
```java
${escape("{"result": {"code": 200, "message": "success"}}")} == "{\"result\": {\"code\": 200, \"message\": \"success\"}}"
```
