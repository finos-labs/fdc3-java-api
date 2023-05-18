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
        "groupRecipients",
        "public",
        "allowHistoryBrowsing",
        "allowMessageCopy",
        "allowAddUser",
})
public class Options {
    @JsonProperty("groupRecipients")
    private boolean groupRecipients;

    @JsonProperty("public")
    private boolean publicFlag;

    @JsonProperty("allowHistoryBrowsing")
    private boolean allowHistoryBrowsing;

    @JsonProperty("allowMessageCopy")
    private boolean allowMessageCopy;

    @JsonProperty("allowAddUser")
    private boolean allowAddUser;

    public Options() {
        this.groupRecipients = false;
        this.publicFlag = false;
        this.allowHistoryBrowsing = false;
        this.allowMessageCopy = false;
        this.allowAddUser = false;
    }

    public boolean isGroupRecipients() {
        return groupRecipients;
    }

    public void setGroupRecipients(boolean groupRecipients) {
        this.groupRecipients = groupRecipients;
    }

    public boolean isPublicFlag() {
        return publicFlag;
    }

    public void setPublicFlag(boolean publicFlag) {
        this.publicFlag = publicFlag;
    }

    public boolean isAllowHistoryBrowsing() {
        return allowHistoryBrowsing;
    }

    public void setAllowHistoryBrowsing(boolean allowHistoryBrowsing) {
        this.allowHistoryBrowsing = allowHistoryBrowsing;
    }

    public boolean isAllowMessageCopy() {
        return allowMessageCopy;
    }

    public void setAllowMessageCopy(boolean allowMessageCopy) {
        this.allowMessageCopy = allowMessageCopy;
    }

    public boolean isAllowAddUser() {
        return allowAddUser;
    }

    public void setAllowAddUser(boolean allowAddUser) {
        this.allowAddUser = allowAddUser;
    }
}
