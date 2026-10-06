package com.carlos.fx.common.component;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Progress Dialog for long-running tasks
 *
 * @author Carlos
 * @since 3.0.0
 */
public class ProgressDialog {

    private final Stage stage;
    private final Label messageLabel;
    private final ProgressBar progressBar;
    private final ProgressIndicator progressIndicator;

    public ProgressDialog(Window owner, String title) {
        stage = new Stage();
        stage.initStyle(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle(title);
        stage.setResizable(false);

        // Create UI
        VBox vbox = new VBox(15);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        vbox.setPrefWidth(400);

        messageLabel = new Label("Processing...");
        messageLabel.setStyle("-fx-font-size: 14px;");

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(350);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);

        vbox.getChildren().addAll(messageLabel, progressBar, progressIndicator);

        Scene scene = new Scene(vbox);
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        stage.setScene(scene);
    }

    /**
     * Bind to task
     */
    public <T> void bindToTask(Task<T> task) {
        progressBar.progressProperty().bind(task.progressProperty());
        messageLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> stage.close());
        task.setOnFailed(e -> stage.close());
        task.setOnCancelled(e -> stage.close());
    }

    /**
     * Set message
     */
    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    /**
     * Set progress
     */
    public void setProgress(double progress) {
        progressBar.setProgress(progress);
    }

    /**
     * Set indeterminate progress
     */
    public void setIndeterminate(boolean indeterminate) {
        if (indeterminate) {
            progressBar.setVisible(false);
            progressIndicator.setVisible(true);
        } else {
            progressBar.setVisible(true);
            progressIndicator.setVisible(false);
        }
    }

    /**
     * Show dialog
     */
    public void show() {
        stage.show();
    }

    /**
     * Show and wait
     */
    public void showAndWait() {
        stage.showAndWait();
    }

    /**
     * Close dialog
     */
    public void close() {
        stage.close();
    }

    /**
     * Get stage
     */
    public Stage getStage() {
        return stage;
    }
}
