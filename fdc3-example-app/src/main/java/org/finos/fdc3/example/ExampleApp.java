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

package org.finos.fdc3.example;

import org.finos.fdc3.api.DesktopAgent;
import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.getagent.GetAgent;
import org.finos.fdc3.getagent.GetAgentParams;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Example Java Swing application demonstrating FDC3 Desktop Agent connectivity.
 * <p>
 * This application:
 * <ul>
 *   <li>Connects to a Desktop Agent via WebSocket when launched with credentials or on user request</li>
 *   <li>Displays the current user channel and allows changing channels</li>
 *   <li>Shows a log of context broadcasts received on the current channel</li>
 *   <li>Supports adding and removing context listeners</li>
 * </ul>
 * <p>
 * Required system properties or environment variables:
 * <ul>
 *   <li>{@code FDC3_WEBSOCKET_URL} - WebSocket URL (e.g. ws://host/fdc3/ws)</li>
 *   <li>{@code FDC3_CONNECTION_SECRET} - Per-instance shared secret from Sail pairing UI</li>
 * </ul>
 */
public class ExampleApp extends JFrame {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    /** macOS delivers custom protocol URLs via Desktop OpenURIHandler, not main() args. */
    private static final AtomicReference<String> pendingOpenUri = new AtomicReference<>();
    private static volatile ExampleApp activeInstance;
    
    private DesktopAgent agent;
    private Listener contextListener;
    private List<Channel> userChannels;
    private Channel currentChannel;
    
    // UI Components
    private JComboBox<ChannelItem> channelComboBox;
    private JTextArea logArea;
    private JButton addListenerButton;
    private JButton removeListenerButton;
    private JLabel statusLabel;
    private JLabel listenerStatusLabel;
    
    // Broadcast buttons
    private JButton broadcastInstrumentButton;
    private JButton broadcastTeslaButton;
    private JButton broadcastCurrencyButton;
    private JButton broadcastContactButton;
    
    private String websocketUrl;
    private String sharedSecret;

    private final String[] launchArgs;
    
    // Stored connection info for reconnection
    private String lastInstanceId;
    
    // Connection buttons
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton reconnectButton;

    public ExampleApp(String[] launchArgs) {
        super("FDC3 Example App (" + buildLabel() + ")");
        this.launchArgs = launchArgs != null ? launchArgs : new String[0];
        initUI();
        
        SwingUtilities.invokeLater(this::initializeConnection);
    }

    /**
     * Connect automatically when launch args, a pending protocol URL, or environment
     * variables supply WSCP credentials; otherwise wait for Connect or OpenURI.
     */
    private void initializeConnection() {
        logLaunchArguments();

        if (tryConnectFromLaunchArgs()) {
            return;
        }
        if (tryConnectFromPendingOpenUri()) {
            return;
        }
        if (tryConnectFromEnvironment()) {
            return;
        }
        showAwaitingConnection();
    }

    private boolean tryConnectFromLaunchArgs() {
        for (String arg : launchArgs) {
            if (arg != null
                    && arg.regionMatches(true, 0, ProtocolLaunchParams.SCHEME + "://", 0,
                    ProtocolLaunchParams.SCHEME.length() + 3)) {
                log("Protocol launch URL: " + arg);
            }
        }

        Optional<ProtocolLaunchParams> protocolLaunch = ProtocolLaunchParams.fromArgs(launchArgs);
        if (protocolLaunch.isPresent()) {
            applyProtocolLaunchParams(protocolLaunch.get(), "command-line launch URL");
            return true;
        }

        if (launchArgs.length > 0) {
            log("No WSCP parameters parsed from launch arguments");
        }
        return false;
    }

    private boolean tryConnectFromPendingOpenUri() {
        String uri = pendingOpenUri.getAndSet(null);
        if (uri == null) {
            return false;
        }
        return applyProtocolLaunchUri(uri, "desktop open URI");
    }

    private boolean tryConnectFromEnvironment() {
        websocketUrl = envOrProperty("FDC3_WEBSOCKET_URL");
        sharedSecret = envOrProperty("FDC3_CONNECTION_SECRET");

        if (websocketUrl == null || sharedSecret == null) {
            return false;
        }
        log("Using WSCP connection config from environment");
        connectToAgent();
        return true;
    }

    private void showAwaitingConnection() {
        statusLabel.setText("Not Connected");
        statusLabel.setForeground(Color.GRAY);
        connectButton.setEnabled(true);
        if (supportsOpenUriHandler()) {
            log("No WSCP credentials at startup. Click Connect, or launch from Sail to connect via protocol URL.");
        } else {
            log("No WSCP credentials at startup. Click Connect to enter WebSocket URL and shared secret.");
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
            pendingOpenUri.set(uri);
            ExampleApp instance = activeInstance;
            if (instance != null) {
                instance.log("Received open URI: " + uri);
                SwingUtilities.invokeLater(() -> instance.handleOpenUri(uri));
            }
        });
    }

    private void handleOpenUri(String uri) {
        pendingOpenUri.compareAndSet(uri, null);
        applyProtocolLaunchUri(uri, "desktop open URI");
    }

    private boolean applyProtocolLaunchUri(String uri, String source) {
        Optional<ProtocolLaunchParams> protocolLaunch = ProtocolLaunchParams.parseLaunchUri(uri);
        if (protocolLaunch.isEmpty()) {
            log("No WSCP parameters parsed from " + source + ": " + uri);
            return false;
        }
        applyProtocolLaunchParams(protocolLaunch.get(), source);
        return true;
    }

    private void applyProtocolLaunchParams(ProtocolLaunchParams params, String source) {
        websocketUrl = params.getWebSocketUrl();
        sharedSecret = params.getSharedSecret();
        log("Parsed WSCP webSocketUrl: " + websocketUrl);
        log("Using WSCP connection config from " + source);
        if (agent != null) {
            disconnect();
        }
        connectToAgent();
    }

    private void logLaunchArguments() {
        log("Build: " + buildLabel());
        log("URI Handler: " + supportsOpenUriHandler());
        log("Running from: " + codeLocation());
        if (launchArgs.length == 0) {
            log("Started with no launch arguments");
            return;
        }
        log("Launch arguments (" + launchArgs.length + "):");
        for (int i = 0; i < launchArgs.length; i++) {
            log("  [" + i + "] " + launchArgs[i]);
        }
    }

    private static String buildLabel() {
        Package pkg = ExampleApp.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        return "dev";
    }

    private static String codeLocation() {
        try {
            if (ExampleApp.class.getProtectionDomain().getCodeSource() != null) {
                return ExampleApp.class.getProtectionDomain().getCodeSource().getLocation().toString();
            }
        } catch (SecurityException ignored) {
            // fall through
        }
        return "unknown";
    }

    private static String envOrProperty(String name) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) {
            v = System.getProperty(name);
        }
        return (v == null || v.isEmpty()) ? null : v;
    }

    private void promptForConnectionConfig() {
        String message = "WSCP connection values are required.\n\n" +
                "Copy these from the Sail app directory for your native app:\n" +
                "  • WebSocket URL (e.g. ws://localhost:8090/fdc3/ws)\n" +
                "  • Shared secret (unique per app instance)\n\n" +
                "Enter WebSocket URL:";

        String url = JOptionPane.showInputDialog(this, message, "WSCP WebSocket URL",
                JOptionPane.QUESTION_MESSAGE);
        if (url == null || url.trim().isEmpty()) {
            showConfigError("No WebSocket URL provided.");
            return;
        }
        websocketUrl = url.trim();

        String secret = JOptionPane.showInputDialog(this, "Enter shared secret:", "WSCP Shared Secret",
                JOptionPane.QUESTION_MESSAGE);
        if (secret == null || secret.trim().isEmpty()) {
            showConfigError("No shared secret provided.");
            return;
        }
        sharedSecret = secret.trim();

        log("Using user-provided WSCP connection config");
        connectToAgent();
    }

    private void showConfigError(String msg) {
        statusLabel.setText("Not Connected");
        statusLabel.setForeground(Color.RED);
        connectButton.setEnabled(true);
        log(msg);
        log("Click Connect, set FDC3_WEBSOCKET_URL and FDC3_CONNECTION_SECRET, or launch from Sail.");
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 600);
        setLocationRelativeTo(null);

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top panel - Connection status and channel selection
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Status:"));
        statusLabel = new JLabel("Not Connected");
        statusLabel.setForeground(Color.GRAY);
        statusPanel.add(statusLabel);

        connectButton = new JButton("Connect");
        connectButton.setToolTipText("Enter WSCP WebSocket URL and shared secret");
        connectButton.addActionListener(e -> promptForConnectionConfig());
        statusPanel.add(connectButton);
        
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnect());
        statusPanel.add(disconnectButton);
        
        reconnectButton = new JButton("Reconnect");
        reconnectButton.setEnabled(false);
        reconnectButton.setToolTipText("Reconnect using the same shared secret");
        reconnectButton.addActionListener(e -> reconnect());
        statusPanel.add(reconnectButton);
        
        topPanel.add(statusPanel, BorderLayout.NORTH);

        // Channel selection panel
        JPanel channelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        channelPanel.setBorder(new TitledBorder("User Channel"));
        channelPanel.add(new JLabel("Current Channel:"));
        channelComboBox = new JComboBox<>();
        channelComboBox.setPreferredSize(new Dimension(200, 25));
        channelComboBox.setEnabled(false);
        channelComboBox.addActionListener(e -> onChannelSelected());
        channelPanel.add(channelComboBox);
        
        JButton leaveButton = new JButton("Leave Channel");
        leaveButton.addActionListener(e -> leaveChannel());
        channelPanel.add(leaveButton);
        
        topPanel.add(channelPanel, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center panel - Log area
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("Context Log"));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(550, 250));
        logPanel.add(scrollPane, BorderLayout.CENTER);
        
        JButton clearButton = new JButton("Clear Log");
        clearButton.addActionListener(e -> logArea.setText(""));
        JPanel clearPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        clearPanel.add(clearButton);
        logPanel.add(clearPanel, BorderLayout.SOUTH);

        mainPanel.add(logPanel, BorderLayout.CENTER);

        // Bottom panel - contains listener controls and broadcast controls
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        // Listener controls
        JPanel listenerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        listenerPanel.setBorder(new TitledBorder("Context Listener"));
        
        listenerStatusLabel = new JLabel("Listener: Not Active");
        listenerStatusLabel.setForeground(Color.GRAY);
        listenerPanel.add(listenerStatusLabel);
        
        addListenerButton = new JButton("Add Listener");
        addListenerButton.setEnabled(false);
        addListenerButton.addActionListener(e -> addContextListener());
        listenerPanel.add(addListenerButton);
        
        removeListenerButton = new JButton("Remove Listener");
        removeListenerButton.setEnabled(false);
        removeListenerButton.addActionListener(e -> removeContextListener());
        listenerPanel.add(removeListenerButton);
        
        bottomPanel.add(listenerPanel);
        
        // Broadcast controls
        JPanel broadcastPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        broadcastPanel.setBorder(new TitledBorder("Broadcast Context (requires channel)"));
        
        broadcastInstrumentButton = new JButton("Instrument (MSFT)");
        broadcastInstrumentButton.setEnabled(false);
        broadcastInstrumentButton.setToolTipText("Broadcast Microsoft stock instrument context");
        broadcastInstrumentButton.addActionListener(e -> broadcastInstrument());
        broadcastPanel.add(broadcastInstrumentButton);

        broadcastTeslaButton = new JButton("Instrument (TSLA)");
        broadcastTeslaButton.setEnabled(false);
        broadcastTeslaButton.setToolTipText("Broadcast Tesla stock instrument context");
        broadcastTeslaButton.addActionListener(e -> broadcastTesla());
        broadcastPanel.add(broadcastTeslaButton);
        
        broadcastCurrencyButton = new JButton("Currency (USD)");
        broadcastCurrencyButton.setEnabled(false);
        broadcastCurrencyButton.setToolTipText("Broadcast US Dollar currency context");
        broadcastCurrencyButton.addActionListener(e -> broadcastCurrency());
        broadcastPanel.add(broadcastCurrencyButton);
        
        broadcastContactButton = new JButton("Contact (Jane Doe)");
        broadcastContactButton.setEnabled(false);
        broadcastContactButton.setToolTipText("Broadcast sample contact context");
        broadcastContactButton.addActionListener(e -> broadcastContact());
        broadcastPanel.add(broadcastContactButton);
        
        bottomPanel.add(broadcastPanel);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Handle window close - cleanup
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    private void connectToAgent() {
        if (websocketUrl == null || sharedSecret == null) {
            log("ERROR: WSCP connection config incomplete");
            return;
        }

        log("Connecting to Desktop Agent at: " + websocketUrl);

        try {
            GetAgentParams params = GetAgentParams.builder()
                    .timeoutMs(30000)
                    .webSocketUrl(websocketUrl)
                    .sharedSecret(sharedSecret)
                    .build();

            GetAgent.getAgent(params)
                    .thenAccept(this::onAgentConnected)
                    .exceptionally(error -> {
                        SwingUtilities.invokeLater(() -> onConnectionError(error));
                        return null;
                    });
        } catch (Exception e) {
            onConnectionError(e);
        }
    }

    private void onAgentConnected(DesktopAgent agent) {
        this.agent = agent;
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Connected");
            statusLabel.setForeground(new Color(0, 128, 0));
            disconnectButton.setEnabled(true);
            reconnectButton.setEnabled(false);
            log("Successfully connected to Desktop Agent");
            
            // Store connection info for potential reconnection
            storeConnectionInfo();
            
            // Load user channels
            loadUserChannels();
        });
    }
    
    /**
     * Store instanceId for display after connect.
     */
    private void storeConnectionInfo() {
        if (agent == null) return;
        
        agent.getInfo()
                .thenAccept(info -> {
                    if (info != null && info.getAppMetadata() != null) {
                        lastInstanceId = info.getAppMetadata().getInstanceId();
                        
                        SwingUtilities.invokeLater(() -> {
                            if (lastInstanceId != null) {
                                log("Assigned instanceId: " + lastInstanceId);
                            }
                        });
                    }
                })
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() -> 
                        log("Warning: Could not retrieve connection info - " + error.getMessage()));
                    return null;
                });
    }

    private void onConnectionError(Throwable error) {
        statusLabel.setText("Connection Failed");
        statusLabel.setForeground(Color.RED);
        disconnectButton.setEnabled(false);
        // Enable reconnect if we have stored credentials
        reconnectButton.setEnabled(sharedSecret != null);
        log("ERROR: Failed to connect - " + error.getMessage());
        
        // Show error dialog with option to retry
        int result = JOptionPane.showOptionDialog(this,
                "Failed to connect to Desktop Agent:\n" + error.getMessage() +
                "\n\nURL: " + websocketUrl +
                "\n\nWould you like to enter a different URL?",
                "Connection Error",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                new String[]{"Enter New URL", "Close"},
                "Enter New URL");
        
        if (result == 0) {
            promptForConnectionConfig();
        }
    }
    
    /**
     * Disconnect from the Desktop Agent.
     */
    private void disconnect() {
        if (agent == null) return;
        
        log("Disconnecting from Desktop Agent...");
        
        // Clean up listener
        if (contextListener != null) {
            try {
                contextListener.unsubscribe().toCompletableFuture().join();
                contextListener = null;
            } catch (Exception e) {
                log("Warning: Error unsubscribing listener - " + e.getMessage());
            }
        }
        
        // Disconnect the agent proxy
        if (agent instanceof org.finos.fdc3.proxy.DesktopAgentProxy) {
            try {
                ((org.finos.fdc3.proxy.DesktopAgentProxy) agent).disconnect().toCompletableFuture().join();
            } catch (Exception e) {
                log("Warning: Error during disconnect - " + e.getMessage());
            }
        }
        
        agent = null;
        currentChannel = null;
        
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Disconnected");
            statusLabel.setForeground(Color.GRAY);
            disconnectButton.setEnabled(false);
            // Enable reconnect if we have stored credentials
            reconnectButton.setEnabled(sharedSecret != null);
            channelComboBox.setEnabled(false);
            channelComboBox.removeAllItems();
            addListenerButton.setEnabled(false);
            removeListenerButton.setEnabled(false);
            listenerStatusLabel.setText("Listener: Not Active");
            listenerStatusLabel.setForeground(Color.GRAY);
            updateBroadcastButtonsState();
            log("Disconnected from Desktop Agent");
        });
    }
    
    /**
     * Reconnect to the Desktop Agent using the same shared secret.
     */
    private void reconnect() {
        if (websocketUrl == null || sharedSecret == null) {
            log("ERROR: webSocketUrl and sharedSecret required for reconnection");
            return;
        }
        
        log("Reconnecting to Desktop Agent...");
        
        statusLabel.setText("Reconnecting...");
        statusLabel.setForeground(Color.ORANGE);
        reconnectButton.setEnabled(false);
        
        try {
            GetAgentParams params = GetAgentParams.builder()
                    .timeoutMs(30000)
                    .webSocketUrl(websocketUrl)
                    .sharedSecret(sharedSecret)
                    .build();

            GetAgent.getAgent(params)
                    .thenAccept(this::onAgentConnected)
                    .exceptionally(error -> {
                        SwingUtilities.invokeLater(() -> onConnectionError(error));
                        return null;
                    });
        } catch (Exception e) {
            onConnectionError(e);
        }
    }

    private void loadUserChannels() {
        if (agent == null) return;
        
        log("Loading user channels...");
        agent.getUserChannels()
                .thenAccept(channels -> {
                    this.userChannels = channels;
                    SwingUtilities.invokeLater(() -> {
                        channelComboBox.removeAllItems();
                        channelComboBox.addItem(new ChannelItem(null)); // "None" option
                        for (Channel channel : channels) {
                            channelComboBox.addItem(new ChannelItem(channel));
                        }
                        channelComboBox.setEnabled(true);
                        addListenerButton.setEnabled(true);
                        log("Loaded " + channels.size() + " user channels");
                        
                        // Check current channel
                        loadCurrentChannel();
                    });
                })
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() -> 
                        log("ERROR: Failed to load channels - " + error.getMessage()));
                    return null;
                });
    }

    private void loadCurrentChannel() {
        if (agent == null) return;
        
        agent.getCurrentChannel()
                .thenAccept(optChannel -> {
                    SwingUtilities.invokeLater(() -> {
                        if (optChannel.isPresent()) {
                            currentChannel = optChannel.get();
                            // Find and select the channel in the combo box
                            for (int i = 0; i < channelComboBox.getItemCount(); i++) {
                                ChannelItem item = channelComboBox.getItemAt(i);
                                if (item.channel != null && 
                                    item.channel.getId().equals(currentChannel.getId())) {
                                    channelComboBox.setSelectedIndex(i);
                                    break;
                                }
                            }
                            log("Current channel: " + currentChannel.getId());
                        } else {
                            currentChannel = null;
                            channelComboBox.setSelectedIndex(0);
                            log("Not currently joined to any channel");
                        }
                        updateBroadcastButtonsState();
                    });
                })
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() ->
                        log("ERROR: Failed to get current channel - " + error.getMessage()));
                    return null;
                });
    }

    private void onChannelSelected() {
        if (agent == null || !channelComboBox.isEnabled()) return;
        
        ChannelItem selected = (ChannelItem) channelComboBox.getSelectedItem();
        if (selected == null) return;
        
        if (selected.channel == null) {
            // "None" selected - leave current channel
            if (currentChannel != null) {
                leaveChannel();
            }
        } else if (currentChannel == null || 
                   !currentChannel.getId().equals(selected.channel.getId())) {
            // Join the selected channel
            joinChannel(selected.channel);
        }
    }

    private void joinChannel(Channel channel) {
        if (agent == null) return;
        
        log("Joining channel: " + channel.getId());
        agent.joinUserChannel(channel.getId())
                .thenRun(() -> {
                    currentChannel = channel;
                    SwingUtilities.invokeLater(() -> {
                        log("Joined channel: " + channel.getId());
                        updateBroadcastButtonsState();
                    });
                })
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() -> {
                        log("ERROR: Failed to join channel - " + error.getMessage());
                        loadCurrentChannel(); // Refresh to show actual state
                    });
                    return null;
                });
    }

    private void leaveChannel() {
        if (agent == null) return;
        
        log("Leaving current channel...");
        agent.leaveCurrentChannel()
                .thenRun(() -> {
                    currentChannel = null;
                    SwingUtilities.invokeLater(() -> {
                        channelComboBox.setSelectedIndex(0);
                        log("Left channel");
                        updateBroadcastButtonsState();
                    });
                })
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() ->
                        log("ERROR: Failed to leave channel - " + error.getMessage()));
                    return null;
                });
    }

    private void addContextListener() {
        if (agent == null || contextListener != null) return;
        
        log("Adding context listener (all types)...");
        agent.addContextListener(null, this::onContextReceived)
                .thenAccept(listener -> {
                    this.contextListener = listener;
                    SwingUtilities.invokeLater(() -> {
                        listenerStatusLabel.setText("Listener: Active");
                        listenerStatusLabel.setForeground(new Color(0, 128, 0));
                        addListenerButton.setEnabled(false);
                        removeListenerButton.setEnabled(true);
                        log("Context listener added successfully");
                    });
                })
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() ->
                        log("ERROR: Failed to add listener - " + error.getMessage()));
                    return null;
                });
    }

    private void removeContextListener() {
        if (contextListener == null) return;
        
        log("Removing context listener...");
        contextListener.unsubscribe()
                .thenRun(() -> {
                    contextListener = null;
                    SwingUtilities.invokeLater(() -> {
                        listenerStatusLabel.setText("Listener: Not Active");
                        listenerStatusLabel.setForeground(Color.GRAY);
                        addListenerButton.setEnabled(true);
                        removeListenerButton.setEnabled(false);
                        log("Context listener removed");
                    });
                })
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() ->
                        log("ERROR: Failed to remove listener - " + error.getMessage()));
                    return null;
                });
    }

    /**
     * Update the enabled state of broadcast buttons based on channel membership.
     */
    private void updateBroadcastButtonsState() {
        boolean canBroadcast = currentChannel != null && agent != null;
        broadcastInstrumentButton.setEnabled(canBroadcast);
        broadcastTeslaButton.setEnabled(canBroadcast);
        broadcastCurrencyButton.setEnabled(canBroadcast);
        broadcastContactButton.setEnabled(canBroadcast);
    }

    /**
     * Broadcast a sample instrument context (Microsoft stock).
     */
    private void broadcastInstrument() {
        if (agent == null || currentChannel == null) return;
        
        Map<String, Object> id = new HashMap<>();
        id.put("ticker", "MSFT");
        id.put("ISIN", "US5949181045");
        
        Context instrumentContext = new Context("fdc3.instrument", "Microsoft", id);
        
        log("Broadcasting instrument context: Microsoft (MSFT)");
        agent.broadcast(instrumentContext)
                .thenRun(() -> SwingUtilities.invokeLater(() -> 
                    log("Successfully broadcast instrument context")))
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() ->
                        log("ERROR: Failed to broadcast - " + error.getMessage()));
                    return null;
                });
    }

    /**
     * Broadcast a sample instrument context (Tesla stock).
     */
    private void broadcastTesla() {
        if (agent == null || currentChannel == null) return;

        Map<String, Object> id = new HashMap<>();
        id.put("ticker", "TSLA");
        id.put("ISIN", "US88160R1014");

        Context instrumentContext = new Context("fdc3.instrument", "Tesla", id);

        log("Broadcasting instrument context: Tesla (TSLA)");
        agent.broadcast(instrumentContext)
                .thenRun(() -> SwingUtilities.invokeLater(() ->
                    log("Successfully broadcast instrument context")))
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() ->
                        log("ERROR: Failed to broadcast - " + error.getMessage()));
                    return null;
                });
    }

    /**
     * Broadcast a sample currency context (US Dollar).
     */
    private void broadcastCurrency() {
        if (agent == null || currentChannel == null) return;
        
        Map<String, Object> id = new HashMap<>();
        id.put("CURRENCY_ISOCODE", "USD");
        
        Context currencyContext = new Context("fdc3.currency", "US Dollar", id);
        
        log("Broadcasting currency context: US Dollar (USD)");
        agent.broadcast(currencyContext)
                .thenRun(() -> SwingUtilities.invokeLater(() -> 
                    log("Successfully broadcast currency context")))
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() ->
                        log("ERROR: Failed to broadcast - " + error.getMessage()));
                    return null;
                });
    }

    /**
     * Broadcast a sample contact context (Jane Doe).
     */
    private void broadcastContact() {
        if (agent == null || currentChannel == null) return;
        
        Map<String, Object> id = new HashMap<>();
        id.put("email", "jane.doe@mail.com");
        
        Context contactContext = new Context("fdc3.contact", "Jane Doe", id);
        
        log("Broadcasting contact context: Jane Doe");
        agent.broadcast(contactContext)
                .thenRun(() -> SwingUtilities.invokeLater(() -> 
                    log("Successfully broadcast contact context")))
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() ->
                        log("ERROR: Failed to broadcast - " + error.getMessage()));
                    return null;
                });
    }

    private void onContextReceived(Context context, ContextMetadata metadata) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("=== CONTEXT RECEIVED ===\n");
            sb.append("Type: ").append(context.getType()).append("\n");
            
            if (metadata != null && metadata.getSource() != null) {
                sb.append("Source: ").append(metadata.getSource().getAppId());
                if (metadata.getSource().getInstanceId() != null) {
                    sb.append(" (").append(metadata.getSource().getInstanceId()).append(")");
                }
                sb.append("\n");
            }
            
            // Log the context details
            try {
                String contextJson = context.toString();
                sb.append("Data: ").append(contextJson).append("\n");
            } catch (Exception e) {
                sb.append("Data: [Error formatting context]\n");
            }
            
            log(sb.toString());
        });
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String logMessage = "[" + timestamp + "] " + message + "\n";
        logArea.append(logMessage);
        // Auto-scroll to bottom
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void cleanup() {
        if (contextListener != null) {
            try {
                contextListener.unsubscribe().toCompletableFuture().join();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    /**
     * Helper class to display channels in the combo box
     */
    private static class ChannelItem {
        final Channel channel;

        ChannelItem(Channel channel) {
            this.channel = channel;
        }

        @Override
        public String toString() {
            if (channel == null) {
                return "(None)";
            }
            String name = channel.getId();
            if (channel.getDisplayMetadata() != null && 
                channel.getDisplayMetadata().getName() != null) {
                name = channel.getDisplayMetadata().getName();
            }
            return name + " [" + channel.getId() + "]";
        }
    }

    public static void main(String[] args) {
        String[] launchArgs = args != null ? args : new String[0];

        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }

        // macOS delivers protocol URLs via Desktop.setOpenURIHandler (requires jpackage .app).
        registerOpenUriHandler();

        // Create and show the application
        SwingUtilities.invokeLater(() -> {
            ExampleApp app = new ExampleApp(launchArgs);
            activeInstance = app;
            app.setVisible(true);
        });
    }
}
