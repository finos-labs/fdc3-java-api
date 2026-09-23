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

import org.finos.fdc3.api.channel.Channel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Observable user-channel and app-channel state for the UI.
 */
public class ObservableChannelState {

    private final ObservableList<Channel> userChannels = FXCollections.observableArrayList();
    private final ObjectProperty<Channel> currentUserChannel = new SimpleObjectProperty<>();
    private final StringProperty currentUserChannelId = new SimpleStringProperty("(none)");
    private final ObservableList<Channel> appChannels = FXCollections.observableArrayList();

    public ObservableList<Channel> getUserChannels() {
        return userChannels;
    }

    public ObjectProperty<Channel> currentUserChannelProperty() {
        return currentUserChannel;
    }

    public StringProperty currentUserChannelIdProperty() {
        return currentUserChannelId;
    }

    public ObservableList<Channel> getAppChannels() {
        return appChannels;
    }

    public void setCurrentUserChannel(Channel channel) {
        currentUserChannel.set(channel);
        currentUserChannelId.set(channel == null ? "(none)" : channel.getId());
    }

    public void clear() {
        userChannels.clear();
        setCurrentUserChannel(null);
        appChannels.clear();
    }
}
