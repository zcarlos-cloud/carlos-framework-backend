package com.carlos.fx.gitlab.controller;

import com.carlos.fx.common.controller.BaseController;
import com.carlos.fx.gitlab.entity.GitlabUser;
import com.carlos.fx.gitlab.service.UserService;
import com.carlos.fx.utils.AsyncTaskUtil;
import com.carlos.fx.utils.DialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserManagementController extends BaseController {

    @FXML
    private TextField searchField;

    @FXML
    private Button refreshButton;

    @FXML
    private TableView<UserItem> userTable;

    @FXML
    private TableColumn<UserItem, Boolean> selectColumn;

    @FXML
    private TableColumn<UserItem, Long> idColumn;

    @FXML
    private TableColumn<UserItem, String> usernameColumn;

    @FXML
    private TableColumn<UserItem, String> nameColumn;

    @FXML
    private TableColumn<UserItem, String> emailColumn;

    @FXML
    private TableColumn<UserItem, String> stateColumn;

    @FXML
    private TableColumn<UserItem, Date> createdAtColumn;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button blockButton;

    @FXML
    private Button unblockButton;

    @FXML
    private Button importButton;

    @FXML
    private Button exportButton;

    @FXML
    private Button batchDeleteButton;

    @FXML
    private ProgressIndicator progressIndicator;

    private UserService userService;
    private ObservableList<UserItem> users;

    @Override
    protected void initializeComponents() {
        users = FXCollections.observableArrayList();

        // Initialize table columns
        selectColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtColumn.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.systemDefault());

            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item.toInstant()));
                }
            }
        });

        userTable.setItems(users);
        userTable.setEditable(true);

        // Hide progress indicator initially
        progressIndicator.setVisible(false);
    }

    @Override
    protected void setupEventHandlers() {
        refreshButton.setOnAction(e -> loadUsers());
        addButton.setOnAction(e -> addUser());
        editButton.setOnAction(e -> editUser());
        deleteButton.setOnAction(e -> deleteUser());
        blockButton.setOnAction(e -> blockUser());
        unblockButton.setOnAction(e -> unblockUser());
        importButton.setOnAction(e -> importUsers());
        exportButton.setOnAction(e -> exportUsers());
        batchDeleteButton.setOnAction(e -> batchDeleteUsers());

        // Enable/disable buttons based on selection
        editButton.disableProperty().bind(userTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(userTable.getSelectionModel().selectedItemProperty().isNull());
        blockButton.disableProperty().bind(userTable.getSelectionModel().selectedItemProperty().isNull());
        unblockButton.disableProperty().bind(userTable.getSelectionModel().selectedItemProperty().isNull());

        // Search on text change
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterUsers(newVal);
        });
    }

    /**
     * Set user service
     */
    public void setUserService(UserService service) {
        this.userService = service;
        loadUsers();
    }

    /**
     * Load users
     */
    private void loadUsers() {
        if (userService == null) {
            return;
        }

        progressIndicator.setVisible(true);

        Task<List<GitlabUser>> task = new Task<>() {
            @Override
            protected List<GitlabUser> call() throws Exception {
                return userService.listUsers();
            }
        };

        AsyncTaskUtil.execute(task,
                result -> {
                    users.clear();
                    result.forEach(user -> users.add(new UserItem(user)));
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("加载成功", "成功加载 " + result.size() + " 个用户");
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("加载错误", "加载用户失败", error);
                }
        );
    }

    /**
     * Filter users by search text
     */
    private void filterUsers(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            userTable.setItems(users);
            return;
        }

        String lowerSearch = searchText.toLowerCase();
        ObservableList<UserItem> filtered = users.filtered(user ->
                user.getUsername().toLowerCase().contains(lowerSearch) ||
                        user.getName().toLowerCase().contains(lowerSearch) ||
                        user.getEmail().toLowerCase().contains(lowerSearch)
        );

        userTable.setItems(filtered);
    }

    /**
     * Add user
     */
    private void addUser() {
        // TODO: Show add user dialog
        DialogUtil.showInfo("添加用户", "添加用户功能开发中...");
    }

    /**
     * Edit user
     */
    private void editUser() {
        UserItem selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        // TODO: Show edit user dialog
        DialogUtil.showInfo("编辑用户", """
            编辑用户功能开发中...

            用户: %s""".formatted(selected.getUsername()));
    }

    /**
     * Delete user
     */
    private void deleteUser() {
        UserItem selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        boolean confirmed = DialogUtil.showConfirm(
                "确认删除",
            """
                确定要删除用户 %s 吗？

                此操作不可恢复！""".formatted(selected.getUsername())
        );

        if (!confirmed) {
            return;
        }

        progressIndicator.setVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userService.deleteUser(selected.getId());
                return null;
            }
        };

        AsyncTaskUtil.execute(task,
                result -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("删除成功", "用户已删除");
                    loadUsers();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("删除错误", "删除用户失败", error);
                }
        );
    }

    /**
     * Block user
     */
    private void blockUser() {
        UserItem selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        progressIndicator.setVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userService.blockUser(selected.getId());
                return null;
            }
        };

        AsyncTaskUtil.execute(task,
                result -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("封禁成功", "用户已封禁");
                    loadUsers();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("封禁错误", "封禁用户失败", error);
                }
        );
    }

    /**
     * Unblock user
     */
    private void unblockUser() {
        UserItem selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        progressIndicator.setVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userService.unblockUser(selected.getId());
                return null;
            }
        };

        AsyncTaskUtil.execute(task,
                result -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("解封成功", "用户已解封");
                    loadUsers();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("解封错误", "解封用户失败", error);
                }
        );
    }

    /**
     * Import users from Excel
     */
    private void importUsers() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showOpenDialog(importButton.getScene().getWindow());
        if (file == null) {
            return;
        }

        // TODO: Parse Excel and import users
        DialogUtil.showInfo("导入用户", """
            从Excel导入用户功能开发中...

            文件: %s""".formatted(file.getName()));
    }

    /**
     * Export users to Excel
     */
    private void exportUsers() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存Excel文件");
        fileChooser.setInitialFileName("gitlab_users.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file == null) {
            return;
        }

        // TODO: Export users to Excel
        DialogUtil.showInfo("导出用户", """
            导出用户到Excel功能开发中...

            文件: %s""".formatted(file.getName()));
    }

    /**
     * Batch delete users
     */
    private void batchDeleteUsers() {
        List<UserItem> selected = users.stream()
                .filter(UserItem::isSelected)
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            DialogUtil.showWarning("未选择用户", "请至少选择一个用户进行删除！");
            return;
        }

        boolean confirmed = DialogUtil.showConfirm(
                "确认批量删除",
            """
                确定要删除 %d 个用户吗？

                此操作不可恢复！""".formatted(selected.size())
        );

        if (!confirmed) {
            return;
        }

        progressIndicator.setVisible(true);

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                List<Long> userIds = selected.stream()
                        .map(UserItem::getId)
                        .collect(Collectors.toList());
                List<Long> deleted = userService.batchDeleteUsers(userIds);
                return deleted.size();
            }
        };

        AsyncTaskUtil.execute(task,
                count -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("批量删除成功", "成功删除 " + count + " 个用户");
                    loadUsers();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("批量删除错误", "批量删除用户失败", error);
                }
        );
    }

    /**
     * User Item for table view
     */
    public static class UserItem {
        private final javafx.beans.property.BooleanProperty selected;
        private final Long id;
        private final String username;
        private final String name;
        private final String email;
        private final String state;
        private final Date createdAt;

        public UserItem(GitlabUser user) {
            this.selected = new javafx.beans.property.SimpleBooleanProperty(false);
            this.id = user.getId();
            this.username = user.getUsername();
            this.name = user.getName();
            this.email = user.getEmail();
            this.state = user.getState();
            this.createdAt = user.getCreatedAt();
        }

        public boolean isSelected() {
            return selected.get();
        }

        public javafx.beans.property.BooleanProperty selectedProperty() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        public Long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getState() {
            return state;
        }

        public Date getCreatedAt() {
            return createdAt;
        }
    }
}
