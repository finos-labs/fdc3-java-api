# FDC3 Example App

A simple Java Swing application demonstrating FDC3 Desktop Agent connectivity via WebSocket.

## Features

- Connects to a Desktop Agent on startup using WSCP (WebSocket Connection Protocol)
- Supports launch via the `fdc3-java-app://` custom protocol handler (from FDC3-Sail or other DAs)
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

The application accepts WSCP connection parameters from (in priority order):

1. **Protocol handler launch URL** — `fdc3-java-app://launch?webSocketUrl=...&sharedSecret=...`
2. **System properties / environment variables**
3. **Interactive prompts** at startup

| Property / Variable       | Description                                              |
| ------------------------- | -------------------------------------------------------- |
| `FDC3_WEBSOCKET_URL`      | WebSocket URL (e.g. `ws://localhost:8090/fdc3/ws`)       |
| `FDC3_CONNECTION_SECRET`  | Per-instance shared secret from the Sail pairing UI      |

### Manual launch

```bash
java -DFDC3_WEBSOCKET_URL=ws://localhost:8090/fdc3/ws \
     -DFDC3_CONNECTION_SECRET=your-pairing-secret-from-sail \
     -jar fdc3-example-app/target/fdc3-example-app-1.0.0-SNAPSHOT.jar
```

### Using with FDC3-Sail

**Option A — Open On Desktop (recommended)**

1. Register the `fdc3-java-app://` protocol handler (see below)
2. Start FDC3-Sail and open **Start Application**
3. Select **FDC3 Java Example App**
4. Click **Open On Desktop** — Sail mints a pairing secret and launches the app via the protocol handler

**Option B — Manual credentials**

1. Open the Sail app directory and select the native app
2. Copy the **WebSocket URL** and **shared secret** from the pairing credentials panel
3. Pass them via system properties or enter them when prompted at startup
4. Use **Reconnect** after disconnect to resume the same instance with the same secret

## Protocol handler setup

Sail launches native apps using URLs of the form:

```
fdc3-java-app://launch?webSocketUrl=<url>>&sharedSecret=<sharedSecret>
```

The OS passes this URL as a command-line argument to the registered handler. The example app parses it in `ProtocolLaunchParams`.

Helper scripts are in `fdc3-example-app/scripts/`.

### macOS

macOS requires a small **app bundle** to register a custom URL scheme. The `Info.plist` registers `fdc3-java-app://`; `launch.sh` runs the JAR and forwards the launch URL (passed as an argument by the OS).

1. Build the JAR (see above).
2. Edit `scripts/macos/launch.sh` and set `JAR_PATH` to the absolute path of your built JAR.
3. Create the app bundle:

```bash
APP="FDC3 Java Example.app"
mkdir -p "$APP/Contents/MacOS"
cp scripts/macos/Info.plist "$APP/Contents/"
cp scripts/macos/launch.sh "$APP/Contents/MacOS/"
chmod +x "$APP/Contents/MacOS/launch.sh"
```

4. Copy the app bundle to `/Applications` or another location, then open it once so macOS registers the URL scheme.
5. Test from Terminal:

```bash
open "fdc3-java-app://launch?webSocketUrl=ws%3A%2F%2Flocalhost%3A8090%2Ffdc3%2Fws&sharedSecret=test-secret"
```

### Windows

1. Build the JAR (see above).
2. Register the protocol handler for your user account.

**Using PowerShell (recommended):**

```powershell
cd fdc3-example-app\scripts\windows
.\register-protocol.ps1 -JarPath "C:\full\path\to\fdc3-example-app-1.0.0-SNAPSHOT.jar"
```

**Using a .reg file:**

1. Edit `scripts/windows/register-protocol.reg` and replace the placeholder paths to `java.exe` and the JAR.
2. Double-click the file or run `reg import register-protocol.reg`.

3. Test from Command Prompt:

```cmd
start "" "fdc3-java-app://launch?webSocketUrl=ws%3A%2F%2Flocalhost%3A8090%2Ffdc3%2Fws&sharedSecret=test-secret"
```

Registration is per-user (`HKCU`) when using the PowerShell script, or machine-wide when using `HKEY_CLASSES_ROOT` in the `.reg` file.

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

1. Register the protocol handler and start FDC3-Sail, or launch manually with credentials
2. Once connected, select a user channel from the dropdown
3. Click "Add Listener" to start receiving context broadcasts
4. Context received from other apps on the same channel will appear in the log
5. Use "Remove Listener" to stop receiving context
6. Use "Leave Channel" to leave the current channel
