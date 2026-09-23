#!/usr/bin/env bash
# Builds a macOS .app bundle with jpackage and registers the fdc3-java-app:// URL scheme.
#
# Usage (from repo root or fdc3-example-app):
#   fdc3-example-app/scripts/macos/build-app.sh [app-version]
#
# Requires: JDK with jpackage (14+), macOS, a built shaded JAR in target/

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MODULE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
APP_NAME="FDC3 Java Example"
MAIN_JAR="fdc3-example-app-1.0.0-SNAPSHOT.jar"
MAIN_CLASS="org.finos.fdc3.example.ExampleApp"
BUNDLE_ID="org.finos.fdc3.example"
URL_SCHEME="fdc3-java-app"
RESOURCE_DIR="$SCRIPT_DIR/jpackage-resources"

APP_VERSION="${1:-1.0.0}"
APP_VERSION="${APP_VERSION/-SNAPSHOT/}"

JAR_PATH="$MODULE_DIR/target/$MAIN_JAR"
INPUT_DIR="$MODULE_DIR/target/jpackage-input"
OUTPUT_DIR="$MODULE_DIR/target/jpackage-staging"
APP_BUNDLE="$OUTPUT_DIR/$APP_NAME.app"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "error: macOS app bundles must be built on macOS" >&2
  exit 1
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "error: JAR not found at $JAR_PATH — run: mvn package -pl fdc3-example-app -am" >&2
  exit 1
fi

if ! command -v jpackage >/dev/null 2>&1; then
  echo "error: jpackage not found on PATH (install a JDK that includes jpackage)" >&2
  exit 1
fi

echo "Building $APP_NAME.app (version $APP_VERSION)..."
rm -rf "$APP_BUNDLE"
mkdir -p "$INPUT_DIR"
cp "$JAR_PATH" "$INPUT_DIR/"

jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --input "$INPUT_DIR" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --app-version "$APP_VERSION" \
  --mac-package-identifier "$BUNDLE_ID" \
  --resource-dir "$RESOURCE_DIR" \
  --dest "$OUTPUT_DIR"

if ! codesign --verify --deep --strict --verbose=2 "$APP_BUNDLE" >/dev/null 2>&1; then
  echo "error: jpackage app bundle signature verification failed" >&2
  exit 1
fi

echo ""
echo "Built: $APP_BUNDLE"
echo ""
echo "Install and launch (recommended — clears Gatekeeper quarantine on copy):"
echo "  $SCRIPT_DIR/install-app.sh"
echo ""
echo "Or manually:"
echo "  open \"$APP_BUNDLE\""
echo "  open \"${URL_SCHEME}://launch?webSocketUrl=ws%3A%2F%2Flocalhost%3A8090%2Ffdc3%2Fws&sharedSecret=test-secret\""
echo ""
echo "If double-click in Finder does nothing, the app is unsigned — use install-app.sh"
echo "or right-click → Open once, then double-click will work."
