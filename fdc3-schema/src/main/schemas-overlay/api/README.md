# API schema overlay

The build downloads `@finos/fdc3-schema` from npm (see `fdc3.schema.version` in
`fdc3-schema/pom.xml`) and copies it into `target/schema-work`. The files in this
directory are then copied over the top, replacing or adding to the published set.

This directory exists only because the schemas below are not yet in a published
release, and Java code in this project depends on what they add. Every file here is a
temporary patch: when the change it carries reaches npm, delete the file. When the
directory is empty, delete the directory and the `copy-api-overlay` executions in
`fdc3-schema/pom.xml` and `fdc3-context/pom.xml`.

Do not add files here for any other reason. In particular, do not overlay a schema to
pick up a cosmetic upstream fix such as a corrected description or title — that would
make this project inconsistent with the published release for no benefit. Wait for the
next release instead. Schema changes belong upstream in the
[FDC3 repository](https://github.com/finos/FDC3), not in this binding.

There is deliberately no context overlay: `@finos/fdc3-context` is consumed exactly as
published.

## Contents

Measured against `@finos/fdc3-schema@3.0.0-alpha.2`. All other API, bridging and
bridgingAsyncAPI schemas in that release are byte-for-byte equivalent to what this
project needs, so they are consumed straight from npm.

### Absent from the published package

The WebSocket Connection Protocol schemas. `fdc3-get-agent` is built on the types
generated from these, so the build cannot succeed without them.

- `WSCPApplicationConnect.schema.json`
- `WSCPConnectFailed.schema.json`
- `WSCPConnectionStep.schema.json`
- `WSCPDesktopAgentConnect.schema.json`
- `WSCPGoodbye.schema.json`

### Published, but missing a field

- `addIntentListenerRequest.schema.json` — adds `contextTypes`, required by
  `DesktopAgent.addIntentListenerWithContext`.
- `raiseIntentRequest.schema.json` — adds `newInstance`.
- `raiseIntentForContextRequest.schema.json` — adds `newInstance`.
