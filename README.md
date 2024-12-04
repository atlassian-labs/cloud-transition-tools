# README #

Cloud Transition Tools (CTT)
A collection of tools and utilities designed to resolve application integration issues following a cloud transition.

Refer to following documents for more details:
1. [CTT Design Document](https://hello.atlassian.net/wiki/spaces/CT5/pages/4102829269/RFC+Fixing+External+Integration+Post+Migration)
2. [List of Supported Entities](https://hello.atlassian.net/wiki/spaces/CT5/pages/4135101946/Jira+JQL+Fields+affected+by+ID+Mapping)

### What is this repository for? ###

This repository provides basic translation lookup APIs for entities that are affected by a cloud transition. After a
cloud transition there is a possibility that the ID of an entity may change.
For example, if there is an API access to get project details by project ID, and the project ID changes after a cloud
transition, then the API access will fail. This repository provides a way to find the new project ID based on the old
project ID. This also provides integration fixes to solve custom integrations like URL Access, API Access and JQL.
Custom integration fixes can be built on top of these APIs to resolve the integration issues.
For example: JQL Filter translation, API translation, URL translation and 3P App usage translation.
The application is scoped by the Cloud URL. Multiple migration and scopes are supported from different server/dc to a single destination cloud.
If you have a requirement for multiple destination clouds, then you can create a new instance of the application for each destination cloud.

### How do I get set up? ###

#### Project Setup

##### Versions

Use `Java 17` and `Maven 3.9.9` for development. More details in the Dockerfile.

Basic Spring Boot project setup with Maven.

1. Create `application.properties` file in root directory. Refer to
   [application.properties](src/main/resources/application.properties.template) for a reference.
2. Make sure to set `cloudURL` in `application.properties` file along with optional values for data store and loader.
3. If you are using persistent storage, make sure to set the database details as well.

###### Run Maven build and start the Spring Boot application.
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
mvn test -Dtest=ClassName1,ClassName2 # To set a specific test classes
```

### Contribution guidelines ###
Make sure you follow the below guidelines before contributing to the project.

#### Linting and Formatting
We use Detekt for Linting and Ktlint for formatting.
- Auto-format the code using
```
mvn ktlint:format
```
- Ensure no more formatting errors using
```
mvn ktlint:check
```
- Check and fix linting errors using
```
mvn detekt:check -Ddetekt.config=detekt.yml # To detect lint errors
```
We use Sonarlint for local code quality checks. Install Sonarlint plugin in your IDE and run the code quality checks.
Make sure all formatting and linting issues are resolved before raising a PR.

#### SonarQube and Code Coverage
JaCoCo(Java Code Coverage) is used for code coverage. After running all the unit tests, you can view the report at `target/site/jacoco/index.html`.
Please make sure to write unit tests for all the new code you write and maintain the code coverage.
We use SonarQube for code quality checks and code coverage tests. You would ideally need to setup SonarQube locally to run the checks.
However, internally in Atlassian network, to run SonarQube, run the following command:
```
atlas plugin install -n sonar
atlas sonar scan --defaultBranch develop
```
More details at: https://developer.atlassian.com/platform/tool/sonarqube/getting-started/
Make sure the code quality checks and code coverage checks pass before raising a PR.

Follow standard Kotlin coding conventions.

#### Checklist before raising PR
1. `mvn ktlint:check` and ensure no errors
2. `mvn detekt:check -Ddetekt.config=detekt.yml` and ensure no errors
3. Add unit tests for the code that you have added
4. `mvn test` and ensure all tests pass
5. ensure code coverage and quality with sonarqube

### Who do I talk to? ###
Vineeth Kumar T(@vkumart)
Nitin Suri(@nsuri2)
