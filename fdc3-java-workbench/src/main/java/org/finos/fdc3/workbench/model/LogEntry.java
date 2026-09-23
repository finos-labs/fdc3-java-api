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

import java.time.Instant;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * One row in the system log / a registered listener summary.
 */
public class LogEntry {

    private final Instant timestamp;
    private final String type;
    private final String message;
    private final String body;

    public LogEntry(String type, String message) {
        this(type, message, null);
    }

    public LogEntry(String type, String message, String body) {
        this.timestamp = Instant.now();
        this.type = type;
        this.message = message;
        this.body = body;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getBody() {
        return body;
    }

    public StringProperty displayProperty() {
        String text = timestamp + " [" + type + "] " + message;
        if (body != null && !body.isBlank()) {
            text = text + "\n" + body;
        }
        return new SimpleStringProperty(text);
    }

    @Override
    public String toString() {
        return displayProperty().get();
    }
}
