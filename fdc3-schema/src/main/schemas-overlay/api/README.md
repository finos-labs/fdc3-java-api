# API schema overlay

The build downloads `@finos/fdc3-schema` from npm (see `fdc3.npm.version` in the root
`pom.xml`) and copies it into `target/schema-work`. The files in this directory are
then copied over the top, adding schemas that are not yet in a published release.

This directory exists only for the WebSocket Connection Protocol (WSCP) schemas.
`fdc3-get-agent` generates types from these, and they are not in
`@finos/fdc3-schema@3.0.0-alpha.5` (or any earlier cut). When WSCP lands in a published
npm release, delete every file here, then remove this directory and the
`copy-api-overlay` executions in `fdc3-schema/pom.xml` and `fdc3-context/pom.xml`.

Do not add DACP or other API schemas here — those come from npm unmodified. Schema
changes belong upstream in the [FDC3 repository](https://github.com/finos/FDC3), not
in this binding.
