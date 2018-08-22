# OOTBee Webhook Hub
This project aims to build a small, Java-based server that can be used to process webhook events from various services in order to alter / enhance the webhook payload and send these events on to other services. The originally intended use case was to enable the Order of the Bee to forward webhook events to its Discord server, even events from sources which do not natively generate a event payload that can be properly processed by Discord out-of-the-box. Additionally, initial evaluations of existing webhook integrations, e.g. concerning GitHub, have shown that reformatting of messages / payloads would be necessary to make Discord messages accessible to a larger group of users.

## Use of GraalVM / Graal.js
Besides the core goal of This project, it is also meant to be an exploration of the capabilities of the [GraalVM](https://www.graalvm.org/) Java Virtual Machine, including its polyglot capabilities - particularly the [Graal.js](https://github.com/graalvm/graaljs) runtime for JavaScript meant as a alternative/replacement to the now deprecated Nashorn runtime - and support for generating stand-alone binaries from Java applications using [Substrate VM](https://github.com/oracle/graal/tree/master/substratevm). As such, the application built by this project can only be executed properly in a GraalVM runtime. It may be technically possible to run it in a regular Oracle / OpenJDK runtime with the addition of the Graal.js and GraalVM polyglot JARs, but this is neither the intended scenario nor supported.

## Building
This project is built using Maven. The build will produce a regular JAR, a fat/shaded JAR as a stand-alone executable JAR, and a basic Docker image to run the application inside a GraalVM Community Edition runtime. As a result of building a Docker image, the build process is dependent on the existence of a Docker engine on the system running the build.

### Non-standard dependency: svm.jar
In order to support the secondary use case of generating a native binary from the Java application using Substrate VM, the project is using annotations / classes from a special tooling JAR provided by GraalVM to provide substitutions and hints for the compilation process to deal with otherwise unsupported features / code patterns. Unfortunately this tooling JAR is currently not publicly accessible in any Maven artifact repository and needs to be installed into the local Maven repository prior to building the project. As illustrated in [this Medium article about generating a native image](https://medium.com/graalvm/instant-netty-startup-using-graalvm-native-image-generation-ed6f14ff7692), this can be done by taking the JAR from a GraalVM installation and running the following command (assuming JAVA_HOME points to your GraalVM installation and the GraalVM version is 1.0.0-rc5):

```
mvn install:install-file -Dfile=${JAVA_HOME}/jre/lib/svm/builder/svm.jar -DgroupId=com.oracle.substratevm -DartifactId=svm -Dversion=1.0.0-rc5 -Dpackaging=jar
```

### Regular Build
The build can be run by simply executing the standard Maven command

```
mvn install
```

or

```
mvn clean install
```

Currently, due to unresolved issues with the configuration of the [Spotify dockerfile-maven-plugin](https://github.com/spotify/dockerfile-maven) it is not possible to execute the deploy goal of the project build and publish both the produced Maven artifacts of the build as well as push the Docker image to Docker Hub.

## Using the Docker image
The Docker image built by this project includes a basic configuration file for the hook processing supported by the application, and using this image is as simple as using either the regular Docker run command or Docker Compose while specifying a few environment variables to fill in sensitive parts of the configuration. A simple Docker Compose file could look like this:

```json
version: '2.0'

services:
   webhook-hub:
      image: OrderOfTheBee/webhook-hub:latest
      ports:
        - 8080:8080
      environment:
        - WEBHOOK_OOTBEE_TESTFIELD_URL=<Webhook-URL to post to a specific Discord channel (here webhook-testfield on OrderOfTheBee Discord)>
        - WEBHOOK_OOTBEE_TESTFIELD_GITHUB_SECRET=<sharedSecretSetupWithGitHub>
        - WEBHOOK_OOTBEE_SUPPORT_TOOLS_URL=<Webhook-URL to post to a specific Discord channel (here support-tools on OrderOfTheBee Discord)>
        - WEBHOOK_OOTBEE_SUPPORT_TOOLS_GITHUB_SECRET=<sharedSecretSetupWithGitHub>
      hostname: webhook-hub
      restart: unless-stopped
```

The supported environment variables are defined by placeholders put in the [application configuration YAML file](https://github.com/AFaust/webhook-hub/blob/master/src/main/ImageBuild/hooks.yml) and use the prefix "WEBHOOK_" to differentiate them from any other environment variables and trigger processing by the [application initialisation script inside the container](https://github.com/AFaust/webhook-hub/blob/master/src/main/ImageBuild/initHub.sh).

The Docker image simply exposes a plain HTTP port for incoming webhook requests from other services. It does not support HTTPS and is only meant to be run behind a secure web server / proxy dealing with cross-cutting web concerns.

## Application Configuration

TBD