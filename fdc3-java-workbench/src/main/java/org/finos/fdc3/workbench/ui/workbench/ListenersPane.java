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

import org.finos.fdc3.workbench.model.ListenerRecord;
import org.finos.fdc3.workbench.service.WorkbenchServices;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Right-hand listener lists (context / intent / app / private).
 */
public class ListenersPane extends VBox {

    private final WorkbenchServices services;

    public ListenersPane(WorkbenchServices services) {
        this.services = services;
        setSpacing(8);
        setPadding(new Insets(8));

        getChildren().addAll(
                section("Context Listeners", services.getListeners().getContextListeners()),
                section("Intent Listeners", services.getListeners().getIntentListeners()),
                section("App Channel Listeners", services.getListeners().getAppChannelListeners()),
                section("Private Channel Listeners", services.getListeners().getPrivateChannelListeners()));
    }

    private TitledPane section(String title, javafx.collections.ObservableList<ListenerRecord> items) {
        ListView<ListenerRecord> list = new ListView<>(items);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ListenerRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label label = new Label();
                label.textProperty().bind(Bindings.createStringBinding(
                        () -> item + "\nlast: " + abbreviate(item.lastReceivedProperty().get()),
                        item.lastReceivedProperty()));
                Button remove = new Button("Remove");
                remove.setOnAction(e -> services.getListeners().remove(item));
                BorderPane row = new BorderPane();
                row.setCenter(label);
                row.setRight(remove);
                row.getStyleClass().add("listener-item");
                setGraphic(row);
            }
        });
        list.setPrefHeight(140);
        TitledPane pane = new TitledPane(title, list);
        pane.setExpanded(true);
        VBox.setVgrow(pane, Priority.ALWAYS);
        return pane;
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        String flat = value.replace('\n', ' ');
        return flat.length() > 120 ? flat.substring(0, 117) + "..." : flat;
    }
}
