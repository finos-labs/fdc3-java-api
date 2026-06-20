#!/bin/bash
# Edit JAR_PATH below, then copy this file to:
#   FDC3 Java Example.app/Contents/MacOS/launch.sh
# and run: chmod +x FDC3\ Java\ Example.app/Contents/MacOS/launch.sh

JAR_PATH="/absolute/path/to/fdc3-example-app-1.0.0-SNAPSHOT.jar"

exec java -jar "$JAR_PATH" "$@"
