#!/usr/bin/env bash
# Installs the jpackage-built .app to ~/Applications and launches it.
#
# Usage:
#   fdc3-example-app/scripts/macos/install-app.sh
#
# Run build-app.sh (or mvn package -Pmacos-app) first.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MODULE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
APP_NAME="FDC3 Java Example"
SOURCE_APP="$MODULE_DIR/target/jpackage-staging/$APP_NAME.app"
DEST_DIR="${HOME}/Applications"
DEST_APP="$DEST_DIR/$APP_NAME.app"

if [[ ! -d "$SOURCE_APP" ]]; then
  echo "error: $SOURCE_APP not found — run: mvn package -pl fdc3-example-app -am -Pmacos-app" >&2
  exit 1
fi

echo "Quitting any running copy..."
pkill -x "$APP_NAME" 2>/dev/null || true
sleep 1

mkdir -p "$DEST_DIR"
echo "Installing to $DEST_APP..."
rm -rf "$DEST_APP"
ditto "$SOURCE_APP" "$DEST_APP"

# Remove quarantine/Gatekeeper flags that block double-click from Finder.
xattr -cr "$DEST_APP"

echo "Launching..."
open "$DEST_APP"

echo ""
echo "Installed and launched. Double-click from ~/Applications should work now."
echo "First time only: if macOS still blocks it, open System Settings →"
echo "Privacy & Security → Open Anyway, or right-click the app → Open."
