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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Example Java Swing application demonstrating FDC3 Desktop Agent connectivity.
 * <p>
 * This application:
 * <ul>
 *   <li>Connects to a Desktop Agent via WebSocket on startup</li>
 *   <li>Displays the current user channel and allows changing channels</li>
 *   <li>Shows a log of context broadcasts received on the current channel</li>
 *   <li>Supports adding and removing context listeners</li>
 * </ul>
 * <p>
 * Required system properties or environment variables:
 * <ul>
 *   <li>{@code FDC3_WEBSOCKET_URL} - WebSocket URL for the Desktop Agent</li>
 * </ul>
 */
public class ExampleApp extends JFrame {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
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

    public ExampleApp() {
        super("FDC3 Example App");
        initUI();
        
        // Connect to Desktop Agent on startup
        SwingUtilities.invokeLater(this::connectToAgent);
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top panel - Connection status and channel selection
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Status:"));
        statusLabel = new JLabel("Connecting...");
        statusLabel.setForeground(Color.ORANGE);
        statusPanel.add(statusLabel);
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

        // Bottom panel - Listener controls
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

        mainPanel.add(listenerPanel, BorderLayout.SOUTH);

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
        log("Connecting to Desktop Agent...");
        
        try {
            GetAgentParams params = GetAgentParams.builder()
                    .timeoutMs(30000)
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
            log("Successfully connected to Desktop Agent");
            
            // Load user channels
            loadUserChannels();
        });
    }

    private void onConnectionError(Throwable error) {
        statusLabel.setText("Connection Failed");
        statusLabel.setForeground(Color.RED);
        log("ERROR: Failed to connect - " + error.getMessage());
        
        // Show error dialog
        JOptionPane.showMessageDialog(this,
                "Failed to connect to Desktop Agent:\n" + error.getMessage() +
                "\n\nMake sure the following system properties are set:\n" +
                "- FDC3_WEBSOCKET_URL\n" +
                "- FDC3_INSTANCE_ID\n" +
                "- FDC3_INSTANCE_UUID",
                "Connection Error",
                JOptionPane.ERROR_MESSAGE);
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
                    SwingUtilities.invokeLater(() -> 
                        log("Joined channel: " + channel.getId()));
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
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }

        // Create and show the application
        SwingUtilities.invokeLater(() -> {
            ExampleApp app = new ExampleApp();
            app.setVisible(true);
        });
    }
}
