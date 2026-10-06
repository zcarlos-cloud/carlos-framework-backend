package com.carlos.fx.common.component;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * Reusable Database Connection Panel
 *
 * @author Carlos
 * @since 3.0.0
 */
public class DatabaseConnectionPanel extends GridPane {

    private final ComboBox<String> databaseTypeCombo;
    private final TextField hostField;
    private final TextField portField;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final TextField databaseField;

    public DatabaseConnectionPanel() {
        // Initialize components
        databaseTypeCombo = new ComboBox<>();
        hostField = new TextField();
        portField = new TextField();
        usernameField = new TextField();
        passwordField = new PasswordField();
        databaseField = new TextField();

        // Set default values
        hostField.setText("localhost");
        portField.setText("3306");

        // Set prompts
        hostField.setPromptText("localhost");
        portField.setPromptText("3306");
        usernameField.setPromptText("root");
        passwordField.setPromptText("password");
        databaseField.setPromptText("database_name");

        // Setup layout
        setupLayout();
    }

    private void setupLayout() {
        setHgap(15);
        setVgap(12);
        setPadding(new Insets(10));

        // Row 0
        add(new Label("数据库类型:"), 0, 0);
        add(databaseTypeCombo, 1, 0);
        add(new Label("主机地址:"), 2, 0);
        add(hostField, 3, 0);

        // Row 1
        add(new Label("端口:"), 0, 1);
        add(portField, 1, 1);
        add(new Label("数据库名:"), 2, 1);
        add(databaseField, 3, 1);

        // Row 2
        add(new Label("用户名:"), 0, 2);
        add(usernameField, 1, 2);
        add(new Label("密码:"), 2, 2);
        add(passwordField, 3, 2);

        // Make fields grow
        hostField.setPrefWidth(200);
        portField.setPrefWidth(200);
        usernameField.setPrefWidth(200);
        passwordField.setPrefWidth(200);
        databaseField.setPrefWidth(200);
        databaseTypeCombo.setPrefWidth(200);
    }

    public ComboBox<String> getDatabaseTypeCombo() {
        return databaseTypeCombo;
    }

    public TextField getHostField() {
        return hostField;
    }

    public TextField getPortField() {
        return portField;
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public TextField getDatabaseField() {
        return databaseField;
    }

    /**
     * Validate all fields
     */
    public boolean validate() {
        return !hostField.getText().trim().isEmpty()
                && !portField.getText().trim().isEmpty()
                && !usernameField.getText().trim().isEmpty()
                && !databaseField.getText().trim().isEmpty();
    }

    /**
     * Clear all fields
     */
    public void clear() {
        hostField.clear();
        portField.clear();
        usernameField.clear();
        passwordField.clear();
        databaseField.clear();
        databaseTypeCombo.getSelectionModel().clearSelection();
    }
}
