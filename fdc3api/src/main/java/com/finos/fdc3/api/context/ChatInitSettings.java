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

package com.finos.fdc3.api.context;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "type",
        "chatName",
        "members",
        "initMessage",
        "options"
})
public class ChatInitSettings extends Context {
    public static String TYPE = "fdc3.chat.initSettings";

    @JsonProperty("chatName")
    private String chatName;

    @JsonProperty("members")
    private ContactList members;

    @JsonProperty("initMessage")
    private String initMessage;

    @JsonProperty("options")
    private Options options;

    public ChatInitSettings() {
        super(TYPE);
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public ContactList getMembers() {
        return members;
    }

    public void setMembers(ContactList members) {
        this.members = members;
    }

    public String getInitMessage() {
        return initMessage;
    }

    public void setInitMessage(String initMessage) {
        this.initMessage = initMessage;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }
}
