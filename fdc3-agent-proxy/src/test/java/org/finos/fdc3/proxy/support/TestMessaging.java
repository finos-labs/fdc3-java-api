/**
 * Copyright 2023 Wellington Management Company LLP
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

package org.finos.fdc3.proxy.support;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.proxy.listeners.RegisterableListener;
import org.finos.fdc3.proxy.messaging.AbstractMessaging;
import org.finos.fdc3.proxy.support.responses.*;

/**
 * Test implementation of messaging for Cucumber tests.
 * Simulates the message exchange between the Desktop Agent and apps.
 */
public class TestMessaging extends AbstractMessaging {

    private final List<Map<String, Object>> allPosts = new ArrayList<>();
    private final Map<String, RegisterableListener> listeners = new ConcurrentHashMap<>();
    private final List<IntentDetail> intentDetails = new ArrayList<>();
    private final Map<String, List<Context>> channelState;
    private final List<AutomaticResponse> automaticResponses;
    
    private Channel currentChannel;
    private PossibleIntentResult intentResult;

    public TestMessaging(Map<String, List<Context>> channelState) {
        super(new AppIdentifier() {
            @Override
            public String getAppId() {
                return "cucumber-app";
            }

            @Override
            public java.util.Optional<String> getInstanceId() {
                return java.util.Optional.of("cucumber-instance");
            }

            @Override
            public java.util.Optional<String> getDesktopAgent() {
                return java.util.Optional.of("testing-da");
            }
        });
        this.channelState = channelState != null ? channelState : new HashMap<>();
        
        // Set up automatic responses for various message types
        this.automaticResponses = new ArrayList<>();
        this.automaticResponses.add(new FindIntentResponse());
        this.automaticResponses.add(new FindIntentByContextResponse());
        this.automaticResponses.add(new RaiseIntentResponse());
        this.automaticResponses.add(new RaiseIntentForContextResponse());
        this.automaticResponses.add(new IntentResultResponse());
        this.automaticResponses.add(new GetAppMetadataResponse());
        this.automaticResponses.add(new GetInfoResponse());
        this.automaticResponses.add(new FindInstancesResponse());
        this.automaticResponses.add(new OpenResponse());
        this.automaticResponses.add(new GetOrCreateChannelResponse());
        this.automaticResponses.add(new ChannelStateResponse(this.channelState));
        this.automaticResponses.add(new GetUserChannelsResponse());
        this.automaticResponses.add(new RegisterListenersResponse());
        this.automaticResponses.add(new UnsubscribeListenersResponse());
        this.automaticResponses.add(new CreatePrivateChannelResponse());
        this.automaticResponses.add(new DisconnectPrivateChannelResponse());
        this.automaticResponses.add(new AddEventListenerResponse());
    }

    @Override
    public String createUUID() {
        return UUID.randomUUID().toString();
    }

    public int getTimeoutMs() {
        return 1000;
    }

    public List<Map<String, Object>> getAllPosts() {
        return allPosts;
    }

    @Override
    public CompletionStage<Void> post(Map<String, Object> message) {
        allPosts.add(message);
        
        String type = (String) message.get("type");
        if (!"WCP6Goodbye".equals(type)) {
            for (AutomaticResponse ar : automaticResponses) {
                if (ar.filter(type)) {
                    return ar.action(message, this);
                }
            }
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void register(RegisterableListener listener) {
        if (listener.getId() == null) {
            throw new IllegalArgumentException("Listener must have ID set");
        }
        listeners.put(listener.getId(), listener);
    }

    @Override
    public void unregister(String id) {
        listeners.remove(id);
    }

    @Override
    public CompletionStage<Void> disconnect() {
        Map<String, Object> bye = new HashMap<>();
        bye.put("type", "WCP6Goodbye");
        Map<String, Object> meta = new HashMap<>();
        meta.put("timestamp", OffsetDateTime.now().toString());
        bye.put("meta", meta);
        return post(bye);
    }

    public void addAppIntentDetail(IntentDetail detail) {
        intentDetails.add(detail);
    }

    public List<IntentDetail> getIntentDetails() {
        return intentDetails;
    }

    /**
     * Used in testing steps to create response metadata.
     */
    public Map<String, Object> createResponseMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestUuid", createUUID());
        meta.put("responseUuid", createUUID());
        meta.put("timestamp", Instant.now().toString());
        Map<String, String> source = new HashMap<>();
        source.put("appId", getAppIdentifier().getAppId());
        getAppIdentifier().getInstanceId().ifPresent(id -> source.put("instanceId", id));
        meta.put("source", source);
        return meta;
    }

    /**
     * Used in testing steps to create event metadata.
     */
    public Map<String, Object> createEventMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestUuid", createUUID());
        meta.put("eventUuid", createUUID());
        meta.put("timestamp", Instant.now().toString());
        Map<String, String> source = new HashMap<>();
        source.put("appId", getAppIdentifier().getAppId());
        getAppIdentifier().getInstanceId().ifPresent(id -> source.put("instanceId", id));
        meta.put("source", source);
        return meta;
    }

    /**
     * Simulates receiving a message from the Desktop Agent.
     * Dispatches to all registered listeners that match the message.
     */
    public void receive(Map<String, Object> message, Consumer<String> log) {
        listeners.forEach((id, listener) -> {
            if (listener.filter(message)) {
                if (log != null) {
                    log.accept("Processing in " + id);
                }
                listener.action(message);
            } else {
                if (log != null) {
                    log.accept("Ignoring in " + id);
                }
            }
        });
    }

    public PossibleIntentResult getIntentResult() {
        return intentResult;
    }

    public void setIntentResult(PossibleIntentResult result) {
        this.intentResult = result;
    }

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void setCurrentChannel(Channel channel) {
        this.currentChannel = channel;
    }

    public Map<String, List<Context>> getChannelState() {
        return channelState;
    }

    /**
     * Represents details about an intent that can be resolved.
     */
    public static class IntentDetail {
        private AppIdentifier app;
        private String intent;
        private String context;
        private String resultType;

        public AppIdentifier getApp() {
            return app;
        }

        public void setApp(AppIdentifier app) {
            this.app = app;
        }

        public String getIntent() {
            return intent;
        }

        public void setIntent(String intent) {
            this.intent = intent;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public String getResultType() {
            return resultType;
        }

        public void setResultType(String resultType) {
            this.resultType = resultType;
        }
    }

    /**
     * Represents a possible result from raising an intent.
     */
    public static class PossibleIntentResult {
        private Context context;
        private Channel channel;
        private String error;
        private boolean timeout;

        public Context getContext() {
            return context;
        }

        public void setContext(Context context) {
            this.context = context;
        }

        public Channel getChannel() {
            return channel;
        }

        public void setChannel(Channel channel) {
            this.channel = channel;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public boolean isTimeout() {
            return timeout;
        }

        public void setTimeout(boolean timeout) {
            this.timeout = timeout;
        }
    }
}
