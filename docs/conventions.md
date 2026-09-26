# Local conventions

The estate's conventions apply here as written in `haembina-conventions/CONVENTIONS.md`. This file holds what is local.

## Kinds

No kind set is declared, so S1, S2 and S8 are off. Java names a file after the public class it holds, and a test after
its subject with `Test` appended (`AnalysisDetails.java`, `AnalysisDetailsTest.java`), which is how Gradle's JUnit
platform finds the suites. The TSX under `sonarqube-webapp-addons` follows SonarSource's webapp: a PascalCase file per
component.

## Languages

| Language | Linted by | Measured by |
| --- | --- | --- |
| Java | javac, every lint category as an error | JUnit 5 and JaCoCo, failing under the declared floor |
| TypeScript | The `sonarqube-webapp` workspace's TypeScript build | The integration test loading the built webapp |
| Shell | shellcheck | Nothing: the Docker upgrade test and the integration test run them |
| Workflows | actionlint | Nothing |
| Markdown | markdownlint-cli2 | Nothing |

`conventions.json` carries the reason for each entry.

## Headers

Every Java and TSX file opens with its LGPL-3.0 licence header. Keep it when changing a file.

## Where this bites

- A lint warning fails the Java build: `-Werror` is on every compile task. Run `./gradlew compileJava compileTestJava`
  to see one before pushing.
- A suite dropping under the JaCoCo floor fails `./gradlew build`, which `build.yml` runs. The report is
  `build/reports/jacoco/test/html/index.html`.
