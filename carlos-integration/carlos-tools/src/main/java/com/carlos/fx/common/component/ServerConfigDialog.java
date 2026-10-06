package com.carlos.fx.common.component;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Server Configuration Dialog
 *
 * @author Carlos
 * @since 3.0.0
 */
public class ServerConfigDialog {

    private final Stage stage;
    private final TextField serverUrlField;
    private final TextField tokenField;
    private final TextField nameField;
    private final TextArea descriptionArea;
    private boolean confirmed = false;

    public ServerConfigDialog(Window owner) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("服务器配置");
        stage.setResizable(false);

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        // Server URL
        Label urlLabel = new Label("服务器地址:");
        urlLabel.setStyle("-fx-font-weight: bold;");
        serverUrlField = new TextField();
        serverUrlField.setPromptText("http://gitlab.example.com");
        serverUrlField.setPrefWidth(400);

        // Token
        Label tokenLabel = new Label("访问令牌:");
        tokenLabel.setStyle("-fx-font-weight: bold;");
        tokenField = new TextField();
        tokenField.setPromptText("your-gitlab-token");
        tokenField.setPrefWidth(400);

        // Name
        Label nameLabel = new Label("名称:");
        nameLabel.setStyle("-fx-font-weight: bold;");
        nameField = new TextField();
        nameField.setPromptText("My GitLab Server");
        nameField.setPrefWidth(400);

        // Description
        Label descLabel = new Label("描述:");
        descLabel.setStyle("-fx-font-weight: bold;");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("服务器描述（可选）");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setPrefWidth(400);

        // Add to grid
        grid.add(urlLabel, 0, 0);
        grid.add(serverUrlField, 0, 1);
        grid.add(tokenLabel, 0, 2);
        grid.add(tokenField, 0, 3);
        grid.add(nameLabel, 0, 4);
        grid.add(nameField, 0, 5);
        grid.add(descLabel, 0, 6);
        grid.add(descriptionArea, 0, 7);

        // Buttons
        Button okButton = new Button("确定");
        okButton.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-padding: 8px 20px;");
        okButton.setOnAction(e -> {
            if (validate()) {
                confirmed = true;
                stage.close();
            }
        });

        Button cancelButton = new Button("取消");
        cancelButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-padding: 8px 20px;");
        cancelButton.setOnAction(e -> {
            confirmed = false;
            stage.close();
        });

        HBox buttonBox = new HBox(10, okButton, cancelButton);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        // Main layout
        VBox vbox = new VBox(15, grid, buttonBox);
        vbox.setPadding(new Insets(20));

        Scene scene = new Scene(vbox);
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        stage.setScene(scene);
    }

    /**
     * Validate form
     */
    private boolean validate() {
        if (serverUrlField.getText().trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("验证错误");
            alert.setHeaderText(null);
            alert.setContentText("请输入服务器地址！");
            alert.showAndWait();
            return false;
        }

        if (tokenField.getText().trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("验证错误");
            alert.setHeaderText(null);
            alert.setContentText("请输入访问令牌！");
            alert.showAndWait();
            return false;
        }

        if (nameField.getText().trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("验证错误");
            alert.setHeaderText(null);
            alert.setContentText("请输入服务器名称！");
            alert.showAndWait();
            return false;
        }

        return true;
    }

    /**
     * Show dialog and wait
     */
    public boolean showAndWait() {
        stage.showAndWait();
        return confirmed;
    }

    /**
     * Get server URL
     */
    public String getServerUrl() {
        return serverUrlField.getText().trim();
    }

    /**
     * Get token
     */
    public String getToken() {
        return tokenField.getText().trim();
    }

    /**
     * Get name
     */
    public String getName() {
        return nameField.getText().trim();
    }

    /**
     * Get description
     */
    public String getDescription() {
        return descriptionArea.getText().trim();
    }

    /**
     * Set values for editing
     */
    public void setValues(String serverUrl, String token, String name, String description) {
        serverUrlField.setText(serverUrl);
        tokenField.setText(token);
        nameField.setText(name);
        descriptionArea.setText(description);
    }
}
