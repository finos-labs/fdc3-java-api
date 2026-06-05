#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
# Copyright FINOS FDC3 contributors - see NOTICE file
#
# Assemble a unified schema layout (api/, bridging/, context/) for quicktype.
# api/ and bridging/ come from the schema source dir; context/ always from npm.

set -eu

schemas_work_dir="${1:?usage: prepare-schemas.sh <work-dir> <source-dir> <context-dir>}"
schemas_source_dir="${2:?usage: prepare-schemas.sh <work-dir> <source-dir> <context-dir>}"
context_schemas_dir="${3:?usage: prepare-schemas.sh <work-dir> <source-dir> <context-dir>}"

mkdir -p "$schemas_work_dir"
ln -sfn "$schemas_source_dir/api" "$schemas_work_dir/api"
ln -sfn "$schemas_source_dir/bridging" "$schemas_work_dir/bridging"
ln -sfn "$context_schemas_dir" "$schemas_work_dir/context"
