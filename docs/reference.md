# SWADL reference

## name

## activities

### activity common attributes

Attribute | Type | Required | Description
------------ | -------| --- |------ 
id | string | true | The content of the message to send, simple text or MessageML
name | string | true | The content of the message to send, simple text or MessageML
description | string | true | The content of the message to send, simple text or MessageML
one | object | true | The content of the message to send, simple text or MessageML

TODO object = link below

#### on

##### timeout

##### one-of

##### form-replied

TODO document event type

...

#### if

#### else

### built-in activities

#### send-message

Attribute | Type | Required | Description
------------ | -------| --- |------ 
content | string | true | The content of the message to send, simple text or MessageML

Output       | Type | Description
------------ | -------- | -----
message | V4Message | The send message

Examples:

```yaml
activities:
  - send-message:
      id: act1
      content: Hello # 
```
