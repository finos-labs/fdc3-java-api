/**
 * Copyright FINOS and its Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.finos.fdc3.getagent.steps;

import static io.github.robmoffat.support.MatchingUtils.handleResolve;

import org.finos.fdc3.api.DesktopAgent;
import org.finos.fdc3.getagent.GetAgent;
import org.finos.fdc3.getagent.GetAgentParams;
import org.finos.fdc3.getagent.support.MockWebSocketServer;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.github.robmoffat.world.PropsWorld;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Domain-specific Cucumber steps for GetAgent WSCP integration tests.
 */
public class GetAgentSteps {

    private final PropsWorld world;

    public GetAgentSteps(PropsWorld world) {
        this.world = world;
    }

    @Before
    public void registerGetAgentFunction() {
        world.set("getAgent", (Function<GetAgentParams, CompletionStage<DesktopAgent>>) GetAgent::getAgent);
    }

    @Given("a mock WebSocket server in {string}")
    public void createMockServer(String name) throws Exception {
        MockWebSocketServer server = new MockWebSocketServer();
        server.start();
        world.put(name, server);
    }

    @Given("{string} will accept pairing for sessionId {string} sharedSecret {string} as appId {string} instanceId {string}")
    public void acceptPairing(String serverName, String sessionId, String secret,
                              String appId, String instanceId) {
        getServer(serverName).acceptPairing(sessionId, secret, appId, instanceId);
    }

    @Given("{string} will reject pairing with message {string}")
    public void rejectPairing(String serverName, String message) {
        getServer(serverName).rejectPairing(message);
    }

    @Given("{string} will timeout on WSCP handshake")
    public void timeoutPairing(String serverName) {
        getServer(serverName).timeoutPairing();
    }

    @Given("{string} will return provider {string} fdc3Version {string}")
    public void setProvider(String serverName, String provider, String version) {
        getServer(serverName).setImplementationMetadata(provider, version);
    }

    @Given("{string} is GetAgentParams with webSocketUrl {string} sessionId {string} sharedSecret {string} instanceId {string} instanceUuid {string}")
    public void buildParams(String name, String url, String sessionId, String secret,
                            String instanceId, String instanceUuid) throws Exception {
        world.put(name, GetAgentParams.builder()
                .webSocketUrl((String) handleResolve(url, world))
                .sessionId(sessionId)
                .sharedSecret(secret)
                .instanceId(instanceId)
                .instanceUuid(instanceUuid)
                .build());
    }

    @Given("{string} is GetAgentParams reconnect with webSocketUrl {string} sessionId {string} instanceId {string} instanceUuid {string}")
    public void buildReconnectParams(String name, String url, String sessionId,
                                     String instanceId, String instanceUuid) throws Exception {
        world.put(name, GetAgentParams.builder()
                .webSocketUrl((String) handleResolve(url, world))
                .sessionId(sessionId)
                .instanceId(instanceId)
                .instanceUuid(instanceUuid)
                .build());
    }

    @Given("{string} is GetAgentParams with webSocketUrl {string} sessionId {string} sharedSecret {string} instanceId {string} instanceUuid {string} timeout {long}")
    public void buildParamsWithTimeout(String name, String url, String sessionId, String secret,
                                       String instanceId, String instanceUuid, long timeout) throws Exception {
        world.put(name, GetAgentParams.builder()
                .webSocketUrl((String) handleResolve(url, world))
                .sessionId(sessionId)
                .sharedSecret(secret)
                .instanceId(instanceId)
                .instanceUuid(instanceUuid)
                .timeoutMs(timeout)
                .build());
    }

    private MockWebSocketServer getServer(String name) {
        return (MockWebSocketServer) world.get(name);
    }
}
