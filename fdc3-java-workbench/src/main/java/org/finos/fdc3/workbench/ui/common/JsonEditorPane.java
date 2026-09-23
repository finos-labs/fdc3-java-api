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

package org.finos.fdc3.workbench.ui.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * Simple JSON text editor with format / validate helpers.
 */
public class JsonEditorPane extends BorderPane {

    private final TextArea editor = new TextArea();
    private final Label status = new Label();
    private final ObjectMapper mapper;

    public JsonEditorPane(ObjectMapper mapper) {
        this.mapper = mapper;
        editor.setWrapText(true);
        editor.getStyleClass().add("log-view");
        Button format = new Button("Format");
        format.setOnAction(e -> formatJson());
        HBox toolbar = new HBox(8, format, status);
        toolbar.setPadding(new Insets(0, 0, 6, 0));
        setTop(toolbar);
        setCenter(editor);
        setPrefHeight(220);
    }

    public TextArea getEditor() {
        return editor;
    }

    public void setJson(JsonNode node) {
        try {
            editor.setText(node == null ? "" : mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
            status.setText("");
        } catch (Exception e) {
            editor.setText(String.valueOf(node));
            status.setText(e.getMessage());
        }
    }

    public JsonNode getJson() throws Exception {
        String text = editor.getText();
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("JSON is empty");
        }
        return mapper.readTree(text);
    }

    private void formatJson() {
        try {
            JsonNode node = getJson();
            setJson(node);
            status.setText("OK");
        } catch (Exception e) {
            status.setText("Invalid JSON: " + e.getMessage());
        }
    }
}
