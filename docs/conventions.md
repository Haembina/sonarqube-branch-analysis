# Local conventions

The estate's conventions apply here as written in `haembina-conventions/CONVENTIONS.md`. This file holds what is local.

## Kinds

No kind set is declared, so S1, S2 and S8 are off. Java names a file after the public class it holds, and a test after
its subject with `Test` appended (`AnalysisDetails.java`, `AnalysisDetailsTest.java`), which is how Gradle's JUnit
platform finds the suites. The TSX under `sonarqube-webapp-addons` follows SonarSource's webapp: a PascalCase file per
component, and its suite beside it as `<Component>.test.tsx`. The Playwright specs in `e2e` are `<page>.test.ts`, so the
conventions check reads their `capability <name>: <what>` titles; the capabilities in `conventions.json` name them.
The WCAG specs are `wcag-<concern>.test.ts`, each case titled by criterion number under the capability it drives, and
`accessibility.criteria` names them.

## Languages

| Language | Linted by | Measured by |
| --- | --- | --- |
| Java | Error Prone and javac, every finding an error | JUnit 5 and JaCoCo, failing under the declared floor |
| TypeScript | ESLint with typescript-eslint and sonarjs, and the `sonarqube-webapp` workspace's TypeScript build | Jest in that workspace, failing under the declared floor; Playwright over the built webapp |
| JavaScript | ESLint with sonarjs | Nothing: both files are configuration |
| JSON | ESLint with @eslint/json | Nothing |
| Gradle | npm-groovy-lint | Nothing |
| Shell | shellcheck | Nothing: the Docker upgrade test and the integration test run them |
| Workflows | actionlint | Nothing |
| Markdown | markdownlint-cli2 | Nothing |

`conventions.json` carries the reason for each entry.

## What the SonarQube scan reads

`sonar-project.properties` names the product's code and its tests. Tool configs at the root that no suite measures,
such as `jest.config.cjs` and `playwright.config.ts`, stay out of `sonar.sources`, because the quality gate asks
every new line to be covered. The `sonarqube-webapp` submodule stays out too: `npm test` removes it once the addons'
suite has run, and its dependencies are SonarSource's.

## Headers

Every Java and TSX file opens with its LGPL-3.0 licence header. Keep it when changing a file.

## Where this bites

- An Error Prone or javac warning fails the Java build: `-Werror` is on every compile task. Run `npm run lint:java`
  to see one before pushing.
- A number other than -1, 0 or 1 written inline in the addons or an `.mjs` script fails `npm run lint:es` (S6). Name
  it as a constant.
- A suite dropping under the JaCoCo floor fails `./gradlew build`, which `build.yml` runs. The report is
  `build/reports/jacoco/test/html/index.html`.
