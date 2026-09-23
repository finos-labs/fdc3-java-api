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
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * App channels: getOrCreate, broadcast, addContextListener, discard (local).
 */
public class AppChannelsTab extends VBox {

    private final WorkbenchServices services;
    private final TextField channelIdField = new TextField();
    private final ListView<Channel> channelList = new ListView<>();
    private final ComboBox<ContextTemplate> contextCombo = new ComboBox<>();
    private final TextField listenerType = new TextField();

    public AppChannelsTab(WorkbenchServices services) {
        this.services = services;
        setSpacing(12);
        setPadding(new Insets(12));
        getStyleClass().add("panel-card");

        channelIdField.setPromptText("channel id");
        channelList.setItems(services.getChannels().getAppChannels());
        contextCombo.setItems(services.getContexts().getTemplates());
        listenerType.setPromptText("context type (blank = all)");

        Button getOrCreate = new Button("Get or Create");
        getOrCreate.setOnAction(e -> getOrCreate());
        Button discard = new Button("Discard");
        discard.setOnAction(e -> {
            Channel selected = channelList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                services.getChannels().getAppChannels().remove(selected);
                services.getLog().info("appChannel", "Discarded local reference to " + selected.getId());
            }
        });
        Button broadcast = new Button("Broadcast");
        broadcast.setOnAction(e -> broadcast());
        Button addListener = new Button("Add Context Listener");
        addListener.setOnAction(e -> addListener());

        HBox createRow = new HBox(8, channelIdField, getOrCreate, discard);
        HBox.setHgrow(channelIdField, Priority.ALWAYS);
        HBox actionRow = new HBox(8, contextCombo, broadcast, listenerType, addListener);

        VBox.setVgrow(channelList, Priority.ALWAYS);
        getChildren().addAll(
                new Label("App Channels") {{ getStyleClass().add("section-title"); }},
                createRow,
                new Label("Open app channels"),
                channelList,
                actionRow);
    }

    private void getOrCreate() {
        DesktopAgent agent = services.getAgentHolder().getAgent().orElse(null);
        String id = channelIdField.getText();
        if (agent == null || id == null || id.isBlank()) {
            return;
        }
        agent.getOrCreateChannel(id.trim())
                .thenAccept(channel -> Platform.runLater(() -> {
                    if (services.getChannels().getAppChannels().stream()
                            .noneMatch(c -> c.getId().equals(channel.getId()))) {
                        services.getChannels().getAppChannels().add(channel);
                    }
                    channelList.getSelectionModel().select(channel);
                    services.getLog().info("getOrCreateChannel", "Got channel " + channel.getId());
                }))
                .exceptionally(err -> {
                    services.getLog().error("getOrCreateChannel", err.getMessage());
                    return null;
                });
    }

    private void broadcast() {
        Channel channel = channelList.getSelectionModel().getSelectedItem();
        ContextTemplate template = contextCombo.getValue();
        if (channel == null || template == null) {
            return;
        }
        Context context = toContext(template);
        channel.broadcast(context)
                .thenRun(() -> services.getLog().info("broadcast",
                        "Broadcast on app channel " + channel.getId(), pretty(context)))
                .exceptionally(err -> {
                    services.getLog().error("broadcast", err.getMessage());
                    return null;
                });
    }

    private void addListener() {
        Channel channel = channelList.getSelectionModel().getSelectedItem();
        if (channel == null) {
            return;
        }
        String type = listenerType.getText();
        String label = (type == null || type.isBlank()) ? "(all)" : type.trim();
        String contextType = (type == null || type.isBlank()) ? null : type.trim();
        final ListenerRecord[] holder = new ListenerRecord[1];
        channel.addContextListener(contextType, (ctx, meta) -> {
            if (holder[0] != null) {
                services.getListeners().contextHandler(holder[0]).handleContext(ctx, meta);
            }
        }).thenAccept(listener -> Platform.runLater(() ->
                holder[0] = services.getListeners().trackAppChannelListener(channel.getId(), label, listener)
        )).exceptionally(err -> {
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
