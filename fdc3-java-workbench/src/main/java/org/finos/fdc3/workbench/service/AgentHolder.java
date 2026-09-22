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

import java.util.Optional;

import org.finos.fdc3.api.DesktopAgent;
import org.finos.fdc3.api.metadata.ImplementationMetadata;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Holds the connected {@link DesktopAgent} and cached {@link ImplementationMetadata}.
 */
public class AgentHolder {

    private final ObjectProperty<DesktopAgent> agent = new SimpleObjectProperty<>();
    private final ObjectProperty<ImplementationMetadata> info = new SimpleObjectProperty<>();
    private final StringProperty statusText = new SimpleStringProperty("Not Connected");
    private final StringProperty statusStyle = new SimpleStringProperty("status-disconnected");

    public ObjectProperty<DesktopAgent> agentProperty() {
        return agent;
    }

    public Optional<DesktopAgent> getAgent() {
        return Optional.ofNullable(agent.get());
    }

    public void setAgent(DesktopAgent desktopAgent) {
        agent.set(desktopAgent);
    }

    public ObjectProperty<ImplementationMetadata> infoProperty() {
        return info;
    }

    public ImplementationMetadata getInfo() {
        return info.get();
    }

    public void setInfo(ImplementationMetadata metadata) {
        info.set(metadata);
    }

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public StringProperty statusStyleProperty() {
        return statusStyle;
    }

    public void setStatus(String text, String styleClass) {
        statusText.set(text);
        statusStyle.set(styleClass);
    }

    public boolean isConnected() {
        return agent.get() != null;
    }
}
