# FDC3 Example App

A simple Java Swing application demonstrating FDC3 Desktop Agent connectivity via WebSocket.

## Features

- Connects to a Desktop Agent when launched with WSCP credentials, via protocol URL, or on user request
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
3. **Connect button** in the UI (manual entry)

On macOS, protocol URLs are delivered via `Desktop.setOpenURIHandler`, not as `main()` arguments.

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
3. Pass them via system properties or click **Connect** in the app
4. Use **Reconnect** after disconnect to resume the same instance with the same secret

## Protocol handler setup

Sail launches native apps using URLs of the form:

```
fdc3-java-app://launch?webSocketUrl=<url>>&sharedSecret=<sharedSecret>
```

The example app parses these in `ProtocolLaunchParams`. Delivery differs by platform:

| Platform | How the URL arrives |
| -------- | ------------------- |
| **macOS** | `Desktop.setOpenURIHandler` (requires a jpackage-built `.app`) |
| **Windows** | Command-line argument to the registered handler |

Helper scripts are in `fdc3-example-app/scripts/`.

### macOS

macOS requires a proper **app bundle** to register a custom URL scheme. Use **jpackage** (included with JDK 14+) — do **not** use the old manual `launch.sh` wrapper, which prevents Java from receiving OpenURL events ([JDK-8360120](https://bugs.openjdk.org/browse/JDK-8360120)).

**Build the app bundle** (macOS only):

```bash
mvn clean package -pl fdc3-example-app -am -Pmacos-app
```

Or run the script directly after `mvn package`:

```bash
chmod +x fdc3-example-app/scripts/macos/build-app.sh
fdc3-example-app/scripts/macos/build-app.sh
```

Output: `fdc3-example-app/target/jpackage-staging/FDC3 Java Example.app`

**Install and register** (copies to `~/Applications`, clears quarantine, and launches):

```bash
chmod +x fdc3-example-app/scripts/macos/install-app.sh
fdc3-example-app/scripts/macos/install-app.sh
```

Or manually:

```bash
APP="fdc3-example-app/target/jpackage-staging/FDC3 Java Example.app"
mkdir -p ~/Applications
cp -R "$APP" ~/Applications/
open "$APP"
```

**Test from Terminal:**

```bash
open "fdc3-java-app://launch?webSocketUrl=ws%3A%2F%2Flocalhost%3A8090%2Ffdc3%2Fws&sharedSecret=test-secret"
```

Check the in-app **Context Log** for `Received open URI:` and `Using WSCP connection config from desktop open URI`.

**Dock icon bounces but no window appears:** this was caused by `-XstartOnFirstThread` in the jpackage build (that flag is for SWT, not Swing — it prevents the AWT event loop from running). Rebuild with a current `-Pmacos-app` build; `build-app.sh` no longer passes that flag.

**If the app won't start**, diagnose in this order:

1. **Run the launcher from Terminal** (shows Java errors on stderr):

   ```bash
   APP="fdc3-example-app/target/jpackage-staging/FDC3 Java Example.app"
   "$APP/Contents/MacOS/FDC3 Java Example"
   ```

2. **Double-click blocked (unsigned dev build):** use `install-app.sh`, or right-click → Open once.

3. **Check the code signature:**

   ```bash
   codesign --verify --deep --strict --verbose=2 "$APP"
   ```

   Rebuild with `-Pmacos-app` if verification fails.

4. **Crash logs:** open **Console.app** → Crash Reports, or:

   ```bash
   ls ~/Library/Logs/DiagnosticReports/*FDC3*
   log stream --predicate 'process == "FDC3 Java Example"' --level debug
   ```

**After rebuilding:** quit any running copy (`Cmd+Q`), rebuild with `-Pmacos-app`, and re-copy to `~/Applications` if you installed it there. The jpackage bundle always embeds the latest JAR from `target/`.

To test without the protocol handler:

```bash
java -jar fdc3-example-app/target/fdc3-example-app-1.0.0-SNAPSHOT.jar \
  'fdc3-java-app://launch?webSocketUrl=ws%3A%2F%2Flocalhost%3A8090%2Ffdc3%2Fws&sharedSecret=test-secret'
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
