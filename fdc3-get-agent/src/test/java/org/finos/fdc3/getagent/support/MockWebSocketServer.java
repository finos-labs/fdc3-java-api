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

package org.finos.fdc3.getagent.support;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import org.finos.fdc3.api.metadata.AppMetadata;
import org.finos.fdc3.api.metadata.ImplementationMetadata;
import org.finos.fdc3.schema.SchemaConverter;
import org.finos.fdc3.schema.WebConnectionProtocol1HelloMeta;
import org.finos.fdc3.schema.WebConnectionProtocol4ValidateAppIdentity;
import org.finos.fdc3.schema.WebConnectionProtocol5ValidateAppIdentityFailedResponse;
import org.finos.fdc3.schema.WebConnectionProtocol5ValidateAppIdentityFailedResponsePayload;
import org.finos.fdc3.schema.WebConnectionProtocol5ValidateAppIdentityFailedResponseType;
import org.finos.fdc3.schema.WebConnectionProtocol5ValidateAppIdentitySuccessResponse;
import org.finos.fdc3.schema.WebConnectionProtocol5ValidateAppIdentitySuccessResponsePayload;
import org.finos.fdc3.schema.WebConnectionProtocol5ValidateAppIdentitySuccessResponseType;
import org.finos.fdc3.schema.WebConnectionProtocol6Goodbye;
import org.glassfish.tyrus.server.Server;

/**
 * Mock WebSocket server for testing GetAgent.
 * Simulates a Desktop Agent responding to WCP messages.
 * 
 * Uses schema types from fdc3-schema for type-safe JSON handling:
 * - WebConnectionProtocol4ValidateAppIdentity
 * - WebConnectionProtocol5ValidateAppIdentitySuccessResponse
 * - WebConnectionProtocol5ValidateAppIdentityFailedResponse
 * - WebConnectionProtocol6Goodbye
 */
@ServerEndpoint("/fdc3")
public class MockWebSocketServer {

    private static final SchemaConverter converter = new SchemaConverter();

    // Instance-specific state (accessed via static holder for @ServerEndpoint)
    private static final CopyOnWriteArrayList<Session> sessions = new CopyOnWriteArrayList<>();
    private static volatile MockWebSocketServer currentInstance;

    private Server server;
    private int port;
    
    // Configuration for responses
    private String acceptedIdentityUrl;
    private String responseAppId;
    private String responseInstanceId;
    private String responseInstanceUuid = "response-uuid";
    private String rejectMessage;
    private boolean shouldTimeout;
    private String providerName = "test-provider";
    private String fdc3Version = "2.0";
    
    // Received messages for assertions (using typed schema classes)
    private WebConnectionProtocol4ValidateAppIdentity lastWCP4;
    private WebConnectionProtocol6Goodbye lastWCP6;

    public void start() throws Exception {
        port = 8025 + (int) (Math.random() * 1000);
        server = new Server("localhost", port, "/", null, MockWebSocketServer.class);
        currentInstance = this;
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
        currentInstance = null;
        sessions.clear();
    }

    public String getUrl() {
        return "ws://localhost:" + port + "/fdc3";
    }

    public void acceptIdentity(String identityUrl, String appId, String instanceId) {
        acceptedIdentityUrl = identityUrl;
        responseAppId = appId;
        responseInstanceId = instanceId;
        rejectMessage = null;
        shouldTimeout = false;
    }

    public void rejectIdentity(String message) {
        acceptedIdentityUrl = null;
        rejectMessage = message;
        shouldTimeout = false;
    }

    public void timeoutIdentity() {
        shouldTimeout = true;
    }

    public void setImplementationMetadata(String provider, String version) {
        providerName = provider;
        fdc3Version = version;
    }

    // Property accessors for assertions
    public WebConnectionProtocol4ValidateAppIdentity getLastWCP4() {
        return lastWCP4;
    }

    public WebConnectionProtocol6Goodbye getLastWCP6() {
        return lastWCP6;
    }

    // WebSocket endpoint methods - these are called on the endpoint instance
    // created by the container, so we delegate to the currentInstance
    
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        sessions.remove(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            // First, peek at the type to determine which class to use
            String type = extractType(message);

            // Delegate to current instance for state management
            MockWebSocketServer instance = currentInstance;
            if (instance == null) {
                return;
            }

            if ("WCP4ValidateAppIdentity".equals(type)) {
                WebConnectionProtocol4ValidateAppIdentity wcp4 = 
                    converter.fromJson(message, WebConnectionProtocol4ValidateAppIdentity.class);
                instance.lastWCP4 = wcp4;
                instance.handleValidateAppIdentity(wcp4, session);
            } else if ("WCP6Goodbye".equals(type)) {
                WebConnectionProtocol6Goodbye wcp6 = 
                    converter.fromJson(message, WebConnectionProtocol6Goodbye.class);
                instance.lastWCP6 = wcp6;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract the "type" field from a JSON message without full parsing.
     */
    private String extractType(String json) throws IOException {
        return converter.getObjectMapper().readTree(json).path("type").asText(null);
    }

    private void handleValidateAppIdentity(WebConnectionProtocol4ValidateAppIdentity request, Session session) {
        if (shouldTimeout) {
            // Don't respond - simulate timeout
            return;
        }

        String connectionAttemptUuid = request.getMeta().getConnectionAttemptUUID();
        String identityUrl = request.getPayload().getIdentityURL();

        try {
            String responseJson;

            if (rejectMessage != null) {
                responseJson = buildFailedResponse(connectionAttemptUuid, rejectMessage);
            } else if (acceptedIdentityUrl != null && acceptedIdentityUrl.equals(identityUrl)) {
                responseJson = buildSuccessResponse(connectionAttemptUuid);
            } else {
                responseJson = buildFailedResponse(connectionAttemptUuid, "Unknown identity URL: " + identityUrl);
            }

            session.getBasicRemote().sendText(responseJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String buildSuccessResponse(String connectionAttemptUuid) throws IOException {
        WebConnectionProtocol5ValidateAppIdentitySuccessResponse response = 
            new WebConnectionProtocol5ValidateAppIdentitySuccessResponse();

        // Build meta
        WebConnectionProtocol1HelloMeta meta = new WebConnectionProtocol1HelloMeta();
        meta.setConnectionAttemptUUID(connectionAttemptUuid);
        meta.setTimestamp(OffsetDateTime.now());
        response.setMeta(meta);

        // Set type
        response.setType(WebConnectionProtocol5ValidateAppIdentitySuccessResponseType.WCP5_VALIDATE_APP_IDENTITY_RESPONSE);

        // Build payload
        WebConnectionProtocol5ValidateAppIdentitySuccessResponsePayload payload = 
            new WebConnectionProtocol5ValidateAppIdentitySuccessResponsePayload();
        payload.setAppID(responseAppId);
        payload.setInstanceID(responseInstanceId);
        payload.setInstanceUUID(responseInstanceUuid);

        // Build implementation metadata
        ImplementationMetadata implMeta = new ImplementationMetadata();
        implMeta.setFdc3Version(fdc3Version);
        implMeta.setProvider(providerName);
        implMeta.setProviderVersion("1.0.0");

        // Build app metadata
        AppMetadata appMeta = new AppMetadata();
        appMeta.setAppId(responseAppId);
        appMeta.setInstanceId(responseInstanceId);
        implMeta.setAppMetadata(appMeta);

        // Build optional features
        ImplementationMetadata.OptionalFeatures optFeatures = new ImplementationMetadata.OptionalFeatures();
        optFeatures.setOriginatingAppMetadata(true);
        optFeatures.setUserChannelMembershipAPIs(true);
        optFeatures.setDesktopAgentBridging(false);
        implMeta.setOptionalFeatures(optFeatures);

        payload.setImplementationMetadata(implMeta);
        response.setPayload(payload);

        return converter.toJson(response);
    }

    private String buildFailedResponse(String connectionAttemptUuid, String message) throws IOException {
        WebConnectionProtocol5ValidateAppIdentityFailedResponse response = 
            new WebConnectionProtocol5ValidateAppIdentityFailedResponse();

        // Build meta
        WebConnectionProtocol1HelloMeta meta = new WebConnectionProtocol1HelloMeta();
        meta.setConnectionAttemptUUID(connectionAttemptUuid);
        meta.setTimestamp(OffsetDateTime.now());
        response.setMeta(meta);

        // Set type
        response.setType(WebConnectionProtocol5ValidateAppIdentityFailedResponseType.WCP5_VALIDATE_APP_IDENTITY_FAILED_RESPONSE);

        // Build payload
        WebConnectionProtocol5ValidateAppIdentityFailedResponsePayload payload = 
            new WebConnectionProtocol5ValidateAppIdentityFailedResponsePayload();
        payload.setMessage(message);
        response.setPayload(payload);

        return converter.toJson(response);
    }
} 
