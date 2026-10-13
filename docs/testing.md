<div align="center">

<img src="../icons/icon.png" alt="" width="48">

# Testing

Each suite, what it proves and cannot reach, and what CI runs.

[README](../README.md) · [Architecture](architecture.md) ·
[Contributing](contributing.md) · [undefined](guide.md) ·
[Security](security.md) · **Testing**

</div>

---

## Suites

| | Suite | Runs | Proves | Cannot reach |
| --- | --- | --- | --- | --- |
| <img src="../icons/readme/code.svg" width="18" alt=""> | Java suite | `./gradlew build` | The agent, extensions and ALM clients under JUnit 5, failing under the JaCoCo floor in build.gradle | A real SonarQube: the agent names the classes it rewrites by string |
| <img src="../icons/readme/device-desktop.svg" width="18" alt=""> | Branch page suite | `npm run test:addons` | Each branch page component under Jest and Testing Library, failing under jest.config.cjs's floor | Types, which npm run typecheck:addons checks, and the webapp around the pages |
| <img src="../icons/readme/checklist.svg" width="18" alt=""> | Integration test | `bash scripts/integration-test.sh <jar> <webapp dir>` | SonarQube in Docker loads the jar and webapp and runs a main, branch and pull request analysis | Decoration on a real ALM |
| <img src="../icons/readme/accessible.svg" width="18" alt=""> | End-to-end suite | `bash scripts/integration-test.sh <jar> <webapp dir> npm run test:e2e` | The branch pages and dialogs in Chromium, by mouse and by keyboard, audited for accessibility on every page and dialog | Browsers other than Chromium, and screen readers |
| <img src="../icons/readme/box.svg" width="18" alt=""> | Docker scripts | `./docker/test-branch-analysis-entrypoint.sh` | The entrypoint's plugin upgrade handling; test-push-release-image.sh holds the release push to a recording docker | A real registry |
| <img src="../icons/readme/bug.svg" width="18" alt=""> | Mutation runs | `./gradlew pitest` | Whether the Java assertions notice a changed line; npm run mutation does the same for the branch pages with Stryker | A threshold: equivalent mutants make one noise |

## What CI runs

```mermaid
flowchart LR
  s0["Java build and suite"]
  s1["Webapp build and branch page suite"]
  s2["Dockerfile lint and image builds"]
  s3["Integration and end-to-end suites"]
  s4["Linters, coverage floors, audit and scan"]
  s5["Release on a tag"]
  s1 --> s3
  s3 --> s4
  s4 --> s5
```

## Run it

```bash
npm run gate
```

Runs what CI runs, in its order. One suite: its command from the table.
