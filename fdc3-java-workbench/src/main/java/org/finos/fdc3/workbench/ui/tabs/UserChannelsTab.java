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

package org.finos.fdc3.workbench.ui.tabs;

import java.util.Map;

import org.finos.fdc3.api.DesktopAgent;
import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.workbench.model.ContextTemplate;
import org.finos.fdc3.workbench.model.ListenerRecord;
import org.finos.fdc3.workbench.service.WorkbenchServices;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * User channel join / leave / broadcast / addContextListener.
 */
public class UserChannelsTab extends VBox {

    private final WorkbenchServices services;
    private final ComboBox<Channel> channelCombo = new ComboBox<>();
    private final ComboBox<ContextTemplate> contextCombo = new ComboBox<>();
    private final TextField listenerType = new TextField();
    private final Label currentLabel = new Label();

    public UserChannelsTab(WorkbenchServices services) {
        this.services = services;
        setSpacing(12);
        setPadding(new Insets(12));
        getStyleClass().add("panel-card");

        channelCombo.setItems(services.getChannels().getUserChannels());
        channelCombo.setPrefWidth(280);
        contextCombo.setItems(services.getContexts().getTemplates());
        contextCombo.setPrefWidth(280);
        listenerType.setPromptText("context type (blank = all)");
        currentLabel.textProperty().bind(services.getChannels().currentUserChannelIdProperty());

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> refreshChannels());
        Button join = new Button("Join");
        join.setOnAction(e -> joinChannel());
        Button leave = new Button("Leave");
        leave.setOnAction(e -> leaveChannel());
        Button broadcast = new Button("Broadcast");
        broadcast.setOnAction(e -> broadcast());
        Button addListener = new Button("Add Context Listener");
        addListener.setOnAction(e -> addListener());

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label("Current channel"), 0, 0);
        grid.add(currentLabel, 1, 0);
        grid.add(refresh, 2, 0);
        grid.add(new Label("Join channel"), 0, 1);
        grid.add(channelCombo, 1, 1);
        grid.add(join, 2, 1);
        grid.add(leave, 3, 1);
        grid.add(new Label("Broadcast context"), 0, 2);
        grid.add(contextCombo, 1, 2);
        grid.add(broadcast, 2, 2);
        grid.add(new Label("Listener type"), 0, 3);
        grid.add(listenerType, 1, 3);
        grid.add(addListener, 2, 3);

        getChildren().addAll(new Label("User Channels") {{ getStyleClass().add("section-title"); }}, grid);
    }

    private DesktopAgent agent() {
        return services.getAgentHolder().getAgent().orElse(null);
    }

    private void refreshChannels() {
        DesktopAgent agent = agent();
        if (agent == null) {
            return;
        }
        agent.getUserChannels().thenAccept(channels -> Platform.runLater(() -> {
            services.getChannels().getUserChannels().setAll(channels);
            services.getLog().info("userChannels", "Refreshed " + channels.size() + " user channels");
        }));
        agent.getCurrentChannel().thenAccept(opt -> Platform.runLater(() ->
                services.getChannels().setCurrentUserChannel(opt.orElse(null))));
    }

    private void joinChannel() {
        DesktopAgent agent = agent();
        Channel channel = channelCombo.getValue();
        if (agent == null || channel == null) {
            return;
        }
        agent.joinUserChannel(channel.getId())
                .thenRun(() -> Platform.runLater(() -> {
                    services.getChannels().setCurrentUserChannel(channel);
                    services.getLog().info("joinUserChannel", "Joined " + channel.getId());
                }))
                .exceptionally(err -> {
                    services.getLog().error("joinUserChannel", err.getMessage());
                    return null;
                });
    }

    private void leaveChannel() {
        DesktopAgent agent = agent();
        if (agent == null) {
            return;
        }
        agent.leaveCurrentChannel()
                .thenRun(() -> Platform.runLater(() -> {
                    services.getChannels().setCurrentUserChannel(null);
                    services.getLog().info("leaveCurrentChannel", "Left current user channel");
                }))
                .exceptionally(err -> {
                    services.getLog().error("leaveCurrentChannel", err.getMessage());
                    return null;
                });
    }

    private void broadcast() {
        DesktopAgent agent = agent();
        ContextTemplate template = contextCombo.getValue();
        if (agent == null || template == null) {
            return;
        }
        Context context = toContext(template);
        agent.broadcast(context)
                .thenRun(() -> services.getLog().info("broadcast",
                        "Broadcast " + template.getId(), pretty(context)))
                .exceptionally(err -> {
                    services.getLog().error("broadcast", err.getMessage());
                    return null;
                });
    }

    private void addListener() {
        DesktopAgent agent = agent();
        if (agent == null) {
            return;
        }
        String type = listenerType.getText();
        String label = (type == null || type.isBlank()) ? "(all)" : type.trim();
        String contextType = (type == null || type.isBlank()) ? null : type.trim();

        // Placeholder record so handler can update lastReceived once we have Listener
        final ListenerRecord[] holder = new ListenerRecord[1];
        agent.addContextListener(contextType, (ctx, meta) -> {
            if (holder[0] != null) {
                services.getListeners().contextHandler(holder[0]).handleContext(ctx, meta);
            }
        }).thenAccept(listener -> Platform.runLater(() -> {
            holder[0] = services.getListeners().trackContextListener(label, listener);
        })).exceptionally(err -> {
            services.getLog().error("addContextListener", err.getMessage());
            return null;
        });
    }

    private Context toContext(ContextTemplate template) {
        Map<String, Object> map = template.asMap(services.getMapper());
        Context context = new Context();
        context.putAll(map);
        return context;
    }

    private String pretty(Object value) {
        try {
            return services.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
