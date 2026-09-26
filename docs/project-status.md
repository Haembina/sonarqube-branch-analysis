# Project status

Where the project stands. The outstanding work, each item with the whole of its prose in `detail`, is in the estate
ticket store, which rule **D8** makes the one place an item is written; `npx haembina-tickets list` prints it. This
document never restates an item; it carries what is not item-shaped, beside `conventions.json`.

## Where it is

The plugin runs on the Haembina server's SonarQube Community Build, which scans every estate repository with branch
and pull request analysis through it. The server keeps `auto-configuration.enabled` at `false`, so CI scans that name
no branch land on main.

The repository has adopted the conventions record. `build.yml` builds, tests and releases the plugin; `ci.yml` runs the
conventions gate beside it. No analysis server scans this repository yet, so `ci.yml` runs no SonarQube step.

The code from mc1arke's repository still carries its package names, its plugin key and its setting keys, and the
rebrand to Haembina's names is an item in the store.
