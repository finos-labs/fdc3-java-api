# FDC3 Example App

A simple Java Swing application demonstrating FDC3 Desktop Agent connectivity via WebSocket.

## Features

- Connects to a Desktop Agent on startup using WSCP (WebSocket Connection Protocol)
- Displays available user channels and allows joining/leaving channels
- Shows a log of context broadcasts received on the current channel
- Supports adding and removing context listeners on the user channel

## Prerequisites

- Java 11 or higher
- A running FDC3 Desktop Agent (e.g., FDC3-Sail)

## Building

From the `fdc3-java-api` root directory:

```bash
mvn clean package -pl fdc3-example-app -am
```

This will create an executable JAR with all dependencies at:
`fdc3-example-app/target/fdc3-example-app-1.0.0-SNAPSHOT.jar`

## Running

The application requires the following system properties (or environment variables):

| Property                 | Description                                              |
| ------------------------ | -------------------------------------------------------- |
| `FDC3_WEBSOCKET_URL`     | WebSocket URL (e.g. `ws://localhost:8090/fdc3/ws`)       |
| `FDC3_CONNECTION_SECRET` | Per-instance shared secret from the Sail pairing UI      |

### Example

```bash
java -DFDC3_WEBSOCKET_URL=ws://localhost:8090/fdc3/ws \
     -DFDC3_CONNECTION_SECRET=your-pairing-secret-from-sail \
     -jar fdc3-example-app/target/fdc3-example-app-1.0.0-SNAPSHOT.jar
```

### Using with FDC3-Sail

1. Open the Sail app directory and select a native app
2. Copy the **WebSocket URL** and **shared secret** from the pairing credentials panel
3. Pass them via system properties or enter them when prompted at startup
4. Use **Reconnect** after disconnect to resume the same instance with the same secret

## UI Overview

### Status Bar

Shows the connection status to the Desktop Agent.

### User Channel

- **Current Channel**: Dropdown to select which user channel to join
- **Leave Channel**: Button to leave the current channel

### Context Log

Displays timestamped log entries for:

- Connection events
- Channel join/leave events
- Context messages received on the current channel

### Context Listener

- **Add Listener**: Adds a context listener for all context types on the user channel
- **Remove Listener**: Removes the active context listener

## Example Usage

1. Start the application with the required system properties
2. Once connected, select a user channel from the dropdown
3. Click "Add Listener" to start receiving context broadcasts
4. Context received from other apps on the same channel will appear in the log
5. Use "Remove Listener" to stop receiving context
6. Use "Leave Channel" to leave the current channel
