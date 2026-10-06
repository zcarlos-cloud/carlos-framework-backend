package com.carlos.fx.common.component;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Code Diff Viewer using TextFlow
 *
 * @author Carlos
 * @since 3.0.0
 */
public class DiffViewer extends VBox {

    private final TextFlow textFlow;
    private final ScrollPane scrollPane;

    public DiffViewer() {
        textFlow = new TextFlow();
        textFlow.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");

        scrollPane = new ScrollPane(textFlow);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);

        getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * Set diff content
     */
    public void setDiff(String diffContent) {
        textFlow.getChildren().clear();

        if (diffContent == null || diffContent.isEmpty()) {
            Text emptyText = new Text("No changes");
            emptyText.setStyle("-fx-fill: #757575;");
            textFlow.getChildren().add(emptyText);
            return;
        }

        String[] lines = diffContent.split("\n");
        for (String line : lines) {
            Text text = new Text(line + "\n");
            text.setFont(Font.font("Consolas", 12));

            // Color code based on diff markers
            if (line.startsWith("+")) {
                text.setStyle("-fx-fill: #22863a; -fx-background-color: #f0fff4;");
            } else if (line.startsWith("-")) {
                text.setStyle("-fx-fill: #cb2431; -fx-background-color: #ffeef0;");
            } else if (line.startsWith("@@")) {
                text.setStyle("-fx-fill: #005cc5; -fx-font-weight: bold;");
            } else if (line.startsWith("diff") || line.startsWith("index") || line.startsWith("---") || line.startsWith("+++")) {
                text.setStyle("-fx-fill: #6a737d;");
            } else {
                text.setStyle("-fx-fill: #24292e;");
            }

            textFlow.getChildren().add(text);
        }
    }

    /**
     * Clear diff content
     */
    public void clear() {
        textFlow.getChildren().clear();
    }

    /**
     * Show diff in modal dialog
     */
    public static void showDiffDialog(Window owner, String title, String diffContent) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle(title);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        DiffViewer diffViewer = new DiffViewer();
        diffViewer.setDiff(diffContent);

        vbox.getChildren().addAll(titleLabel, diffViewer);
        VBox.setVgrow(diffViewer, javafx.scene.layout.Priority.ALWAYS);

        Scene scene = new Scene(vbox, 800, 600);
        scene.getStylesheets().add(DiffViewer.class.getResource("/css/main.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
    }
}
