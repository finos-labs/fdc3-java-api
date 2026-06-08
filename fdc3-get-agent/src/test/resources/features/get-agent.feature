Feature: GetAgent WebSocket Connection

  Background: Test Setup
    Given a mock WebSocket server in "server"

  Scenario: Successful connection with valid pairing
    Given "server" will accept pairing for sessionId "test-session" sharedSecret "test-secret" as appId "test-app" instanceId "test-instance"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sessionId "test-session" sharedSecret "test-secret" instanceId "test-instance" instanceUuid "test-uuid-123"
    When I wait for "{getAgent}" using argument "{params}"
    And "{result}" is not null
    When I call "{server}" with "getLastWSCP1"
    And I refer to "{result}" as "wscp1"
    Then "{wscp1}" is an object with the following contents
      | payload.sessionID | payload.sharedSecret | payload.instanceID | payload.instanceUUID |
      | test-session      | test-secret          | test-instance      | test-uuid-123        |

  Scenario: Connection fails with invalid pairing
    Given "server" will reject pairing with message "Access denied"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sessionId "test-session" sharedSecret "wrong-secret" instanceId "test-instance" instanceUuid "test-uuid-123"
    When I wait for "{getAgent}" using argument "{params}"
    And "{result}" is an error with message "Connection failed: Access denied"

  Scenario: Connection times out
    Given "server" will timeout on WSCP handshake
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sessionId "test-session" sharedSecret "test-secret" instanceId "test-instance" instanceUuid "test-uuid-123" timeout 2000
    When I wait for "{getAgent}" using argument "{params}"
    And "{result}" is an error

  Scenario: GetAgent returns implementation metadata
    Given "server" will accept pairing for sessionId "test-session" sharedSecret "test-secret" as appId "test-app" instanceId "test-instance"
    Given "server" will return provider "test-provider" fdc3Version "2.0"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sessionId "test-session" sharedSecret "test-secret" instanceId "test-instance" instanceUuid "test-uuid-123"
    When I wait for "{getAgent}" using argument "{params}"
    When I call "{result}" with "getInfo"
    And "{result}" is an object with the following contents
      | provider      | fdc3Version |
      | test-provider |         2.0 |

  Scenario: Reconnect with instanceUuid and no sharedSecret
    Given "server" will accept pairing for sessionId "test-session" sharedSecret "test-secret" as appId "test-app" instanceId "test-instance"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sessionId "test-session" sharedSecret "test-secret" instanceId "test-instance" instanceUuid "test-uuid-123"
    When I wait for "{getAgent}" using argument "{params}"
    Given "reconnectParams" is GetAgentParams reconnect with webSocketUrl "{server.url}" sessionId "test-session" instanceId "test-instance" instanceUuid "test-uuid-123"
    When I wait for "{getAgent}" using argument "{reconnectParams}"
    When I call "{server}" with "getLastWSCP1"
    And I refer to "{result}" as "wscp1reconnect"
    Then "{wscp1reconnect}" is an object with the following contents
      | payload.sessionID | payload.sharedSecret | payload.instanceID | payload.instanceUUID |
      | test-session      | {null}               | test-instance      | test-uuid-123        |

  Scenario: Disconnect sends WSCP3Goodbye
    Given "server" will accept pairing for sessionId "test-session" sharedSecret "test-secret" as appId "test-app" instanceId "test-instance"
    Given "params" is GetAgentParams with webSocketUrl "{server.url}" sessionId "test-session" sharedSecret "test-secret" instanceId "test-instance" instanceUuid "test-uuid-123"
    When I wait for "{getAgent}" using argument "{params}"
    When I refer to "{result}" as "agent"
    When I call "{agent}" with "disconnect"
    When I call "{server}" with "getLastWSCP3"
    And I refer to "{result}" as "wscp3"
    Then "{wscp3}" is not null
