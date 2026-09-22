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

package org.finos.fdc3.workbench.ui.workbench;

import org.finos.fdc3.workbench.model.LogEntry;
import org.finos.fdc3.workbench.service.WorkbenchServices;

import javafx.geometry.Insets;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Chronological system log.
 */
public class SystemLogPane extends VBox {

    public SystemLogPane(WorkbenchServices services) {
        setPadding(new Insets(8));
        ListView<LogEntry> list = new ListView<>(services.getLog().getEntries());
        list.getStyleClass().add("log-view");
        VBox.setVgrow(list, Priority.ALWAYS);
        getChildren().add(list);
    }
}
