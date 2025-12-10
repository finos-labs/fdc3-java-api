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

package com.finos.fdc3.proxy.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.finos.fdc3.api.channel.Channel;
import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.types.AppIdentifier;

/**
 * Test implementation of messaging for Cucumber tests.
 * Simulates the message exchange between the Desktop Agent and apps.
 */
public class TestMessaging {

    private final List<Map<String, Object>> allPosts = new ArrayList<>();
    private final List<IntentDetail> intentDetails = new ArrayList<>();
    private final Map<String, List<Context>> channelState;
    private Channel currentChannel;
    private PossibleIntentResult intentResult;
    private final String appId;
    private final String instanceId;

    public TestMessaging(Map<String, List<Context>> channelState) {
        this.channelState = channelState != null ? channelState : new HashMap<>();
        this.appId = "cucumber-app";
        this.instanceId = "cucumber-instance";
    }

    public String createUUID() {
        return UUID.randomUUID().toString();
    }

    public int getTimeoutMs() {
        return 1000;
    }

    public List<Map<String, Object>> getAllPosts() {
        return allPosts;
    }

    public void post(Map<String, Object> message) {
        allPosts.add(message);
    }

    public void addAppIntentDetail(IntentDetail detail) {
        intentDetails.add(detail);
    }

    public List<IntentDetail> getIntentDetails() {
        return intentDetails;
    }

    public Map<String, Object> createMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestUuid", createUUID());
        meta.put("timestamp", Instant.now().toString());
        Map<String, String> source = new HashMap<>();
        source.put("appId", appId);
        source.put("instanceId", instanceId);
        meta.put("source", source);
        return meta;
    }

    public Map<String, Object> createResponseMeta() {
        Map<String, Object> meta = createMeta();
        meta.put("responseUuid", createUUID());
        return meta;
    }

    public Map<String, Object> createEventMeta() {
        Map<String, Object> meta = createMeta();
        meta.put("eventUuid", createUUID());
        return meta;
    }

    public void receive(Map<String, Object> message, Consumer<String> log) {
        // Process incoming messages
        if (log != null) {
            log.accept("Received message: " + message.get("type"));
        }
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

    public String getAppId() {
        return appId;
    }

    public String getInstanceId() {
        return instanceId;
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

