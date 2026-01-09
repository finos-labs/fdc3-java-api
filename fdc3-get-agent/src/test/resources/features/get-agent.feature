Feature: GetAgent WebSocket Connection

  Background: Test Setup
    Given a mock WebSocket server in "server"

  Scenario: Successful connection with valid identity
    Given "server" will accept identity for "https://myapp.example.com/" as appId "test-app" instanceId "test-instance"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" identityUrl "https://myapp.example.com/" instanceId "test-instance" instanceUuid "test-uuid-123"
    When I call "GetAgent" with "getAgent" with parameter "{params}"
    Then the promise "result" should resolve
    And "{result}" is not null
    When I refer to the server "server" last WCP4 as "wcp4"
    Then "{wcp4}" is an object with the following contents
      | payload.identityURL        | payload.instanceID | payload.instanceUUID |
      | https://myapp.example.com/ | test-instance      | test-uuid-123        |

  Scenario: Connection fails with invalid identity
    Given "server" will reject identity with message "Access denied"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" identityUrl "https://unknown.example.com/" instanceId "test-instance" instanceUuid "test-uuid-123"
    When I call "GetAgent" with "getAgent" with parameter "{params}"
    Then the promise "result" should resolve
    And "{result}" is an error with message "Access denied"

  Scenario: Connection times out
    Given "server" will timeout on identity validation
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" identityUrl "https://myapp.example.com/" instanceId "test-instance" instanceUuid "test-uuid-123" timeout 2000
    When I call "GetAgent" with "getAgent" with parameter "{params}"
    Then the promise "result" should resolve
    And "{result}" is an error

  Scenario: GetAgent returns implementation metadata
    Given "server" will accept identity for "https://myapp.example.com/" as appId "test-app" instanceId "test-instance"
    Given "server" will return provider "test-provider" fdc3Version "2.0"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" identityUrl "https://myapp.example.com/" instanceId "test-instance" instanceUuid "test-uuid-123"
    When I call "GetAgent" with "getAgent" with parameter "{params}"
    Then the promise "result" should resolve
    When I call "{result}" with "getInfo"
    Then the promise "result" should resolve
    And "{result}" is an object with the following contents
      | provider      | fdc3Version |
      | test-provider |         2.0 |

  Scenario: Disconnect sends WCP6Goodbye
    Given "server" will accept identity for "https://myapp.example.com/" as appId "test-app" instanceId "test-instance"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" identityUrl "https://myapp.example.com/" instanceId "test-instance" instanceUuid "test-uuid-123"
    When I call "GetAgent" with "getAgent" with parameter "{params}"
    Then the promise "result" should resolve
    When I refer to "{result}" as "agent"
    When I call "{agent}" with "disconnect"
    Then the promise "result" should resolve
    When I refer to the server "server" last WCP6 as "wcp6"
    Then "{wcp6}" is not null
