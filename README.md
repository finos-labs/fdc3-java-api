[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://community.finos.org/docs/governance/Software-Projects/stages/incubating)

# FDC3 Java API

A Java implementation of the [FDC3 Standard](https://fdc3.finos.org/) enabling Java desktop applications to interoperate with other FDC3-enabled applications via the [Desktop Agent Communication Protocol (DACP)](https://fdc3.finos.org/docs/api/specs/desktopAgentCommunicationProtocol).

## Overview

This project provides:

- **FDC3 Standard API interfaces** — Java equivalents of the FDC3 TypeScript API
- **Desktop Agent Proxy** — Client-side implementation that communicates with a Desktop Agent over WebSocket
- **GetAgent factory** — Simple entry point for connecting to a Desktop Agent
- **Cucumber testing framework** — Shared step definitions for conformance testing against the official FDC3 feature files

## Modules

| Module             | Description                                                                     |
| ------------------ | ------------------------------------------------------------------------------- |
| `fdc3-standard`    | Core FDC3 API interfaces (`DesktopAgent`, `Channel`, `Context`, `Intent`, etc.) |
| `fdc3-schema`      | Generated schema types and JSON conversion utilities                            |
| `fdc3-context`     | Context type conversion utilities                                               |
| `fdc3-agent-proxy` | `DesktopAgentProxy` implementation using DACP messaging                         |
| `fdc3-get-agent`   | `GetAgent` factory for obtaining a `DesktopAgent` connection via WebSocket      |
| `fdc3-testing`     | Cucumber step definitions and test utilities for FDC3 conformance testing       |

## Requirements

- Java 11 or later
- Maven 3.6+
- A running FDC3 Desktop Agent that supports the [Desktop Agent Communication Protocol](https://fdc3.finos.org/docs/api/specs/desktopAgentCommunicationProtocol) (e.g., [FDC3 Sail](https://github.com/finos/FDC3-Sail))

## Installation

### Building from Source

```sh
mvn clean install
```

### Maven Dependency

Once published, add to your `pom.xml`:

```xml
<dependency>
    <groupId>org.finos.fdc3</groupId>
    <artifactId>fdc3-get-agent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Usage

### Connecting to a Desktop Agent

```java
import org.finos.fdc3.getagent.GetAgent;
import org.finos.fdc3.getagent.GetAgentParams;
import org.finos.fdc3.api.DesktopAgent;

// Connect to a Desktop Agent via WebSocket
GetAgentParams params = new GetAgentParams.Builder()
    .identityUrl("https://myapp.example.com")
    .channelSelector(true)
    .intentResolver(true)
    .build();

DesktopAgent agent = GetAgent.getAgent(params);

// Now use the agent
agent.broadcast(new Contact("john.doe@example.com", "John Doe"));
```

### Broadcasting Context

```java
// Create and broadcast a context
Context contact = new Contact("jane@example.com", "Jane Smith");
agent.broadcast(contact);
```

### Listening for Context

```java
// Add a context listener
agent.addContextListener("fdc3.contact", context -> {
    System.out.println("Received contact: " + context);
});
```

### Raising Intents

```java
// Raise an intent
IntentResolution resolution = agent.raiseIntent("ViewChart", instrument);
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Java Application                        │
├─────────────────────────────────────────────────────────────┤
│  GetAgent  →  DesktopAgentProxy  →  WebSocketMessaging      │
└──────────────────────────┬──────────────────────────────────┘
                           │ WebSocket (DACP)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     Desktop Agent                            │
│              (e.g., FDC3 Sail, Finsemble)                   │
└─────────────────────────────────────────────────────────────┘
```

The Java API communicates with a Desktop Agent using the FDC3 Desktop Agent Communication Protocol (DACP) over WebSocket, enabling interoperability with any DACP-compliant agent.

## Testing

### Running Tests

```sh
mvn test
```

### Conformance Testing

The `fdc3-agent-proxy` module reuses Cucumber feature files from the official [FDC3 TypeScript repository](https://github.com/finos/FDC3) to ensure conformance with the specification.

Feature files are copied from:

```
FDC3/packages/fdc3-agent-proxy/test/features/
```

## Project Status

This project is under active development. Current focus areas:

- [ ] Complete implementation of `DesktopAgentProxy` messaging
- [ ] Full coverage of FDC3 2.2 API
- [ ] Conformance test suite passing
- [ ] Channel selector and intent resolver UI components
- [ ] Published Maven artifacts

## Contributing

1. Fork the repository (<https://github.com/finos/fdc3-java-api/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Read our [contribution guidelines](.github/CONTRIBUTING.md) and [Community Code of Conduct](https://www.finos.org/code-of-conduct)
4. Commit your changes (`git commit -am 'Add some fooBar'`)
5. Push to the branch (`git push origin feature/fooBar`)
6. Create a new Pull Request

_NOTE:_ Commits and pull requests to FINOS repositories will only be accepted from those contributors with an active, executed Individual Contributor License Agreement (ICLA) with FINOS OR who are covered under an existing and active Corporate Contribution License Agreement (CCLA) executed with FINOS. Commits from individuals not covered under an ICLA or CCLA will be flagged and blocked by the FINOS Clabot tool. Please note that some CCLAs require individuals/employees to be explicitly named on the CCLA.

_Need an ICLA? Unsure if you are covered under an existing CCLA? Email [help@finos.org](mailto:help@finos.org)_

## License

Copyright 2023 Wellington Management Company LLP

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
