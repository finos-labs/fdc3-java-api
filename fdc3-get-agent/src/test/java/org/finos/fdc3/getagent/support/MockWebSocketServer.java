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
import java.util.UUID;
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
import org.finos.fdc3.schema.WebSocketConnectionProtocol1ConnectRequest;
import org.finos.fdc3.schema.WebSocketConnectionProtocol1ConnectRequestMeta;
import org.finos.fdc3.schema.WebSocketConnectionProtocol2ConnectFailedResponse;
import org.finos.fdc3.schema.WebSocketConnectionProtocol2ConnectFailedResponsePayload;
import org.finos.fdc3.schema.WebSocketConnectionProtocol2ConnectFailedResponseType;
import org.finos.fdc3.schema.WebSocketConnectionProtocol2ConnectSuccessResponse;
import org.finos.fdc3.schema.WebSocketConnectionProtocol2ConnectSuccessResponsePayload;
import org.finos.fdc3.schema.WebSocketConnectionProtocol2ConnectSuccessResponseType;
import org.finos.fdc3.schema.AddContextListenerResponseMeta;
import org.finos.fdc3.schema.GetInfoRequest;
import org.finos.fdc3.schema.GetInfoResponse;
import org.finos.fdc3.schema.GetInfoResponsePayload;
import org.finos.fdc3.schema.GetInfoResponseType;
import org.finos.fdc3.schema.WebSocketConnectionProtocol3Goodbye;
import org.glassfish.tyrus.server.Server;

/**
 * Mock WebSocket server for testing GetAgent WSCP handshake.
 */
@ServerEndpoint("/fdc3")
public class MockWebSocketServer {

    private static final SchemaConverter converter = new SchemaConverter();

    private static final CopyOnWriteArrayList<Session> sessions = new CopyOnWriteArrayList<>();
    private static volatile MockWebSocketServer currentInstance;

    private Server server;
    private int port;

    private String acceptedSessionId;
    private String acceptedSharedSecret;
    private String responseAppId;
    private String responseInstanceId;
    private String responseInstanceUuid = "response-uuid";
    private String rejectMessage;
    private boolean shouldTimeout;
    private String providerName = "test-provider";
    private String fdc3Version = "2.0";

    private WebSocketConnectionProtocol1ConnectRequest lastWSCP1;
    private WebSocketConnectionProtocol3Goodbye lastWSCP3;

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

    public void acceptPairing(String sessionId, String sharedSecret, String appId, String instanceId) {
        acceptedSessionId = sessionId;
        acceptedSharedSecret = sharedSecret;
        responseAppId = appId;
        responseInstanceId = instanceId;
        rejectMessage = null;
        shouldTimeout = false;
    }

    public void rejectPairing(String message) {
        acceptedSessionId = null;
        acceptedSharedSecret = null;
        rejectMessage = message;
        shouldTimeout = false;
    }

    public void timeoutPairing() {
        shouldTimeout = true;
    }

    public void setImplementationMetadata(String provider, String version) {
        providerName = provider;
        fdc3Version = version;
    }

    public WebSocketConnectionProtocol1ConnectRequest getLastWSCP1() {
        return lastWSCP1;
    }

    public WebSocketConnectionProtocol3Goodbye getLastWSCP3() {
        return lastWSCP3;
    }

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
            String type = extractType(message);
            MockWebSocketServer instance = currentInstance;
            if (instance == null) {
                return;
            }

            if ("WSCP1ConnectRequest".equals(type)) {
                WebSocketConnectionProtocol1ConnectRequest wscp1 =
                        converter.fromJson(message, WebSocketConnectionProtocol1ConnectRequest.class);
                instance.lastWSCP1 = wscp1;
                instance.handleConnectRequest(wscp1, session);
            } else if ("WSCP3Goodbye".equals(type)) {
                WebSocketConnectionProtocol3Goodbye wscp3 =
                        converter.fromJson(message, WebSocketConnectionProtocol3Goodbye.class);
                instance.lastWSCP3 = wscp3;
            } else if ("getInfoRequest".equals(type)) {
                GetInfoRequest request = converter.fromJson(message, GetInfoRequest.class);
                instance.handleGetInfoRequest(request, session);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractType(String json) throws IOException {
        return converter.getObjectMapper().readTree(json).path("type").asText(null);
    }

    private void handleConnectRequest(
            WebSocketConnectionProtocol1ConnectRequest request, Session session) {
        if (shouldTimeout) {
            return;
        }

        String connectionAttemptUuid = request.getMeta().getConnectionAttemptUUID();
        String sessionId = request.getPayload().getSessionID();
        String sharedSecret = request.getPayload().getSharedSecret();
        String instanceUuid = request.getPayload().getInstanceUUID();

        try {
            String responseJson;

            if (rejectMessage != null) {
                responseJson = buildFailedResponse(connectionAttemptUuid, rejectMessage);
            } else if (acceptedSessionId != null
                    && acceptedSessionId.equals(sessionId)
                    && instanceUuid != null
                    && instanceUuid.equals(responseInstanceUuid)) {
                // Flow 2: reconnect with sessionId + instanceUuid (no sharedSecret)
                responseJson = buildSuccessResponse(connectionAttemptUuid);
            } else if (acceptedSessionId != null
                    && acceptedSessionId.equals(sessionId)
                    && acceptedSharedSecret != null
                    && acceptedSharedSecret.equals(sharedSecret)) {
                if (instanceUuid != null) {
                    responseInstanceUuid = instanceUuid;
                }
                responseJson = buildSuccessResponse(connectionAttemptUuid);
            } else {
                responseJson = buildFailedResponse(connectionAttemptUuid, "Invalid pairing credentials");
            }

            session.getBasicRemote().sendText(responseJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGetInfoRequest(GetInfoRequest request, Session session) throws IOException {
        GetInfoResponse response = new GetInfoResponse();
        response.setType(GetInfoResponseType.GET_INFO_RESPONSE);

        AddContextListenerResponseMeta meta = new AddContextListenerResponseMeta();
        meta.setRequestUUID(request.getMeta().getRequestUUID());
        meta.setResponseUUID(UUID.randomUUID().toString());
        meta.setTimestamp(OffsetDateTime.now());
        meta.setSource(request.getMeta().getSource());
        response.setMeta(meta);

        GetInfoResponsePayload payload = new GetInfoResponsePayload();
        payload.setImplementationMetadata(buildImplementationMetadata());
        response.setPayload(payload);

        session.getBasicRemote().sendText(converter.toJson(response));
    }

    private ImplementationMetadata buildImplementationMetadata() {
        ImplementationMetadata implMeta = new ImplementationMetadata();
        implMeta.setFdc3Version(fdc3Version);
        implMeta.setProvider(providerName);
        implMeta.setProviderVersion("1.0.0");

        AppMetadata appMeta = new AppMetadata();
        appMeta.setAppId(responseAppId);
        appMeta.setInstanceId(responseInstanceId);
        implMeta.setAppMetadata(appMeta);

        ImplementationMetadata.OptionalFeatures optFeatures = new ImplementationMetadata.OptionalFeatures();
        optFeatures.setOriginatingAppMetadata(true);
        optFeatures.setUserChannelMembershipAPIs(true);
        optFeatures.setDesktopAgentBridging(false);
        implMeta.setOptionalFeatures(optFeatures);

        return implMeta;
    }

    private String buildSuccessResponse(String connectionAttemptUuid) throws IOException {
        WebSocketConnectionProtocol2ConnectSuccessResponse response =
                new WebSocketConnectionProtocol2ConnectSuccessResponse();

        WebSocketConnectionProtocol1ConnectRequestMeta meta =
                new WebSocketConnectionProtocol1ConnectRequestMeta();
        meta.setConnectionAttemptUUID(connectionAttemptUuid);
        meta.setTimestamp(OffsetDateTime.now());
        response.setMeta(meta);

        response.setType(
                WebSocketConnectionProtocol2ConnectSuccessResponseType.WSCP2_CONNECT_RESPONSE);

        WebSocketConnectionProtocol2ConnectSuccessResponsePayload payload =
                new WebSocketConnectionProtocol2ConnectSuccessResponsePayload();
        payload.setAppID(responseAppId);
        payload.setInstanceID(responseInstanceId);
        payload.setInstanceUUID(responseInstanceUuid);
        payload.setImplementationMetadata(buildImplementationMetadata());
        response.setPayload(payload);

        return converter.toJson(response);
    }

    private String buildFailedResponse(String connectionAttemptUuid, String message) throws IOException {
        WebSocketConnectionProtocol2ConnectFailedResponse response =
                new WebSocketConnectionProtocol2ConnectFailedResponse();

        WebSocketConnectionProtocol1ConnectRequestMeta meta =
                new WebSocketConnectionProtocol1ConnectRequestMeta();
        meta.setConnectionAttemptUUID(connectionAttemptUuid);
        meta.setTimestamp(OffsetDateTime.now());
        response.setMeta(meta);

        response.setType(
                WebSocketConnectionProtocol2ConnectFailedResponseType.WSCP2_CONNECT_FAILED_RESPONSE);

        WebSocketConnectionProtocol2ConnectFailedResponsePayload payload =
                new WebSocketConnectionProtocol2ConnectFailedResponsePayload();
        payload.setMessage(message);
        response.setPayload(payload);

        return converter.toJson(response);
    }
}
