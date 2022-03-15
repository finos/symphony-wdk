# Getting started

The tool of choice to get started writing and running workflows is
the [Symphony generator](https://github.com/finos/generator-symphony). Essentially we are going to
create a Symphony bot that is capable of running workflows.

It can be installed with:

```
npm install -g yo @finos/generator-symphony
```

Then in an empty folder, run the `yo @finos/symphony` command.

You will be guided as shown below to configure your bot. Make sure to select _Workflow Application_.

```
~/dist $ yo @finos/symphony
 __   __     ___                 _
 \ \ / /__  / __|_  _ _ __  _ __| |_  ___ _ _ _  _
  \ V / _ \ \__ \ || | '  \| '_ \ ' \/ _ \ ' \ || |
   |_|\___/ |___/\_, |_|_|_| .__/_||_\___/_||_\_, |
                 |__/      |_|                |__/ 
        https://developers.symphony.com

Welcome to Symphony Generator v2.1.0
Application files will be generated in folder: dist
______________________________________________________________________________________________________
? Enter your pod host acme.symphony.com
? Enter your bot username my-bot
? Select your type of application 
  Bot Application 
  Extension App Application 
❯ Workflow Application (WDK) 
```

The generator creates for you the configuration file of the bot, along with its credentials. As the workflow bot is made
using Java you will need a JRE to run it.

Running the bot is as simple as:

```
java -jar workflow-bot-app.jar
```

We also provide a sample Dockerfile as an alternative, in that case the image can be built and executed with:

```
docker build -t workflow-bot .
docker run -ti workflow-bot
```

The generated workflow bot comes with a sample workflow:

```yaml
id: ping-pong-workflow
activities:
  - send-message:
      id: pingPong
      on:
        message-received:
          content: /ping
      content: "Pong"
```

Because the workflow file is placed in the `workflows/` folder it is automatically deployed (and will be re-deployed on
changes).

Let's go through the workflow line by line:

1. First we set a name for it, mostly used for documentation and tracing in the logs
2. Then we define the activities the workflow is going to execute
   1. Here we only have one activity, to send a message
      1. An activity requires an id, to be able to reference it if needed (to access its outputs for instance)
      2. When is the activity started? Here since it is the first activity of the workflow we actually define when the
         workflow is started
         1. We start it when the service account used for the workflow bot receives a message
      3. The received message must be `/ping` to start the workflow
      4. Finally, we set the content of the message to send. Here because we did not set any, the message will be sent in the
         same room as the incoming message.

So to run this workflow, simply send an IM to your bot, with `/ping` as the content!
