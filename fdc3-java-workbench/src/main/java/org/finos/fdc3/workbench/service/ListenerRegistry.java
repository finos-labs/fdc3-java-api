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

package org.finos.fdc3.workbench.service;

import java.util.UUID;

import org.finos.fdc3.api.DesktopAgent;
import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.channel.PrivateChannel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.workbench.model.ListenerRecord;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Manages registered listeners shown in the right-hand workbench panel.
 */
public class ListenerRegistry {

    private final ObservableList<ListenerRecord> contextListeners = FXCollections.observableArrayList();
    private final ObservableList<ListenerRecord> intentListeners = FXCollections.observableArrayList();
    private final ObservableList<ListenerRecord> appChannelListeners = FXCollections.observableArrayList();
    private final ObservableList<ListenerRecord> privateChannelListeners = FXCollections.observableArrayList();

    private final SystemLogService log;
    private final ObjectMapper mapper;

    public ListenerRegistry(SystemLogService log, ObjectMapper mapper) {
        this.log = log;
        this.mapper = mapper;
    }

    public ObservableList<ListenerRecord> getContextListeners() {
        return contextListeners;
    }

    public ObservableList<ListenerRecord> getIntentListeners() {
        return intentListeners;
    }

    public ObservableList<ListenerRecord> getAppChannelListeners() {
        return appChannelListeners;
    }

    public ObservableList<ListenerRecord> getPrivateChannelListeners() {
        return privateChannelListeners;
    }

    public void clearAll() {
        unsubscribeAll(contextListeners);
        unsubscribeAll(intentListeners);
        unsubscribeAll(appChannelListeners);
        unsubscribeAll(privateChannelListeners);
        contextListeners.clear();
        intentListeners.clear();
        appChannelListeners.clear();
        privateChannelListeners.clear();
    }

    public ContextHandler contextHandler(ListenerRecord record) {
        return (context, metadata) -> Platform.runLater(() -> {
            try {
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
                record.setLastReceived(json);
                log.info("contextReceived", "Context received on " + record.getLabel(), json);
            } catch (Exception e) {
                record.setLastReceived(String.valueOf(context));
            }
        });
    }

    public ListenerRecord addContextListener(String label, Listener listener, ContextHandler unused) {
        ListenerRecord record = new ListenerRecord(
                UUID.randomUUID().toString(), ListenerRecord.Kind.CONTEXT, label, null, listener);
        contextListeners.add(record);
        return record;
    }

    public ListenerRecord trackContextListener(String label, Listener listener) {
        ListenerRecord record = new ListenerRecord(
                UUID.randomUUID().toString(), ListenerRecord.Kind.CONTEXT, label, null, listener);
        contextListeners.add(record);
        log.info("addContextListener", "Added context listener: " + label);
        return record;
    }

    public ListenerRecord trackIntentListener(String label, Listener listener) {
        ListenerRecord record = new ListenerRecord(
                UUID.randomUUID().toString(), ListenerRecord.Kind.INTENT, label, null, listener);
        intentListeners.add(record);
        log.info("addIntentListener", "Added intent listener: " + label);
        return record;
    }

    public ListenerRecord trackAppChannelListener(String channelId, String label, Listener listener) {
        ListenerRecord record = new ListenerRecord(
                UUID.randomUUID().toString(), ListenerRecord.Kind.APP_CHANNEL, label, channelId, listener);
        appChannelListeners.add(record);
        log.info("addContextListener", "Added app-channel listener on " + channelId + ": " + label);
        return record;
    }

    public ListenerRecord trackPrivateChannelListener(String channelId, String label, Listener listener) {
        ListenerRecord record = new ListenerRecord(
                UUID.randomUUID().toString(), ListenerRecord.Kind.PRIVATE_CHANNEL, label, channelId, listener);
        privateChannelListeners.add(record);
        log.info("addContextListener", "Added private-channel listener on " + channelId + ": " + label);
        return record;
    }

    public void remove(ListenerRecord record) {
        try {
            if (record.getListener() != null) {
                record.getListener().unsubscribe().toCompletableFuture().join();
            }
        } catch (Exception e) {
            log.error("unsubscribe", e.getMessage());
        }
        switch (record.getKind()) {
            case CONTEXT -> contextListeners.remove(record);
            case INTENT -> intentListeners.remove(record);
            case APP_CHANNEL -> appChannelListeners.remove(record);
            case PRIVATE_CHANNEL -> privateChannelListeners.remove(record);
        }
        log.info("unsubscribe", "Removed listener: " + record);
    }

    public Context toContext(java.util.Map<String, Object> map) {
        Context context = new Context();
        if (map != null) {
            context.putAll(map);
        }
        return context;
    }

    private void unsubscribeAll(ObservableList<ListenerRecord> list) {
        for (ListenerRecord record : new java.util.ArrayList<>(list)) {
            try {
                if (record.getListener() != null) {
                    record.getListener().unsubscribe().toCompletableFuture().join();
                }
            } catch (Exception ignored) {
                // best-effort cleanup on disconnect
            }
        }
    }

    public void wirePrivateChannelEvents(PrivateChannel channel, String channelId) {
        try {
            channel.addEventListener("addContextListener", event ->
                    log.info("privateChannel", "addContextListener on " + channelId));
            channel.addEventListener("unsubscribe", event ->
                    log.info("privateChannel", "unsubscribe on " + channelId));
            channel.addEventListener("disconnect", event ->
                    log.info("privateChannel", "disconnect on " + channelId));
        } catch (Exception e) {
            log.error("privateChannel", "Failed to wire events - " + e.getMessage());
        }
    }

    public DesktopAgent requireAgent(AgentHolder holder) {
        return holder.getAgent().orElseThrow(() -> new IllegalStateException("Not connected"));
    }

    public Channel requireChannel(java.util.concurrent.CompletionStage<Channel> stage) {
        return stage.toCompletableFuture().join();
    }
}
