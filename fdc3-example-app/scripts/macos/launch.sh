#!/usr/bin/env bash
# DEPRECATED: use jpackage instead (see README and scripts/macos/build-app.sh).
#
# A shell-script CFBundleExecutable prevents Java from receiving macOS OpenURL
# events (JDK-8360120). The jpackage-built .app uses a native launcher that
# works with Desktop.setOpenURIHandler().

echo "error: use scripts/macos/build-app.sh to build the macOS app bundle" >&2
exit 1
