# Implementing your own custom activity

While the workflow bot comes with built in activities such as `send-message`, `create-room`,...,
you might want to develop your own. To support this use case we provide a Java API that you can use to implement
a custom activity.
It can then be referenced from a workflow definition.

## Defining a custom activity

The _custom-activity-example_ folder at the root of the repository is a sample project where an activity 
and its executor are defined.

[MyActivity](../custom-activity-example/src/main/java/org/acme/workflow/MyActivity.java) extends the `BaseActivity` 
class that provides common attributes for an activity such as its id or its starting event (`on`).
If you want to support variables being passed as attributes in SWADL (with the `${}` syntax) then the class fields
must be typed as `String` (variables are evaluated at runtime).

[MyActivityExecutor](../custom-activity-example/src/main/java/org/acme/workflow/MyActivityExecutor.java) implements the
`ActivityExecutor` interface and gives access to the activity definition, variables, outputs and BDK services during the
activity execution. `ActivityExecutor` is a generic interface that holds the corresponding activity type.

Those 2 classes are part of a project that could live on its own and only needs a compileOnly(Gradle)/provided(Maven)
dependency on the `workflow-language` module. In the example a third-party dependency is used by the custom activity.
The activity classes as well as other dependencies are packaged as an archive that is meant to be added to the 
workflow's bot classpath.

## Running the workflow bot with custom activities

The workflow bot being a Spring Boot based application, it can be executed as standalone JAR file:

```shell
# Build the custom activity project
./gradlew :custom-activity-example:installDist

cd workflow-bot-app
mkdir lib
# Copy the custom activity JARs
cp -r ../custom-activity-example/build/install/custom-activity-example/lib lib/

# Build and start the workflow bot
../gradlew bootJar
java  -Dloader.path=./lib -jar build/libs/workflow-bot-app.jar
```

Here we add a local folder `lib` to the classpath where the JAR file holding the custom activity and its dependencies 
would be copied.

## Using the custom activity in a workflow

_The JSON schema providing documentation and code completion when writing workflows lets you reference custom 
activities. At runtime the custom activities will be added to the schema to validate the workflow upon submission and 
let you know if the activity is not known before execution._

Let's go back to the `custom-activity-example` project:

```yaml
name: custom-activity-example
activities:
  - my-activity:
      id: first
      on:
        message-received:
          content: /go
      my-parameter: "camel case"
  - send-message:
      id: second
      to:
        stream-id: v7ZTHzpNvFu2ADUrwIq0AH___or-SZVqdA
      content: "${first.outputs.myOutput}"
```

Here the `my-activity` is automatically mapped to the `MyActivity` class (based on the name). As a `BaseActivity` it
comes with the `id` and `on` attributes. `MyActivity` defines a custom attribute (`myParameter`) that is used in its 
executor. The executor simply translates the custom attribute to camel case format and will send the message 
_CamelCase_.
