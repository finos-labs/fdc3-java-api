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

package org.finos.fdc3.workbench;

import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.finos.fdc3.workbench.connection.ConnectionService;
import org.finos.fdc3.workbench.connection.ProtocolLaunchParams;
import org.finos.fdc3.workbench.service.WorkbenchServices;
import org.finos.fdc3.workbench.ui.tabs.AppChannelsTab;
import org.finos.fdc3.workbench.ui.tabs.ContextsTab;
import org.finos.fdc3.workbench.ui.tabs.IntentsTab;
import org.finos.fdc3.workbench.ui.tabs.UserChannelsTab;
import org.finos.fdc3.workbench.ui.workbench.ListenersPane;
import org.finos.fdc3.workbench.ui.workbench.SystemLogPane;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * FDC3 Java Workbench — JavaFX desktop port of toolbox/fdc3-workbench (FDC3 3.0).
 */
public class WorkbenchApp extends Application {

    private static final AtomicReference<String> PENDING_OPEN_URI = new AtomicReference<>();
    private static final AtomicReference<WorkbenchApp> ACTIVE = new AtomicReference<>();
    private static String[] launchArgs = new String[0];

    private WorkbenchServices services;
    private Label statusLabel;
    private Button connectButton;
    private Button disconnectButton;
    private Button reconnectButton;
    private TabPane leftTabs;

    public static void main(String[] args) {
        launchArgs = args != null ? args : new String[0];
        registerOpenUriHandler();
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ACTIVE.set(this);
        services = new WorkbenchServices();
        services.getConnection().setOnConnected(agent -> {
            refreshAfterConnect();
            setWorkbenchEnabled(true);
        });
        services.getConnection().setOnDisconnected(v -> {
            services.getListeners().clearAll();
            services.getChannels().clear();
            setWorkbenchEnabled(false);
        });

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(buildTop());
        root.setCenter(buildBody());

        Scene scene = new Scene(root, 1280, 840);
        var css = getClass().getResource("/workbench.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setTitle("FDC3 Java Workbench");
        stage.setScene(scene);
        stage.show();

        Platform.runLater(this::initializeConnection);
    }

    private VBox buildTop() {
        VBox top = new VBox();
        top.getChildren().addAll(buildHeader(), buildConnectionBar());
        return top;
    }

    private HBox buildHeader() {
        Label title = new Label("FDC3 Java Workbench");
        title.getStyleClass().add("title-label");
        Label subtitle = new Label("FDC3 3.0 Desktop Agent testing harness");
        subtitle.getStyleClass().add("subtitle-label");

        Label infoLabel = new Label();
        infoLabel.getStyleClass().add("subtitle-label");
        services.getAgentHolder().infoProperty().addListener((obs, o, n) -> {
            if (n == null) {
                infoLabel.setText("");
            } else {
                String provider = n.getProvider() != null ? n.getProvider() : "?";
                String version = n.getFdc3Version() != null ? n.getFdc3Version() : "?";
                String appId = n.getAppMetadata() != null ? n.getAppMetadata().getAppId() : "?";
                infoLabel.setText(provider + " | FDC3 " + version + " | appId=" + appId);
            }
        });

        Button docs = new Button("API Docs");
        docs.setOnAction(e -> openUrl("https://fdc3.finos.org/docs/api/overview"));

        HBox left = new HBox(12, new VBox(2, title, subtitle), infoLabel);
        HBox box = new HBox(16, left, docs);
        HBox.setHgrow(left, Priority.ALWAYS);
        box.getStyleClass().add("header-bar");
        return box;
    }

    private HBox buildConnectionBar() {
        statusLabel = new Label();
        statusLabel.textProperty().bind(services.getAgentHolder().statusTextProperty());
        services.getAgentHolder().statusStyleProperty().addListener((obs, o, n) -> {
            statusLabel.getStyleClass().setAll(n != null ? n : "status-disconnected");
        });
        statusLabel.getStyleClass().add("status-disconnected");

        connectButton = new Button("Connect");
        connectButton.setOnAction(e -> promptAndConnect());
        disconnectButton = new Button("Disconnect");
        disconnectButton.setDisable(true);
        disconnectButton.setOnAction(e -> services.getConnection().disconnect());
        reconnectButton = new Button("Reconnect");
        reconnectButton.setDisable(true);
        reconnectButton.setOnAction(e -> services.getConnection().reconnect());

        services.getAgentHolder().agentProperty().addListener((obs, o, n) -> {
            boolean connected = n != null;
            disconnectButton.setDisable(!connected);
            reconnectButton.setDisable(connected || !services.getConnection().hasCredentials());
            connectButton.setDisable(connected || services.getConnection().connectingProperty().get());
        });
        services.getConnection().connectingProperty().addListener((obs, o, n) ->
                connectButton.setDisable(Boolean.TRUE.equals(n) || services.getAgentHolder().isConnected()));

        HBox bar = new HBox(8, new Label("Status:"), statusLabel, connectButton, disconnectButton, reconnectButton);
        bar.getStyleClass().add("connection-bar");
        return bar;
    }

    private SplitPane buildBody() {
        leftTabs = new TabPane();
        leftTabs.getTabs().addAll(
                tab("Contexts", new ContextsTab(services)),
                tab("Intents", new IntentsTab(services)),
                tab("User Channels", new UserChannelsTab(services)),
                tab("App Channels", new AppChannelsTab(services)));

        TabPane rightTabs = new TabPane();
        rightTabs.getTabs().addAll(
                tab("Listeners", new ListenersPane(services)),
                tab("System Log", new SystemLogPane(services)));

        VBox left = card(leftTabs);
        VBox right = card(rightTabs);
        SplitPane split = new SplitPane(left, right);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.62);
        SplitPane.setResizableWithParent(left, true);
        SplitPane.setResizableWithParent(right, true);
        setWorkbenchEnabled(false);
        return split;
    }

    private static Tab tab(String title, javafx.scene.Node content) {
        Tab t = new Tab(title, content);
        t.setClosable(false);
        return t;
    }

    private static VBox card(javafx.scene.Node content) {
        VBox box = new VBox(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        box.getStyleClass().add("panel-card");
        box.setPadding(new Insets(8));
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private void setWorkbenchEnabled(boolean enabled) {
        if (leftTabs != null) {
            leftTabs.setDisable(!enabled);
        }
    }

    private void initializeConnection() {
        ConnectionService connection = services.getConnection();
        for (String arg : launchArgs) {
            if (arg != null && arg.regionMatches(true, 0, ProtocolLaunchParams.SCHEME + "://", 0,
                    ProtocolLaunchParams.SCHEME.length() + 3)) {
                services.getLog().info("connection",
                        "Protocol launch URL: " + ProtocolLaunchParams.redactLaunchUri(arg));
            }
        }
        Optional<ProtocolLaunchParams> fromArgs = ProtocolLaunchParams.fromArgs(launchArgs);
        if (fromArgs.isPresent()) {
            connection.applyProtocolLaunch(fromArgs.get(), "command-line launch URL");
            return;
        }
        String pending = connection.takePendingOpenUri();
        if (pending == null) {
            pending = PENDING_OPEN_URI.getAndSet(null);
        }
        if (pending != null) {
            connection.applyProtocolUri(pending, "desktop open URI");
            return;
        }
        String url = ConnectionService.envOrProperty("FDC3_WEBSOCKET_URL");
        String secret = ConnectionService.envOrProperty("FDC3_CONNECTION_SECRET");
        if (url != null && secret != null) {
            connection.setCredentials(url, secret);
            services.getLog().info("connection", "Using WSCP connection config from environment");
            connection.connect();
            return;
        }
        services.getLog().info("connection",
                "No WSCP credentials at startup. Click Connect, or launch from Sail via protocol URL.");
    }

    private void promptAndConnect() {
        TextInputDialog urlDialog = new TextInputDialog(
                services.getConnection().getWebSocketUrl() != null
                        ? services.getConnection().getWebSocketUrl()
                        : "ws://localhost:8090/fdc3/ws");
        urlDialog.setTitle("WSCP WebSocket URL");
        urlDialog.setHeaderText("Enter Desktop Agent WebSocket URL");
        urlDialog.setContentText("WebSocket URL:");
        Optional<String> url = urlDialog.showAndWait();
        if (url.isEmpty() || url.get().isBlank()) {
            return;
        }
        TextInputDialog secretDialog = new TextInputDialog();
        secretDialog.setTitle("WSCP Shared Secret");
        secretDialog.setHeaderText("Enter pairing shared secret from the Desktop Agent");
        secretDialog.setContentText("Shared secret:");
        Optional<String> secret = secretDialog.showAndWait();
        if (secret.isEmpty() || secret.get().isBlank()) {
            return;
        }
        services.getConnection().setCredentials(url.get().trim(), secret.get().trim());
        services.getLog().info("connection", "Using user-provided WSCP connection config");
        services.getConnection().connect();
    }

    private void refreshAfterConnect() {
        reconnectButton.setDisable(true);
        services.getAgentHolder().getAgent().ifPresent(agent ->
                agent.getUserChannels().thenAccept(channels -> Platform.runLater(() -> {
                    services.getChannels().getUserChannels().setAll(channels);
                    services.getLog().info("userChannels", "Loaded " + channels.size() + " user channels");
                })).exceptionally(err -> {
                    services.getLog().error("userChannels", err.getMessage());
                    return null;
                }));
        services.getAgentHolder().getAgent().ifPresent(agent ->
                agent.getCurrentChannel().thenAccept(opt -> Platform.runLater(() ->
                        services.getChannels().setCurrentUserChannel(opt.orElse(null)))));
    }

    private void handleOpenUri(String uri) {
        services.getLog().info("connection", "Received open URI: " + ProtocolLaunchParams.redactLaunchUri(uri));
        services.getConnection().applyProtocolUri(uri, "desktop open URI");
    }

    private static void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, url);
            alert.setHeaderText("Open in browser");
            alert.showAndWait();
        }
    }

    private static boolean supportsOpenUriHandler() {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        return Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_URI);
    }

    private static void registerOpenUriHandler() {
        if (!supportsOpenUriHandler()) {
            return;
        }
        Desktop.getDesktop().setOpenURIHandler(event -> {
            String uri = event.getURI().toString();
            PENDING_OPEN_URI.set(uri);
            WorkbenchApp instance = ACTIVE.get();
            if (instance != null) {
                Platform.runLater(() -> instance.handleOpenUri(uri));
            }
        });
    }
}
