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

package org.finos.fdc3.workbench.model;

import org.finos.fdc3.api.types.Listener;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * UI model for a registered FDC3 listener shown in the right-hand workbench panel.
 */
public class ListenerRecord {

    public enum Kind {
        CONTEXT,
        INTENT,
        APP_CHANNEL,
        PRIVATE_CHANNEL
    }

    private final String id;
    private final Kind kind;
    private final String label;
    private final String channelId;
    private final Listener listener;
    private final StringProperty lastReceived = new SimpleStringProperty("(none)");

    public ListenerRecord(String id, Kind kind, String label, String channelId, Listener listener) {
        this.id = id;
        this.kind = kind;
        this.label = label;
        this.channelId = channelId;
        this.listener = listener;
    }

    public String getId() {
        return id;
    }

    public Kind getKind() {
        return kind;
    }

    public String getLabel() {
        return label;
    }

    public String getChannelId() {
        return channelId;
    }

    public Listener getListener() {
        return listener;
    }

    public StringProperty lastReceivedProperty() {
        return lastReceived;
    }

    public void setLastReceived(String value) {
        lastReceived.set(value);
    }

    @Override
    public String toString() {
        if (channelId != null) {
            return kind + " / " + channelId + " / " + label;
        }
        return kind + " / " + label;
    }
}
