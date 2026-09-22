# FDC3 Java Workbench

JavaFX desktop port of the [FDC3 Workbench](https://github.com/finos/FDC3/tree/master/toolbox/fdc3-workbench)
for testing Desktop Agent interoperability. Targets **FDC3 3.0** only.

## Features

- Connect to a Desktop Agent via WSCP (protocol URL, env vars, or Connect dialog)
- Context template library (create / edit / broadcast / reset defaults)
- Raise intent / raiseIntentForContext with optional target and `getResult()` display
- Intent listeners with optional context / channel / private-channel results
- User channels: join, leave, broadcast, context listeners
- App channels: getOrCreate, broadcast, listeners
- Right-hand listener panels and system log

## Prerequisites

- Java 17+
- A running FDC3 3.0 Desktop Agent (e.g. FDC3-Sail)

## Building

From the `fdc3-java-api` root:

```bash
mvn clean package -pl fdc3-java-workbench -am
```

Executable shaded JAR:

`fdc3-java-workbench/target/fdc3-java-workbench-1.0.0-SNAPSHOT.jar`

## Running

### Dev (JavaFX plugin)

```bash
mvn -pl fdc3-java-workbench javafx:run
```

### Shaded JAR

JavaFX native libraries must be available on the module/classpath for your OS.
Prefer `javafx:run` for local development, or the macOS `.app` below for Sail launches.

```bash
java -DFDC3_WEBSOCKET_URL=ws://localhost:8090/fdc3/ws \
     -DFDC3_CONNECTION_SECRET=your-pairing-secret \
     -jar fdc3-java-workbench/target/fdc3-java-workbench-1.0.0-SNAPSHOT.jar
```

### Connection sources (priority order)

1. Protocol handler URL — `fdc3-java-workbench://launch?webSocketUrl=...&sharedSecret=...`
2. `FDC3_WEBSOCKET_URL` / `FDC3_CONNECTION_SECRET` (env or system properties)
3. **Connect** button in the UI

### Using with FDC3-Sail

1. Register the `fdc3-java-workbench://` protocol (build the macOS app below, or register manually)
2. Point Sail’s native app record at this workbench
3. Use **Open On Desktop** — Sail mints a pairing secret and launches via the protocol URL

## macOS .app

```bash
mvn package -pl fdc3-java-workbench -am -Pmacos-app
fdc3-java-workbench/scripts/macos/install-app.sh
```

## Layout

Mirrors the web workbench:

- Left tabs: Contexts | Intents | User Channels | App Channels
- Right tabs: Listeners | System Log
- Header: connection status + `getInfo()` summary

## Notes

- No FDC3 1.2 / 2.x compatibility mode
- JSON editing is a plain text editor (no remote schema validation)
- Context templates persist in Java `Preferences` under this package
