<div align="center">

<img src="/icons/icon.png" alt="" width="48">

# Security

Reporting a vulnerability, which releases get fixes, and what protects a server.

[README](/README.md) · [Architecture](/docs/architecture.md) ·
[Contributing](/docs/contributing.md) · [Guide](/docs/guide.md) · **Security** ·
[Testing](/docs/testing.md)

</div>

---

## Reporting a vulnerability

- Report it privately through GitHub's
  [private vulnerability reporting](https://github.com/Haembina/sonarqube-branch-analysis/security/advisories/new),
  never in a public issue.
- Say which release you ran, what an attacker can reach, and how to reproduce
  it.
- A public issue reporting a hole is closed with a request for a private
  report, since it discloses the hole before a release fixes it.
- A confirmed vulnerability is fixed in a release, and its advisory is
  published with that release.

## Supported versions

Only the newest release gets fixes.

## Protections

| Protection | What it covers |
| --- | --- |
| Runs inside your server | The plugin sends nothing anywhere but the ALM a project is bound to |
| ALM credentials | Read from SonarQube's ALM settings; Bitbucket requests reach the debug log with their `Authorization` header redacted |
| Shaded dependencies | The plugin's own dependencies are relocated inside the jar, so they cannot clash with SonarQube's |
| Pinned images | Both Dockerfiles and `docker-compose.yml` pin their base images by digest |
| Pinned actions | Every third-party action in the workflows is pinned to a commit sha, and Dependabot moves them |
| Dependency audit | Every pull request audits the npm lockfile; a new advisory fails it |
| One supported build | Each release supports only the Community Build `sonarqubeVersion` in `build.gradle` names |
