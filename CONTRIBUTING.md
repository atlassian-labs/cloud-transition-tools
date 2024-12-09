# Contributing to Cloud Transition Tools(CTT)

Thank you for considering a contribution to CTT! Pull requests, issues and comments are welcome. For pull requests, please:

* Add tests for new features and bug fixes
* Follow the existing style
* Separate unrelated changes into multiple pull requests

See the existing issues for things to start contributing.

For bigger changes, please make sure you start a discussion first by creating an issue and explaining the intended change.

Atlassian requires contributors to sign a Contributor License Agreement, known as a CLA. This serves as a record stating that the contributor is entitled to contribute the code/documentation/translation to the project and is willing to have it used in distributions and derivative works (or is willing to transfer ownership).

Prior to accepting your contributions we ask that you please follow the appropriate link below to digitally sign the CLA. The Corporate CLA is for those who are contributing as a member of an organization and the individual CLA is for those contributing as an individual.

* [CLA for corporate contributors](https://opensource.atlassian.com/corporate)
* [CLA for individuals](https://opensource.atlassian.com/individual)

## Code quality and standards

### Linting and Formatting
We use Detekt for Linting and Ktlint for formatting.
* Format the code by running `mvn ktlint:format`
* Fix linting errors by running `mvn detekt:check -Ddetekt.config=detekt.yml`

### Code quality
* We use Sonarqube for code quality checks. You would need to set up SonarQube locally to run the checks. [Refer to official documentation](https://docs.sonarsource.com/sonarqube-server/10.5/setup-and-upgrade/install-the-server/introduction/)
* We use Sonarlint for coding standards checks. Make sure to install the Sonarlint plugin in your IDE. For IntelliJ IDEA, you can install the plugin from the marketplace.
* JaCoCo(Java Code Coverage) is used for ensuring code coverage. After running all the unit tests, you can view the report at `target/site/jacoco/index.html`.

### Checklist before raising PR
1. Follow standard Kotlin coding conventions as per [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
2. Add Unit tests for new features and bug fixes and ensure code coverage.
3. `mvn ktlint:check` and ensure no errors
4. `mvn detekt:check -Ddetekt.config=detekt.yml` and ensure no errors
5. `mvn test` and ensure all tests pass
6. ensure code coverage and quality with sonarqube

### Contacts

