# Cloud Transition Tools (CTT)
[![Atlassian license](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)](LICENSE) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](CONTRIBUTING.md)

A collection of APIs and utilities designed to resolve application integration issues following a cloud transition.
This primarily addresses the entity id mismatch that happens after a cloud migration.
After a cloud transition there is a possibility that the ID  of an entity may change.
For example, if there is an API access to get project details by project ID and after cloud migration if the project id
changes then the API access will lead to incorrect results. CTT provides utilities to map the entity ids.

This also provides integration fixes to solve custom integrations like URL Access, API Access
and JQL. Custom integration fixes can be built on top of these APIs to resolve the integration issues.
For example: JQL Filter translation, API translation, URL translation and 3P App usage translation.
The application is scoped by the Cloud URL. Multiple migration and scopes are supported from different server/dc to a single destination cloud.
If you have a requirement for multiple destination clouds, then you can create a new instance of the application for each destination cloud.


## Usage
CTT is built as a Spring Boot application. You can run the application as a standalone application or deploy it as a docker container.
Refer to the [loom video](https://www.loom.com/share/e176c938e0124446a0251694ae33c66f?sid=25b4c749-62b8-49d9-9303-d2c9337b6644) for a quick demo on how to use CTT.

## Installation
Use `Java 17` and `Maven 3.9.9` or later for development. More details in the Dockerfile.

1. Create `application.properties` file in root directory. Refer to
   [application.properties.template](src/main/resources/application.properties.template) for a reference.
2. Make sure to set `cloudURL` in  application.properties file along with optional values for data store and loader.
3. If you are using persistent storage, make sure to add the database details.

#### Build Process
Run following maven commands to build and run the application.
```
mvn clean install -DskipTests
mvn spring-boot:run
```

#### Docker Setup
Run following commands to build and run the docker image.
```
docker build -t ctt .
docker run -p 8080:8080 ctt
```

#### API Endpoints
Once the application has been built, you can access the API endpoints relative to the base URL.

* Openapi API Docs `ctt/v3/api-docs`
* Swagger UI `ctt/swagger-ui.html`

## Documentation
[Cloud-Transition-Tools KB](https://confluence.atlassian.com/migrationkb/external-id-mapping-cloud-transition-tools-ctt-1456180780.html )

## Tests
All the unit tests can be run using below commands
* To run all tests `mvn test`
* To run specific tests `mvn test -Dtest=ClassName1,ClassName2`


## Contributions
Contributions to Cloud Transition Tools are welcome, in fact we are looking forward to it as multiple app/ vendor integrations can only
be solved by larger community/ vendor participation. It would best fit to the vendor to provide the integration fixes for their products
by using the APIs provided by CTT.

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

Copyright (c) 2024 Atlassian US., Inc.
Apache 2.0 licensed, see [LICENSE](LICENSE) file.

[![With ❤️ from Atlassian](https://raw.githubusercontent.com/atlassian-internal/oss-assets/master/banner-cheers-light.png)](https://www.atlassian.com)
