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

import org.finos.fdc3.workbench.model.LogEntry;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Chronological system log shared by all workbench panels.
 */
public class SystemLogService {

    private final ObservableList<LogEntry> entries = FXCollections.observableArrayList();

    public ObservableList<LogEntry> getEntries() {
        return entries;
    }

    public void info(String type, String message) {
        append(new LogEntry(type, message));
    }

    public void info(String type, String message, String body) {
        append(new LogEntry(type, message, body));
    }

    public void error(String type, String message) {
        append(new LogEntry(type, "ERROR: " + message));
    }

    private void append(LogEntry entry) {
        if (Platform.isFxApplicationThread()) {
            entries.add(0, entry);
        } else {
            Platform.runLater(() -> entries.add(0, entry));
        }
    }
}
