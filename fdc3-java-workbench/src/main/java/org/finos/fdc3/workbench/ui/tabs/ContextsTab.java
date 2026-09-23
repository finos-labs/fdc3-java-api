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

package org.finos.fdc3.workbench.ui.tabs;

import org.finos.fdc3.workbench.model.ContextTemplate;
import org.finos.fdc3.workbench.service.WorkbenchServices;
import org.finos.fdc3.workbench.ui.common.JsonEditorPane;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Context template library (CRUD + JSON editor).
 */
public class ContextsTab extends BorderPane {

    private final WorkbenchServices services;
    private final ListView<ContextTemplate> list = new ListView<>();
    private final TextField nameField = new TextField();
    private final JsonEditorPane editor;

    public ContextsTab(WorkbenchServices services) {
        this.services = services;
        this.editor = new JsonEditorPane(services.getMapper());

        list.setItems(services.getContexts().getTemplates());
        list.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected != null) {
                nameField.setText(selected.getId());
                editor.setJson(selected.getTemplate());
            }
        });

        Button create = new Button("New");
        create.setOnAction(e -> createTemplate());
        Button save = new Button("Save");
        save.setOnAction(e -> saveSelected());
        Button duplicate = new Button("Duplicate");
        duplicate.setOnAction(e -> duplicateSelected());
        Button delete = new Button("Delete");
        delete.setOnAction(e -> deleteSelected());
        Button reset = new Button("Reset Defaults");
        reset.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Replace all templates with shipped defaults?", ButtonType.OK, ButtonType.CANCEL);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    services.getContexts().resetToDefaults();
                }
            });
        });

        HBox actions = new HBox(8, create, save, duplicate, delete, reset);
        actions.setPadding(new Insets(0, 0, 8, 0));

        VBox form = new VBox(8,
                new Label("Name"), nameField,
                new Label("Context JSON"), editor);
        VBox.setVgrow(editor, Priority.ALWAYS);

        VBox left = new VBox(8, new Label("Templates"), list, actions);
        VBox.setVgrow(list, Priority.ALWAYS);
        left.setPrefWidth(280);
        left.setPadding(new Insets(8));

        form.setPadding(new Insets(8));
        setLeft(left);
        setCenter(form);
        getStyleClass().add("panel-card");

        if (!list.getItems().isEmpty()) {
            list.getSelectionModel().selectFirst();
        }
    }

    private void createTemplate() {
        try {
            var node = services.getMapper().readTree("{\"type\":\"fdc3.instrument\",\"id\":{\"ticker\":\"AAPL\"}}");
            ContextTemplate created = new ContextTemplate("New context", node);
            services.getContexts().add(created);
            list.getSelectionModel().select(created);
        } catch (Exception e) {
            services.getLog().error("contexts", e.getMessage());
        }
    }

    private void saveSelected() {
        ContextTemplate selected = list.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            selected.setId(nameField.getText().trim());
            selected.setTemplate(editor.getJson());
            services.getContexts().update(selected);
            list.refresh();
            services.getLog().info("contexts", "Saved template: " + selected.getId());
        } catch (Exception e) {
            services.getLog().error("contexts", e.getMessage());
        }
    }

    private void duplicateSelected() {
        ContextTemplate selected = list.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        ContextTemplate copy = new ContextTemplate(selected.getId() + " copy", selected.getTemplate().deepCopy());
        services.getContexts().add(copy);
        list.getSelectionModel().select(copy);
    }

    private void deleteSelected() {
        ContextTemplate selected = list.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        services.getContexts().remove(selected);
    }
}
