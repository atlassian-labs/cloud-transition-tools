# README #

Cloud Transition Tools (CTT)
A collection of tools and utilities designed to resolve application integration issues following a cloud transition.

Refer to following documents for more details:
1. [CTT Deign Document](https://hello.atlassian.net/wiki/spaces/CT5/pages/4102829269/RFC+Fixing+External+Integration+Post+Migration)
2. [List of Supported Entities](https://hello.atlassian.net/wiki/spaces/CT5/pages/4135101946/Jira+JQL+Fields+affected+by+ID+Mapping)

### What is this repository for? ###

This repository provides basic translation lookup APIs for entities that are affected by a cloud transition. After a
cloud transition there is a possibility that the ID of an entity may change.
For example, if there is an API access to get project details by project ID, and the project ID changes after a cloud
transition, then the API access will fail. This repository provides a way to find the new project ID based on the old
project ID. This also provides integration fixes to solve custom integrations like URL Access, API Access and JQL.
Custom integration fixes can be built on top of these APIs to resolve the integration issues.
For example: JQL Filter translation, API translation, URL translation and 3P App usage translation.

### How do I get set up? ###

#### Project Setup

##### Versions

Use `Java 17` and `Maven 3.9.9` for development. More details in the Dockerfile.

Basic Spring Boot project setup with Maven.

1. Create `applicatoin.properties` file in `src/main/resources` directory. Refer to
   [application.properties](src/main/resources/application.properties.template) for a reference.

2. Update `application.properties` with migration scope, data loader and database configuration.

3. Run Maven build and start the Spring Boot application.

```
mvn clean install -DskipTests
mvn spring-boot:run
```

#### Docker Setup

Dockerfile is provided to build a docker image for the project.

```
docker build -t ctt .
docker run -p 8080:8080 ctt
```

#### API Endpoints

You can view the api-docs at `ctt/v3/api-docs`
Access Swagger UI at `ctt/swagger-ui.html`

#### Configuration

Refer to [application.properties](src/main/resources/application.properties.template) for configuration details.

#### How to run tests

```
mvn test
```

### Contribution guidelines ###
#### Linting and Formatting
We use Ktlint for linting and formatting. To format the code, run the following command:
```
mvn ktlint:format
```
Follow standard Kotlin coding conventions.

### Who do I talk to? ###

* Please contact Vineeth T(@vkumart) for any issues and suggestions.