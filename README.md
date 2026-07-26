# ZamoraMC Status

ZamoraMC Status is a two-part Minecraft plugin that exposes the availability of servers registered in a Velocity proxy to PlaceholderAPI on Paper servers.

The Velocity module monitors every server declared in `velocity.toml`. The Paper module is only required on servers where the placeholder is used; it does not need to be installed on every monitored backend.

## Features

- Native Velocity server checks with a configurable interval and timeout.
- Automatic discovery from `velocity.toml`; no manual server list is required.
- Case-insensitive status lookup by Velocity's internal server name.
- Plugin messaging over `zamoramc:status` between Velocity and Paper.
- Safe `false` result for offline, unknown, inaccessible, or not-yet-checked servers.
- Optional PlaceholderAPI integration on Paper.
- Reload command available from the console and in-game.
- Java 21-compatible plugin bytecode.

## Installation

1. Install `ZamoraMC-Status-Velocity.jar` in the proxy's `plugins/` directory.
2. Install `ZamoraMC-Status-Paper.jar` on each Paper server where the placeholder will be displayed.
3. Install PlaceholderAPI on those Paper servers if you want to use the placeholder.
4. Restart the proxy and the affected Paper servers.

Velocity monitors all servers registered in `velocity.toml`, including servers that do not have the Paper module installed.

## PlaceholderAPI

Use the following format:

```text
%zamoramcstatus_<server-name>%
```

Examples:

```text
%zamoramcstatus_survival121%
%zamoramcstatus_lobby%
%zamoramcstatus_eventos%
```

The placeholder returns only `true` or `false`:

- `true`: the monitored Velocity server responded successfully to its latest check.
- `false`: the server is offline, inaccessible, unknown, or has not completed its first successful check.

The Paper module requests updates through the connected player connection. Requests are therefore paused while that Paper server has no connected players, but the Velocity module continues monitoring all registered servers independently.

Example:

```text
/papi parse me %zamoramcstatus_survival121%
```

## Configuration

Velocity creates `plugins/zamoramc-status/config.yml`:

```yaml
check-interval-seconds: 5
ping-timeout-milliseconds: 2000
```

Paper creates `plugins/ZamoraMC-Status/config.yml`:

```yaml
request-interval-seconds: 5
unknown-status: false
```

The server names are always read from Velocity's registered server list. No server names are configured manually in this plugin.

## Reload

```text
/zamoramc-status reload
```

Permission:

```text
zamoramcstatus.reload
```

The permission is granted to operators by default.

- On Velocity, reloads the proxy configuration and immediately checks all registered servers.
- On Paper, reloads the local configuration, clears the local cache, and requests a fresh snapshot from Velocity.

## Building

The project uses Gradle and has three modules:

- `common`: shared plugin-message protocol.
- `velocity`: the Velocity proxy module.
- `paper`: the Paper module and PlaceholderAPI expansion.

Run:

```text
gradle clean test build
```

The build targets Java 21 bytecode. A JDK 25 is required to resolve and compile against the selected Paper `26.2.build.84-stable` API. Paper 1.21 servers can run the resulting bytecode on Java 21; Paper 26.2 requires the Java version specified by that Paper release.

Generated JARs are written to:

```text
velocity/build/libs/ZamoraMC-Status-Velocity-1.0.0.jar
paper/build/libs/ZamoraMC-Status-Paper-1.0.0.jar
```

Release JARs are intentionally not committed to this repository. They should be attached to GitHub Releases.

## License

ZamoraMC Status is licensed under the MIT License. See [LICENSE.md](LICENSE.md).

Copyright © 2026 Vicevil.
