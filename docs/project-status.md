# Project status

Where the project stands. The outstanding work, each item with the whole of its prose in `detail`, is in the estate
ticket store, which rule **D8** makes the one place an item is written; `npx haembina-tickets list` prints it. This
document never restates an item; it carries what is not item-shaped, beside `conventions.json`.

## Where it is

The plugin runs on the Haembina server's SonarQube Community Build, which scans every estate repository with branch
and pull request analysis through it. The server keeps `auto-configuration.enabled` at `false`, so CI scans that name
no branch land on main.

The repository has adopted the conventions record. `build.yml` builds, tests and releases the plugin and builds both
images, pushing the release image to GHCR on a tag; its `verify` job runs the conventions gate, the Java suite
and the SonarQube scan on ct 102 after the integration test. The scan's open issues are `java:S2143`
findings where github-api's `GHCheckRunBuilder` and SonarQube's `ComponentDto` take only a `java.util.Date`;
`npm run sonar:ceiling` holds their count to the record's `issues.ceiling`.

The public mirror, [Haembina/sonarqube-branch-analysis](https://github.com/Haembina/sonarqube-branch-analysis),
takes one export commit and the same release on each `v` tag, through `build.yml`'s `mirror` job and the
`MIRROR_TOKEN` secret. Nothing is merged there; its pull requests land here, as the README's recipe says.

The plugin is Haembina Branch Analysis: package `com.haembina.branchanalysis`, plugin key `haembinabranchanalysis`,
setting keys under `com.haembina.branchanalysis.`. The class names still carry mc1arke's `CommunityBranch` prefix.
