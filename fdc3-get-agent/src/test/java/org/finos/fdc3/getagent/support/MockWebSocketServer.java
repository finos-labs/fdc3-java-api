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
import org.finos.fdc3.schema.AddContextListenerResponseMeta;
import org.finos.fdc3.schema.GetInfoRequest;
import org.finos.fdc3.schema.GetInfoResponse;
import org.finos.fdc3.schema.GetInfoResponsePayload;
import org.finos.fdc3.schema.GetInfoResponseType;
import org.finos.fdc3.schema.ProtocolVersion;
import org.finos.fdc3.schema.SchemaConverter;
import org.finos.fdc3.schema.WebSocketConnectionProtocolApplicationConnect;
import org.finos.fdc3.schema.WebSocketConnectionProtocolApplicationConnectMeta;
import org.finos.fdc3.schema.WebSocketConnectionProtocolConnectFailed;
import org.finos.fdc3.schema.WebSocketConnectionProtocolConnectFailedPayload;
import org.finos.fdc3.schema.WebSocketConnectionProtocolConnectFailedType;
import org.finos.fdc3.schema.WebSocketConnectionProtocolDesktopAgentConnect;
import org.finos.fdc3.schema.WebSocketConnectionProtocolDesktopAgentConnectPayload;
import org.finos.fdc3.schema.WebSocketConnectionProtocolDesktopAgentConnectType;
import org.finos.fdc3.schema.WebSocketConnectionProtocolGoodbye;
import org.glassfish.tyrus.server.Server;

/**
 * Mock WebSocket server for testing GetAgent WSCP handshake.
 */
@ServerEndpoint("/fdc3/ws")
public class MockWebSocketServer {

    private static final SchemaConverter converter = new SchemaConverter();

    private static final CopyOnWriteArrayList<Session> sessions = new CopyOnWriteArrayList<>();
    private static volatile MockWebSocketServer currentInstance;

    private Server server;
    private int port;

    private String acceptedSharedSecret;
    private String responseAppId;
    private String responseInstanceId;
    private String rejectMessage;
    private boolean shouldTimeout;
    private String providerName = "test-provider";
    private String fdc3Version = "2.0";

    private WebSocketConnectionProtocolApplicationConnect lastApplicationConnect;
    private WebSocketConnectionProtocolGoodbye lastGoodbye;

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
        return "ws://localhost:" + port + "/fdc3/ws";
    }

    public void acceptPairing(String sharedSecret, String appId, String instanceId) {
        acceptedSharedSecret = sharedSecret;
        responseAppId = appId;
        responseInstanceId = instanceId;
        rejectMessage = null;
        shouldTimeout = false;
    }

    public void rejectPairing(String message) {
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

    public WebSocketConnectionProtocolApplicationConnect getLastApplicationConnect() {
        return lastApplicationConnect;
    }

    public WebSocketConnectionProtocolGoodbye getLastGoodbye() {
        return lastGoodbye;
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

            if ("WSCPApplicationConnect".equals(type)) {
                WebSocketConnectionProtocolApplicationConnect connect =
                        converter.fromJson(message, WebSocketConnectionProtocolApplicationConnect.class);
                instance.lastApplicationConnect = connect;
                instance.handleConnectRequest(connect, session);
            } else if ("WSCPGoodbye".equals(type)) {
                WebSocketConnectionProtocolGoodbye goodbye =
                        converter.fromJson(message, WebSocketConnectionProtocolGoodbye.class);
                instance.lastGoodbye = goodbye;
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
            WebSocketConnectionProtocolApplicationConnect request, Session session) {
        if (shouldTimeout) {
            return;
        }

        String connectionAttemptUuid = request.getMeta().getConnectionAttemptUUID();
        String sharedSecret = request.getPayload().getSharedSecret();

        try {
            String responseJson;

            if (rejectMessage != null) {
                responseJson = buildFailedResponse(connectionAttemptUuid, rejectMessage);
            } else if (acceptedSharedSecret != null
                    && acceptedSharedSecret.equals(sharedSecret)) {
                responseJson = buildSuccessResponse(connectionAttemptUuid);
            } else {
                responseJson = buildFailedResponse(connectionAttemptUuid, "Invalid or unknown sharedSecret");
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
        WebSocketConnectionProtocolDesktopAgentConnect response =
                new WebSocketConnectionProtocolDesktopAgentConnect();

        WebSocketConnectionProtocolApplicationConnectMeta meta =
                new WebSocketConnectionProtocolApplicationConnectMeta();
        meta.setConnectionAttemptUUID(connectionAttemptUuid);
        meta.setTimestamp(OffsetDateTime.now());
        response.setMeta(meta);

        response.setType(
                WebSocketConnectionProtocolDesktopAgentConnectType.WSCP_DESKTOP_AGENT_CONNECT);

        WebSocketConnectionProtocolDesktopAgentConnectPayload payload =
                new WebSocketConnectionProtocolDesktopAgentConnectPayload();
        payload.setProtocolVersion(ProtocolVersion.THE_10);
        payload.setImplementationMetadata(buildImplementationMetadata());
        response.setPayload(payload);

        return converter.toJson(response);
    }

    private String buildFailedResponse(String connectionAttemptUuid, String message) throws IOException {
        WebSocketConnectionProtocolConnectFailed response =
                new WebSocketConnectionProtocolConnectFailed();

        WebSocketConnectionProtocolApplicationConnectMeta meta =
                new WebSocketConnectionProtocolApplicationConnectMeta();
        meta.setConnectionAttemptUUID(connectionAttemptUuid);
        meta.setTimestamp(OffsetDateTime.now());
        response.setMeta(meta);

        response.setType(WebSocketConnectionProtocolConnectFailedType.WSCP_CONNECT_FAILED);

        WebSocketConnectionProtocolConnectFailedPayload payload =
                new WebSocketConnectionProtocolConnectFailedPayload();
        payload.setMessage(message);
        response.setPayload(payload);

        return converter.toJson(response);
    }
}
