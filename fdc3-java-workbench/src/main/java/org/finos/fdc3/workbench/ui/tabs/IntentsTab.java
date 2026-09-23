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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.finos.fdc3.api.DesktopAgent;
import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.channel.PrivateChannel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.metadata.AppMetadata;
import org.finos.fdc3.api.metadata.IntentResolution;
import org.finos.fdc3.workbench.model.ContextTemplate;
import org.finos.fdc3.workbench.model.ListenerRecord;
import org.finos.fdc3.workbench.service.WorkbenchServices;

import com.fasterxml.jackson.core.type.TypeReference;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Raise intent / raiseIntentForContext / addIntentListener with optional results.
 */
public class IntentsTab extends VBox {

    private final WorkbenchServices services;
    private final ComboBox<ContextTemplate> contextCombo = new ComboBox<>();
    private final ComboBox<String> intentCombo = new ComboBox<>();
    private final ComboBox<AppMetadata> targetCombo = new ComboBox<>();
    private final TextField intentListenerField = new TextField();
    private final TextField contextFilterField = new TextField();
    private final TextArea resolutionArea = new TextArea();
    private final CheckBox returnResult = new CheckBox("Return intent result");
    private final ToggleGroup resultType = new ToggleGroup();
    private final RadioButton resultContext = new RadioButton("Context");
    private final RadioButton resultPrivateChannel = new RadioButton("Private channel");
    private final RadioButton resultAppChannel = new RadioButton("App channel");
    private final TextField resultAppChannelId = new TextField("result-channel");

    public IntentsTab(WorkbenchServices services) {
        this.services = services;
        setSpacing(12);
        setPadding(new Insets(12));
        getStyleClass().add("panel-card");

        contextCombo.setItems(services.getContexts().getTemplates());
        contextCombo.setPrefWidth(280);
        intentCombo.setEditable(true);
        intentCombo.setPrefWidth(280);
        intentCombo.getItems().setAll(loadIntentTypes());
        targetCombo.setPrefWidth(320);
        targetCombo.setPromptText("(optional target)");
        intentListenerField.setPromptText("Intent name");
        contextFilterField.setPromptText("Optional context type filter");
        resolutionArea.setEditable(false);
        resolutionArea.setPrefRowCount(8);
        resolutionArea.getStyleClass().add("log-view");

        resultContext.setToggleGroup(resultType);
        resultPrivateChannel.setToggleGroup(resultType);
        resultAppChannel.setToggleGroup(resultType);
        resultContext.setSelected(true);

        Button findTargets = new Button("Find Targets");
        findTargets.setOnAction(e -> findTargets());
        Button raise = new Button("Raise Intent");
        raise.setOnAction(e -> raiseIntent(false));
        Button raiseForContext = new Button("Raise Intent For Context");
        raiseForContext.setOnAction(e -> raiseIntent(true));
        Button addListener = new Button("Add Intent Listener");
        addListener.setOnAction(e -> addIntentListener());

        GridPane raiseGrid = new GridPane();
        raiseGrid.setHgap(8);
        raiseGrid.setVgap(8);
        raiseGrid.add(new Label("Context"), 0, 0);
        raiseGrid.add(contextCombo, 1, 0);
        raiseGrid.add(new Label("Intent"), 0, 1);
        raiseGrid.add(intentCombo, 1, 1);
        raiseGrid.add(new Label("Target"), 0, 2);
        raiseGrid.add(targetCombo, 1, 2);
        raiseGrid.add(new HBox(8, findTargets, raise, raiseForContext), 1, 3);

        VBox listenerBox = new VBox(8,
                new Label("Intent listener"),
                new HBox(8, intentListenerField, contextFilterField, addListener),
                returnResult,
                new HBox(12, resultContext, resultPrivateChannel, resultAppChannel, resultAppChannelId));

        getChildren().addAll(
                new Label("Intents") {{ getStyleClass().add("section-title"); }},
                raiseGrid,
                new Label("Resolution / Result"),
                resolutionArea,
                listenerBox);
    }

    private List<String> loadIntentTypes() {
        try (InputStream in = getClass().getResourceAsStream("/intent-types.json")) {
            if (in == null) {
                return List.of();
            }
            return services.getMapper().readValue(in, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            services.getLog().error("intents", "Failed to load intent types - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private DesktopAgent agent() {
        return services.getAgentHolder().getAgent().orElse(null);
    }

    private Context selectedContext() {
        ContextTemplate template = contextCombo.getValue();
        if (template == null) {
            return null;
        }
        Map<String, Object> map = template.asMap(services.getMapper());
        Context context = new Context();
        context.putAll(map);
        return context;
    }

    private void findTargets() {
        DesktopAgent agent = agent();
        Context context = selectedContext();
        String intent = intentCombo.getValue();
        if (agent == null || context == null || intent == null || intent.isBlank()) {
            return;
        }
        agent.findIntent(intent.trim(), context)
                .thenAccept(appIntent -> Platform.runLater(() -> {
                    targetCombo.getItems().clear();
                    if (appIntent != null && appIntent.getApps() != null) {
                        targetCombo.getItems().addAll(java.util.Arrays.asList(appIntent.getApps()));
                    }
                    services.getLog().info("findIntent",
                            "Found " + targetCombo.getItems().size() + " apps for " + intent);
                }))
                .exceptionally(err -> {
                    services.getLog().error("findIntent", err.getMessage());
                    return null;
                });
    }

    private void raiseIntent(boolean forContext) {
        DesktopAgent agent = agent();
        Context context = selectedContext();
        String intent = intentCombo.getValue();
        if (agent == null || context == null) {
            return;
        }
        AppMetadata target = targetCombo.getValue();
        AppIdentifier appId = target;

        var stage = forContext
                ? (target != null
                        ? agent.raiseIntentForContext(context, appId)
                        : agent.raiseIntentForContext(context))
                : (intent == null || intent.isBlank()
                        ? null
                        : (target != null
                                ? agent.raiseIntent(intent.trim(), context, appId)
                                : agent.raiseIntent(intent.trim(), context)));

        if (stage == null) {
            services.getLog().error("raiseIntent", "Intent name required");
            return;
        }

        stage.thenAccept(resolution -> Platform.runLater(() -> showResolution(resolution)))
                .exceptionally(err -> {
                    services.getLog().error(forContext ? "raiseIntentForContext" : "raiseIntent",
                            err.getMessage());
                    return null;
                });
    }

    private void showResolution(IntentResolution resolution) {
        StringBuilder sb = new StringBuilder();
        if (resolution.getSource() != null) {
            sb.append("source: ").append(resolution.getSource().getAppId());
            if (resolution.getSource().getInstanceId() != null) {
                sb.append(" / ").append(resolution.getSource().getInstanceId());
            }
            sb.append('\n');
        }
        if (resolution.getIntent() != null) {
            sb.append("intent: ").append(resolution.getIntent()).append('\n');
        }
        resolutionArea.setText(sb.toString());
        services.getLog().info("raiseIntent", "Intent raised", sb.toString());

        resolution.getResult().thenAccept(result -> Platform.runLater(() -> {
            try {
                String json = services.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);
                resolutionArea.appendText("\nresult:\n" + json);
                services.getLog().info("intentResult", "Received intent result", json);
                if (result instanceof PrivateChannel privateChannel) {
                    services.getChannels().getAppChannels().add(privateChannel);
                    services.getListeners().wirePrivateChannelEvents(privateChannel, privateChannel.getId());
                } else if (result instanceof Channel channel) {
                    if (services.getChannels().getAppChannels().stream()
                            .noneMatch(c -> c.getId().equals(channel.getId()))) {
                        services.getChannels().getAppChannels().add(channel);
                    }
                }
            } catch (Exception e) {
                resolutionArea.appendText("\nresult: " + result);
            }
        })).exceptionally(err -> {
            services.getLog().info("intentResult", "No result / timeout: " + err.getMessage());
            return null;
        });
    }

    private void addIntentListener() {
        DesktopAgent agent = agent();
        String intent = intentListenerField.getText();
        if (agent == null || intent == null || intent.isBlank()) {
            return;
        }
        String filter = contextFilterField.getText();
        String label = intent.trim() + (filter == null || filter.isBlank() ? "" : " [" + filter.trim() + "]");

        final ListenerRecord[] holder = new ListenerRecord[1];
        var handler = (org.finos.fdc3.api.types.IntentHandler) (context, metadata) -> {
            Platform.runLater(() -> {
                try {
                    String json = services.getMapper().writerWithDefaultPrettyPrinter()
                            .writeValueAsString(context);
                    if (holder[0] != null) {
                        holder[0].setLastReceived(json);
                    }
                    services.getLog().info("intentReceived", "Intent " + intent + " received", json);
                } catch (Exception e) {
                    if (holder[0] != null) {
                        holder[0].setLastReceived(String.valueOf(context));
                    }
                }
            });
            return buildIntentResult(context);
        };

        var stage = (filter == null || filter.isBlank())
                ? agent.addIntentListener(intent.trim(), handler)
                : agent.addIntentListenerWithContext(intent.trim(), filter.trim(), handler);

        stage.thenAccept(listener -> Platform.runLater(() ->
                holder[0] = services.getListeners().trackIntentListener(label, listener)
        )).exceptionally(err -> {
            services.getLog().error("addIntentListener", err.getMessage());
            return null;
        });
    }

    private CompletableFuture<Optional<Object>> buildIntentResult(Context raised) {
        if (!returnResult.isSelected()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        DesktopAgent agent = agent();
        if (agent == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if (resultPrivateChannel.isSelected()) {
            return agent.createPrivateChannel()
                    .thenApply(ch -> {
                        Platform.runLater(() -> {
                            services.getChannels().getAppChannels().add(ch);
                            services.getListeners().wirePrivateChannelEvents(ch, ch.getId());
                            services.getLog().info("intentResult", "Returning private channel " + ch.getId());
                        });
                        return Optional.<Object>of(ch);
                    })
                    .toCompletableFuture();
        }
        if (resultAppChannel.isSelected()) {
            String id = resultAppChannelId.getText();
            if (id == null || id.isBlank()) {
                id = "result-channel";
            }
            String channelId = id.trim();
            return agent.getOrCreateChannel(channelId)
                    .thenApply(ch -> {
                        Platform.runLater(() -> {
                            if (services.getChannels().getAppChannels().stream()
                                    .noneMatch(c -> c.getId().equals(ch.getId()))) {
                                services.getChannels().getAppChannels().add(ch);
                            }
                            services.getLog().info("intentResult", "Returning app channel " + ch.getId());
                        });
                        return Optional.<Object>of(ch);
                    })
                    .toCompletableFuture();
        }
        // Return the raised context as the result by default
        return CompletableFuture.completedFuture(Optional.of(raised));
    }
}
