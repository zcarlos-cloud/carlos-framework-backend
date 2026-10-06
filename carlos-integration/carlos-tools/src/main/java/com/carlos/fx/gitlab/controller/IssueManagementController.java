package com.carlos.fx.gitlab.controller;

import com.carlos.fx.common.controller.BaseController;
import com.carlos.fx.gitlab.entity.GitlabIssue;
import com.carlos.fx.gitlab.service.IssueService;
import com.carlos.fx.utils.AsyncTaskUtil;
import com.carlos.fx.utils.DialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class IssueManagementController extends BaseController {

    @FXML
    private ComboBox<String> projectCombo;

    @FXML
    private ComboBox<String> stateCombo;

    @FXML
    private TextField searchField;

    @FXML
    private Button refreshButton;

    @FXML
    private TableView<IssueItem> issueTable;

    @FXML
    private TableColumn<IssueItem, Boolean> selectColumn;

    @FXML
    private TableColumn<IssueItem, Long> iidColumn;

    @FXML
    private TableColumn<IssueItem, String> titleColumn;

    @FXML
    private TableColumn<IssueItem, String> authorColumn;

    @FXML
    private TableColumn<IssueItem, Date> dateColumn;

    @FXML
    private TableColumn<IssueItem, String> stateColumn;

    @FXML
    private TableColumn<IssueItem, Integer> commentsColumn;

    @FXML
    private Button createButton;

    @FXML
    private Button viewButton;

    @FXML
    private Button closeButton;

    @FXML
    private Button reopenButton;

    @FXML
    private Button assignButton;

    @FXML
    private Button addLabelButton;

    @FXML
    private Button batchCloseButton;

    @FXML
    private ProgressIndicator progressIndicator;

    private IssueService issueService;
    private ObservableList<IssueItem> issues;
    private Long currentProjectId;

    @Override
    protected void initializeComponents() {
        issues = FXCollections.observableArrayList();

        // Initialize state combo
        stateCombo.setItems(FXCollections.observableArrayList(
                "全部", "opened", "closed"
        ));
        stateCombo.getSelectionModel().select(0);

        // Initialize table columns
        selectColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        iidColumn.setCellValueFactory(new PropertyValueFactory<>("iid"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("authorName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        dateColumn.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
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
        stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));
        commentsColumn.setCellValueFactory(new PropertyValueFactory<>("userNotesCount"));

        issueTable.setItems(issues);
        issueTable.setEditable(true);

        // Hide progress indicator initially
        progressIndicator.setVisible(false);

        // Initialize project combo
        projectCombo.setItems(FXCollections.observableArrayList(
                "Project 1", "Project 2", "Project 3"
        ));
        if (!projectCombo.getItems().isEmpty()) {
            projectCombo.getSelectionModel().select(0);
        }
    }

    @Override
    protected void setupEventHandlers() {
        refreshButton.setOnAction(e -> loadIssues());
        createButton.setOnAction(e -> createIssue());
        viewButton.setOnAction(e -> viewIssue());
        closeButton.setOnAction(e -> closeIssue());
        reopenButton.setOnAction(e -> reopenIssue());
        assignButton.setOnAction(e -> assignIssue());
        addLabelButton.setOnAction(e -> addLabel());
        batchCloseButton.setOnAction(e -> batchCloseIssues());

        // Enable/disable buttons based on selection
        viewButton.disableProperty().bind(issueTable.getSelectionModel().selectedItemProperty().isNull());
        closeButton.disableProperty().bind(issueTable.getSelectionModel().selectedItemProperty().isNull());
        reopenButton.disableProperty().bind(issueTable.getSelectionModel().selectedItemProperty().isNull());
        assignButton.disableProperty().bind(issueTable.getSelectionModel().selectedItemProperty().isNull());
        addLabelButton.disableProperty().bind(issueTable.getSelectionModel().selectedItemProperty().isNull());

        // Filter on state change
        stateCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadIssues();
            }
        });

        // Search on text change
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterIssues(newVal);
        });
    }

    /**
     * Set issue service
     */
    public void setIssueService(IssueService service) {
        this.issueService = service;
    }

    /**
     * Set current project ID
     */
    public void setCurrentProjectId(Long projectId) {
        this.currentProjectId = projectId;
        loadIssues();
    }

    /**
     * Load issues
     */
    private void loadIssues() {
        if (issueService == null || currentProjectId == null) {
            return;
        }

        String state = stateCombo.getValue();
        if ("全部".equals(state)) {
            state = null;
        }

        progressIndicator.setVisible(true);
        String finalState = state;

        Task<List<GitlabIssue>> task = new Task<>() {
            @Override
            protected List<GitlabIssue> call() throws Exception {
                return issueService.listIssues(currentProjectId, finalState);
            }
        };

        AsyncTaskUtil.execute(task,
                result -> {
                    issues.clear();
                    result.forEach(issue -> issues.add(new IssueItem(issue)));
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("加载成功", "成功加载 " + result.size() + " 个问题");
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("加载错误", "加载问题失败", error);
                }
        );
    }

    /**
     * Filter issues by search text
     */
    private void filterIssues(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            issueTable.setItems(issues);
            return;
        }

        String lowerSearch = searchText.toLowerCase();
        ObservableList<IssueItem> filtered = issues.filtered(issue ->
                issue.getTitle().toLowerCase().contains(lowerSearch) ||
                        issue.getAuthorName().toLowerCase().contains(lowerSearch)
        );

        issueTable.setItems(filtered);
    }

    /**
     * Create issue
     */
    private void createIssue() {
        // TODO: Show create issue dialog
        DialogUtil.showInfo("创建问题", "创建问题功能开发中...");
    }

    /**
     * View issue details
     */
    private void viewIssue() {
        IssueItem selected = issueTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        // TODO: Show issue details dialog
        DialogUtil.showInfo("查看详情", """
            查看问题详情功能开发中...

            Issue #%d: %s""".formatted(selected.getIid(), selected.getTitle()));
    }

    /**
     * Close issue
     */
    private void closeIssue() {
        IssueItem selected = issueTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        boolean confirmed = DialogUtil.showConfirm(
                "确认关闭",
                "确定要关闭问题 #" + selected.getIid() + " 吗？"
        );

        if (!confirmed) {
            return;
        }

        progressIndicator.setVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                issueService.closeIssue(currentProjectId, selected.getIid());
                return null;
            }
        };

        AsyncTaskUtil.execute(task,
                result -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("关闭成功", "问题已关闭");
                    loadIssues();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("关闭错误", "关闭问题失败", error);
                }
        );
    }

    /**
     * Reopen issue
     */
    private void reopenIssue() {
        IssueItem selected = issueTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        progressIndicator.setVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                issueService.reopenIssue(currentProjectId, selected.getIid());
                return null;
            }
        };

        AsyncTaskUtil.execute(task,
                result -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("重新打开成功", "问题已重新打开");
                    loadIssues();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("重新打开错误", "重新打开问题失败", error);
                }
        );
    }

    /**
     * Assign issue
     */
    private void assignIssue() {
        // TODO: Show assign user dialog
        DialogUtil.showInfo("分配问题", "分配问题功能开发中...");
    }

    /**
     * Add label
     */
    private void addLabel() {
        // TODO: Show add label dialog
        DialogUtil.showInfo("添加标签", "添加标签功能开发中...");
    }

    /**
     * Batch close issues
     */
    private void batchCloseIssues() {
        List<IssueItem> selected = issues.stream()
                .filter(IssueItem::isSelected)
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            DialogUtil.showWarning("未选择问题", "请至少选择一个问题进行关闭！");
            return;
        }

        boolean confirmed = DialogUtil.showConfirm(
                "确认批量关闭",
                "确定要关闭 " + selected.size() + " 个问题吗？"
        );

        if (!confirmed) {
            return;
        }

        progressIndicator.setVisible(true);

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                List<Long> issueIids = selected.stream()
                        .map(IssueItem::getIid)
                        .collect(Collectors.toList());
                List<Long> closed = issueService.batchCloseIssues(currentProjectId, issueIids);
                return closed.size();
            }
        };

        AsyncTaskUtil.execute(task,
                count -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("批量关闭成功", "成功关闭 " + count + " 个问题");
                    loadIssues();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("批量关闭错误", "批量关闭问题失败", error);
                }
        );
    }

    /**
     * Issue Item for table view
     */
    public static class IssueItem {
        private final javafx.beans.property.BooleanProperty selected;
        private final Long iid;
        private final String title;
        private final String authorName;
        private final Date createdAt;
        private final String state;
        private final Integer userNotesCount;

        public IssueItem(GitlabIssue issue) {
            this.selected = new javafx.beans.property.SimpleBooleanProperty(false);
            this.iid = issue.getIid();
            this.title = issue.getTitle();
            this.authorName = issue.getAuthorName();
            this.createdAt = issue.getCreatedAt();
            this.state = issue.getState();
            this.userNotesCount = issue.getUserNotesCount();
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

        public Long getIid() {
            return iid;
        }

        public String getTitle() {
            return title;
        }

        public String getAuthorName() {
            return authorName;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public String getState() {
            return state;
        }

        public Integer getUserNotesCount() {
            return userNotesCount;
        }
    }
}
