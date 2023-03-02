# Deployment with Docker
A Docker image is provided in order to run WDK from scratch with a single command without requiring tooling or a java project.

Symphony WDK released docker images are published as a [Github package](https://github.com/orgs/finos/packages?repo_name=symphony-wdk).

### Pull Docker image
Pull the docker image. You can replace the tag "latest" with the [release tag](https://github.com/finos/symphony-wdk/tags) you want.
```shell
 $ docker pull ghcr.io/finos/symphony-wdk:latest
```

### Mount a shared folder
Mount a folder on your machine to be shared with the Docker container. The shared folder should contain:
- WDK configuration `application.yaml` file.
- Service account authentication RSA private key.


_note: You can define different `application.yml` files, one per Spring profile._

```shell
$ mkdir /my_folder
$ ls my_folder/
application-prod.yaml	application.yaml	private_key_default.pem		private_key_prod.pem
```
_note: `application.yml` configuration file should refer to the RSA private key inside container. This also applies to the persistence database `jdbc-url` if file based persistence is enabled._

Example of `application.yml` with folder being mounted in the container at `./symphony/` :
```yaml
wdk:
  workflows:
    path: ./symphony/workflows/
bdk:
  host: HOST_URL
  bot:
    username: BOT_USERNAME
    privateKey:
      path: ./symphony/private_key_default.pem

spring:
  datasource:
    wdk:
      jdbc-url: jdbc:h2:file:./symphony/data/wdk_workflow;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE
    camunda:
      jdbc-url: jdbc:h2:file:./symphony/data/process_engine;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE
```

### Symphony WDK disk based deployment mode
If you want to run Symphony WDK with disk based deployment mode, you need to initially create an empty `workflows/` folder.

_nb: wdk.workflows.path property value can be replaced with any other name._
```shell
$ cd /my_folder && mkdir workflows
$ ls my_folder/
workflows/	application-prod.yaml	application.yaml	private_key_default.pem		private_key_prod.pem
```

### Run Docker image
Run Symphony WDK docker image with the shared folder as a volume. You can also specify the Spring profile as an environment variable. 

For example, if `PROFILE=prod` is used, then `application-prod.yml` will be used if found in the shared folder. Otherwise, if `./symphony/application.yml` exists, it will be used instead.

`PROFILE` environment variable is optional. 
```shell
docker run -v $(pwd)/my_folder:/symphony -e VOLUME=symphony -e PROFILE=prod -p 8080:8080 ghcr.io/finos/symphony-wdk:latest 
```
---
If Symphony WDK is running with disk based deployment mode, any `*.swadl.yaml` workflow file added to `./symphony/workflows` will be deployed.
