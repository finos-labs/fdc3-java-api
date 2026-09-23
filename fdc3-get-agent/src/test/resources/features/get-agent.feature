Feature: GetAgent WebSocket Connection

  Background: Test Setup
    Given a mock WebSocket server in "server"

  Scenario: Successful connection with valid pairing
    Given "server" will accept pairing for sharedSecret "test-secret" as appId "test-app" instanceId "test-instance"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sharedSecret "test-secret"
    When I wait for "{getAgent}" using argument "{params}"
    And "{result}" is not null
    When I call "{server}" with "getLastApplicationConnect"
    And I refer to "{result}" as "wscpConnect"
    Then "{wscpConnect}" is an object with the following contents
      | payload.sharedSecret |
      | test-secret          |

  Scenario: Connection fails with invalid pairing
    Given "server" will reject pairing with message "Access denied"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sharedSecret "wrong-secret"
    When I wait for "{getAgent}" using argument "{params}"
    And "{result}" is an error with message "Connection failed: Access denied"

  Scenario: Connection times out
    Given "server" will timeout on WSCP handshake
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sharedSecret "test-secret" timeout 2000
    When I wait for "{getAgent}" using argument "{params}"
    And "{result}" is an error

  Scenario: GetAgent returns implementation metadata
    Given "server" will accept pairing for sharedSecret "test-secret" as appId "test-app" instanceId "test-instance"
    Given "server" will return provider "test-provider" fdc3Version "2.0"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sharedSecret "test-secret"
    When I wait for "{getAgent}" using argument "{params}"
    When I call "{result}" with "getInfo"
    And "{result}" is an object with the following contents
      | provider      | fdc3Version |
      | test-provider |         2.0 |

  Scenario: Reconnect with the same sharedSecret
    Given "server" will accept pairing for sharedSecret "test-secret" as appId "test-app" instanceId "test-instance"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sharedSecret "test-secret"
    When I wait for "{getAgent}" using argument "{params}"
    Given "reconnectParams" is GetAgentParams with webSocketUrl "{server.url}" sharedSecret "test-secret"
    When I wait for "{getAgent}" using argument "{reconnectParams}"
    When I call "{server}" with "getLastApplicationConnect"
    And I refer to "{result}" as "wscpReconnect"
    Then "{wscpReconnect}" is an object with the following contents
      | payload.sharedSecret |
      | test-secret          |

  Scenario: Disconnect sends WSCPGoodbye
    Given "server" will accept pairing for sharedSecret "test-secret" as appId "test-app" instanceId "test-instance"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sharedSecret "test-secret"
    When I wait for "{getAgent}" using argument "{params}"
    When I refer to "{result}" as "agent"
    When I call "{agent}" with "disconnect"
    When I call "{server}" with "getLastGoodbye"
    And I refer to "{result}" as "goodbye"
    Then "{goodbye}" is not null
