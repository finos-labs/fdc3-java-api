#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
# Copyright FINOS FDC3 contributors - see NOTICE file
#
# Post-process quicktype Java output: remove duplicate Context types and fix imports.

set -eu

cd "${1:?usage: fix-context-imports.sh <generated-context-dir>}"

rm -f Context.java ContextElement.java 2>/dev/null || true

for f in *.java; do
  [ -f "$f" ] || continue
  # Cross-platform in-place edit (GNU sed vs BSD sed)
  if sed --version 2>/dev/null | grep -q GNU; then
    sed -i -e 's/ContextElement/Context/g' "$f"
    if grep -qE '(private|public) Context |Context get|Context\[\]|ContextFromJsonString' "$f"; then
      sed -i '/^package org.finos.fdc3.context;$/a\
import org.finos.fdc3.api.context.Context;' "$f"
    fi
  else
    sed -i '' -e 's/ContextElement/Context/g' "$f"
    if grep -qE '(private|public) Context |Context get|Context\[\]|ContextFromJsonString' "$f"; then
      sed -i '' '/^package org.finos.fdc3.context;$/a\
import org.finos.fdc3.api.context.Context;' "$f"
    fi
  fi
done
