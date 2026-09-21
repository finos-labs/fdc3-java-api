# Feature files

These Gherkin features are the FDC3 reference test suite for the Desktop Agent proxy. They
originate in the TypeScript implementation, at `packages/fdc3-agent-proxy/test/features` in the
[FDC3 repository](https://github.com/finos/FDC3), and were copied here so that the Java proxy
is held to the same behavioural contract as the reference one.

Cucumber loads them from `classpath:temporary-features`, as configured in `RunCucumberTest`.
They are placed on the test classpath by Maven's ordinary test-resource handling; no copy step
is involved.
