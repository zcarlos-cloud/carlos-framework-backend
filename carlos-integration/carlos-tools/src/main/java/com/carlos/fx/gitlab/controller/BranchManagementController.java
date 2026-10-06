package com.carlos.fx.gitlab.controller;

import com.carlos.fx.common.controller.BaseController;
import com.carlos.fx.gitlab.entity.GitlabBranch;
import com.carlos.fx.gitlab.service.BranchService;
import com.carlos.fx.utils.AsyncTaskUtil;
import com.carlos.fx.utils.DialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BranchManagementController extends BaseController {

    // ==================== FXML 注入的 UI 组件 ====================

    /**
     * 项目下拉框
     * 用于选择要管理的 GitLab 项目
     * 对应 FXML 中的 fx:id="projectCombo"
     */
    @FXML
    private ComboBox<String> projectCombo;

    /**
     * 搜索输入框
     * 用于实时过滤分支列表，支持按分支名称和提交信息搜索
     * 对应 FXML 中的 fx:id="searchField"
     */
    @FXML
    private TextField searchField;

    /**
     * 刷新按钮
     * 点击后重新从 GitLab 服务器加载分支列表
     * 对应 FXML 中的 fx:id="refreshButton"
     */
    @FXML
    private Button refreshButton;

    /**
     * 分支表格
     * 显示所有分支的详细信息（名称、保护状态、最后提交等）
     * 对应 FXML 中的 fx:id="branchTable"
     */
    @FXML
    private TableView<BranchItem> branchTable;

    /**
     * 选择列
     * 复选框列，用于批量选择分支进行操作
     * 对应 FXML 中的 fx:id="selectColumn"
     */
    @FXML
    private TableColumn<BranchItem, Boolean> selectColumn;

    /**
     * 分支名称列
     * 显示分支的名称
     * 对应 FXML 中的 fx:id="nameColumn"
     */
    @FXML
    private TableColumn<BranchItem, String> nameColumn;

    /**
     * 受保护状态列
     * 显示分支是否受保护（受保护的分支不能被强制推送或删除）
     * 对应 FXML 中的 fx:id="protectedColumn"
     */
    @FXML
    private TableColumn<BranchItem, Boolean> protectedColumn;

    /**
     * 提交信息列
     * 显示分支最后一次提交的简短 ID 和标题
     * 对应 FXML 中的 fx:id="commitColumn"
     */
    @FXML
    private TableColumn<BranchItem, String> commitColumn;

    /**
     * 提交时间列
     * 显示分支最后一次提交的时间
     * 对应 FXML 中的 fx:id="dateColumn"
     */
    @FXML
    private TableColumn<BranchItem, Date> dateColumn;

    /**
     * 创建分支按钮
     * 点击后弹出对话框创建新分支
     * 对应 FXML 中的 fx:id="createButton"
     */
    @FXML
    private Button createButton;

    /**
     * 删除选中分支按钮
     * 删除表格中选中的分支（受保护的分支会被跳过）
     * 对应 FXML 中的 fx:id="deleteButton"
     */
    @FXML
    private Button deleteButton;

    /**
     * 保护分支按钮
     * 将选中的分支设置为受保护状态
     * 对应 FXML 中的 fx:id="protectButton"
     */
    @FXML
    private Button protectButton;

    /**
     * 取消保护按钮
     * 取消选中分支的保护状态
     * 对应 FXML 中的 fx:id="unprotectButton"
     */
    @FXML
    private Button unprotectButton;

    /**
     * 比较分支按钮
     * 比较两个分支之间的差异
     * 对应 FXML 中的 fx:id="compareButton"
     */
    @FXML
    private Button compareButton;

    /**
     * 删除已合并分支按钮
     * 批量删除已经合并到主分支的分支
     * 对应 FXML 中的 fx:id="deleteMergedButton"
     */
    @FXML
    private Button deleteMergedButton;

    /**
     * 进度指示器
     * 显示加载状态的旋转动画
     * 对应 FXML 中的 fx:id="progressIndicator"
     */
    @FXML
    private ProgressIndicator progressIndicator;

    // ==================== 业务逻辑字段 ====================

    /**
     * 分支服务
     * 用于调用 GitLab API 进行分支操作
     */
    private BranchService branchService;

    /**
     * 分支列表
     * ObservableList 是 JavaFX 的可观察列表，当列表内容变化时会自动更新 UI
     */
    private ObservableList<BranchItem> branches;

    /**
     * 当前选中的项目 ID
     */
    private Long currentProjectId;

    /**
     * 初始化组件
     * 该方法在 FXML 加载完成后自动调用，用于初始化 UI 组件的状态
     */
    @Override
    protected void initializeComponents() {
        // 创建可观察列表，用于存储分支数据
        branches = FXCollections.observableArrayList();

        // ==================== 初始化表格列 ====================

        // 选择列：使用 PropertyValueFactory 绑定到 BranchItem 的 selected 属性
        selectColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        // 使用复选框单元格工厂，使该列显示为可勾选的复选框
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        // 设置该列可编辑（允许用户勾选/取消勾选）
        selectColumn.setEditable(true);

        // 分支名称列：绑定到 BranchItem 的 name 属性
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // 受保护状态列：绑定到 BranchItem 的 protectedBranch 属性
        protectedColumn.setCellValueFactory(new PropertyValueFactory<>("protectedBranch"));

        // 提交信息列：使用自定义的 cellValueFactory 组合显示提交 ID 和标题
        commitColumn.setCellValueFactory(cellData -> {
            // 获取提交的短 ID 和标题，组合成 "shortId - title" 格式
            String commit = cellData.getValue().getCommitShortId() + " - " + cellData.getValue().getCommitTitle();
            // 返回 SimpleStringProperty 对象，JavaFX 会自动监听其变化
            return new javafx.beans.property.SimpleStringProperty(commit);
        });

        // 提交时间列：绑定到 BranchItem 的 commitCreatedAt 属性
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("commitCreatedAt"));
        // 使用自定义的单元格工厂格式化日期显示
        dateColumn.setCellFactory(column -> new TableCell<>() {
            // 创建线程安全的日期格式化器
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

            /**
             * 更新单元格内容
             * @param item 日期对象
             * @param empty 是否为空单元格
             */
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // 格式化日期并显示
                    setText(formatter.format(item.toInstant()));
                }
            }
        });

        // 将分支列表绑定到表格
        branchTable.setItems(branches);
        // 设置表格可编辑（允许勾选复选框）
        branchTable.setEditable(true);

        // 初始时隐藏进度指示器
        progressIndicator.setVisible(false);

        // ==================== 初始化项目下拉框 ====================
        // TODO: 这里应该从配置文件或 GitLab API 加载实际的项目列表
        // 示例：调用 GitLabApi.getProjectApi().getProjects() 获取项目列表
        projectCombo.setItems(FXCollections.observableArrayList(
                "Project 1", "Project 2", "Project 3"
        ));
        // 默认选中第一个项目
        if (!projectCombo.getItems().isEmpty()) {
            projectCombo.getSelectionModel().select(0);
        }
    }

    /**
     * 设置事件处理器
     * 该方法在 initializeComponents 之后调用，用于绑定 UI 事件
     */
    @Override
    protected void setupEventHandlers() {
        // ==================== 按钮点击事件 ====================

        // 刷新按钮：点击时重新加载分支列表
        // setOnAction 方法用于设置按钮的点击事件处理器
        // e 是 ActionEvent 对象，包含事件信息
        refreshButton.setOnAction(e -> loadBranches());

        // 创建分支按钮：点击时弹出创建分支对话框
        createButton.setOnAction(e -> createBranch());

        // 删除选中分支按钮：点击时删除表格中选中的分支
        deleteButton.setOnAction(e -> deleteSelectedBranches());

        // 保护分支按钮：点击时保护选中的分支
        protectButton.setOnAction(e -> protectSelectedBranches());

        // 取消保护按钮：点击时取消选中分支的保护状态
        unprotectButton.setOnAction(e -> unprotectSelectedBranches());

        // 比较分支按钮：点击时比较两个分支的差异
        compareButton.setOnAction(e -> compareBranches());

        // 删除已合并分支按钮：点击时删除所有已合并的分支
        deleteMergedButton.setOnAction(e -> deleteMergedBranches());

        // ==================== 搜索框文本变化监听 ====================

        // 监听搜索框的文本变化，实现实时搜索功能
        // textProperty() 返回一个 StringProperty 对象，可以监听其变化
        // addListener 添加监听器，当文本变化时自动调用
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            // obs: Observable 对象（textProperty）
            // oldVal: 旧的文本值
            // newVal: 新的文本值
            filterBranches(newVal);
        });
    }

    /**
     * 设置分支服务
     * 由父控制器调用，注入 BranchService 实例
     *
     * @param service 分支服务实例
     */
    public void setBranchService(BranchService service) {
        this.branchService = service;
    }

    /**
     * 设置当前项目 ID
     * 由父控制器调用，设置要管理的项目
     *
     * @param projectId 项目 ID
     */
    public void setCurrentProjectId(Long projectId) {
        this.currentProjectId = projectId;
        // 设置项目后立即加载分支列表
        loadBranches();
    }

    /**
     * 加载分支列表
     * 从 GitLab 服务器异步加载当前项目的所有分支
     */
    private void loadBranches() {
        // 检查必要的参数是否已设置
        if (branchService == null || currentProjectId == null) {
            return;
        }

        // 显示进度指示器
        progressIndicator.setVisible(true);

        // 创建异步任务
        // Task 是 JavaFX 的异步任务类，用于在后台线程执行耗时操作
        Task<List<GitlabBranch>> task = new Task<>() {
            /**
             * call 方法在后台线程执行
             * @return 分支列表
             */
            @Override
            protected List<GitlabBranch> call() throws Exception {
                // 调用 GitLab API 获取分支列表
                return branchService.listBranches(currentProjectId);
            }
        };

        // 执行异步任务
        // AsyncTaskUtil.execute 方法接受三个参数：
        // 1. task: 要执行的任务
        // 2. onSuccess: 成功回调（在 JavaFX 主线程执行）
        // 3. onError: 失败回调（在 JavaFX 主线程执行）
        AsyncTaskUtil.execute(task,
                result -> {
                    // 成功回调：清空现有列表并添加新数据
                    branches.clear();
                    result.forEach(branch -> branches.add(new BranchItem(branch)));
                    // 隐藏进度指示器
                    progressIndicator.setVisible(false);
                    // 显示成功通知
                    DialogUtil.showSuccessNotification("加载成功", "成功加载 " + result.size() + " 个分支");
                },
                error -> {
                    // 失败回调：隐藏进度指示器并显示错误对话框
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("加载错误", "加载分支失败", error);
                }
        );
    }

    /**
     * 根据搜索文本过滤分支列表
     *
     * @param searchText 搜索文本
     */
    private void filterBranches(String searchText) {
        // 如果搜索文本为空，显示所有分支
        if (searchText == null || searchText.trim().isEmpty()) {
            branchTable.setItems(branches);
            return;
        }

        // 转换为小写以实现不区分大小写的搜索
        String lowerSearch = searchText.toLowerCase();

        // 使用 filtered 方法创建过滤后的列表
        // filtered 返回一个 FilteredList，它会自动监听原列表的变化
        ObservableList<BranchItem> filtered = branches.filtered(branch ->
                // 搜索分支名称或提交标题
                branch.getName().toLowerCase().contains(lowerSearch) ||
                        branch.getCommitTitle().toLowerCase().contains(lowerSearch)
        );

        // 将过滤后的列表设置到表格
        branchTable.setItems(filtered);
    }

    /**
     * 创建分支
     * TODO: 实现创建分支对话框
     *
     * 实现步骤：
     * 1. 创建一个自定义对话框（Dialog）
     * 2. 添加输入字段：分支名称、源分支
     * 3. 验证输入（分支名称不能为空，不能包含特殊字符）
     * 4. 调用 branchService.createBranch(projectId, branchName, ref) 创建分支
     * 5. 创建成功后刷新分支列表
     */
    private void createBranch() {
        // TODO: 显示创建分支对话框
        // 示例代码：
        // TextInputDialog dialog = new TextInputDialog();
        // dialog.setTitle("创建分支");
        // dialog.setHeaderText("请输入新分支名称");
        // dialog.setContentText("分支名称:");
        // Optional<String> result = dialog.showAndWait();
        // result.ifPresent(branchName -> {
        //     // 调用 API 创建分支
        //     branchService.createBranch(currentProjectId, branchName, "main");
        //     loadBranches();
        // });

        DialogUtil.showInfo("创建分支", "创建分支功能开发中...");
    }

    /**
     * 删除选中的分支
     * 批量删除表格中勾选的分支（受保护的分支会被跳过）
     */
    private void deleteSelectedBranches() {
        // 使用 Stream API 过滤出选中的分支
        List<BranchItem> selected = branches.stream()
                .filter(BranchItem::isSelected)  // 只保留 selected 为 true 的分支
                .collect(Collectors.toList());   // 收集到 List

        // 如果没有选中任何分支，显示警告
        if (selected.isEmpty()) {
            DialogUtil.showWarning("未选择分支", "请至少选择一个分支进行删除！");
            return;
        }

        // 显示确认对话框
        boolean confirmed = DialogUtil.showConfirm(
                "确认删除",
            """
                确定要删除 %d 个分支吗？

                注意：受保护的分支将被跳过。""".formatted(selected.size())
        );

        // 如果用户取消，直接返回
        if (!confirmed) {
            return;
        }

        // 显示进度指示器
        progressIndicator.setVisible(true);

        // 创建异步任务执行删除操作
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                int deleted = 0;
                // 遍历选中的分支
                for (BranchItem item : selected) {
                    // 跳过受保护的分支
                    if (!item.isProtectedBranch()) {
                        try {
                            // 调用 API 删除分支
                            branchService.deleteBranch(currentProjectId, item.getName());
                            deleted++;
                        } catch (Exception e) {
                            // 记录错误但继续处理其他分支
                            log.error("Failed to delete branch: {} - {}", item.getName(), e.getMessage());
                        }
                    }
                }
                return deleted;
            }
        };

        // 执行异步任务
        AsyncTaskUtil.execute(task,
                deleted -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("删除成功", "成功删除 " + deleted + " 个分支");
                    // 刷新分支列表
                    loadBranches();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("删除错误", "删除分支失败", error);
                }
        );
    }

    /**
     * 保护选中的分支
     * 将选中的未受保护分支设置为受保护状态
     */
    private void protectSelectedBranches() {
        // 过滤出选中且未受保护的分支
        List<BranchItem> selected = branches.stream()
                .filter(BranchItem::isSelected)
                .filter(item -> !item.isProtectedBranch())  // 只保留未受保护的分支
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            DialogUtil.showWarning("未选择分支", "请至少选择一个未受保护的分支！");
            return;
        }

        progressIndicator.setVisible(true);

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                int protected_count = 0;
                for (BranchItem item : selected) {
                    try {
                        branchService.protectBranch(currentProjectId, item.getName());
                        protected_count++;
                    } catch (Exception e) {
                        log.error("Failed to protect branch: {} - {}", item.getName(), e.getMessage());
                    }
                }
                return protected_count;
            }
        };

        AsyncTaskUtil.execute(task,
                count -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("保护成功", "成功保护 " + count + " 个分支");
                    loadBranches();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("保护错误", "保护分支失败", error);
                }
        );
    }

    /**
     * 取消保护选中的分支
     * 取消选中的受保护分支的保护状态
     */
    private void unprotectSelectedBranches() {
        // 过滤出选中且受保护的分支
        List<BranchItem> selected = branches.stream()
                .filter(BranchItem::isSelected)
                .filter(BranchItem::isProtectedBranch)  // 只保留受保护的分支
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            DialogUtil.showWarning("未选择分支", "请至少选择一个受保护的分支！");
            return;
        }

        progressIndicator.setVisible(true);

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                int unprotected_count = 0;
                for (BranchItem item : selected) {
                    try {
                        branchService.unprotectBranch(currentProjectId, item.getName());
                        unprotected_count++;
                    } catch (Exception e) {
                        log.error("Failed to unprotect branch: {} - {}", item.getName(), e.getMessage());
                    }
                }
                return unprotected_count;
            }
        };

        AsyncTaskUtil.execute(task,
                count -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("取消保护成功", "成功取消保护 " + count + " 个分支");
                    loadBranches();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("取消保护错误", "取消保护分支失败", error);
                }
        );
    }

    /**
     * 比较分支
     * TODO: 实现分支比较对话框
     *
     * 实现步骤：
     * 1. 创建对话框，包含两个下拉框选择要比较的分支
     * 2. 调用 GitLab API 获取两个分支之间的差异
     * 3. 显示差异内容（可以使用 DiffViewer 组件）
     */
    private void compareBranches() {
        // TODO: 显示比较分支对话框
        DialogUtil.showInfo("比较分支", "比较分支功能开发中...");
    }

    /**
     * 删除已合并的分支
     * 批量删除所有已经合并到主分支的分支
     */
    private void deleteMergedBranches() {
        // 显示确认对话框
        boolean confirmed = DialogUtil.showConfirm(
                "确认删除",
            """
                确定要删除所有已合并的分支吗？

                注意：受保护的分支和主分支将被跳过。"""
        );

        if (!confirmed) {
            return;
        }

        progressIndicator.setVisible(true);

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                // 调用 API 删除已合并的分支
                // "main" 是目标分支，可以根据实际情况修改
                return branchService.deleteMergedBranches(currentProjectId, "main");
            }
        };

        AsyncTaskUtil.execute(task,
                deleted -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("删除成功", "成功删除 " + deleted.size() + " 个已合并分支");
                    loadBranches();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("删除错误", "删除已合并分支失败", error);
                }
        );
    }

    /**
     * 分支项
     * 用于在表格中显示分支信息的数据模型
     *
     * 该类包装了 GitlabBranch 实体，并添加了 JavaFX 属性支持
     */
    public static class BranchItem {
        /**
         * 选中状态属性
         * BooleanProperty 是 JavaFX 的可观察属性，当值变化时会自动通知 UI 更新
         */
        private final javafx.beans.property.BooleanProperty selected;

        /**
         * 分支名称
         */
        private final String name;

        /**
         * 是否受保护
         */
        private final Boolean protectedBranch;

        /**
         * 提交的短 ID
         */
        private final String commitShortId;

        /**
         * 提交标题
         */
        private final String commitTitle;

        /**
         * 提交创建时间
         */
        private final Date commitCreatedAt;

        /**
         * 构造函数
         * 从 GitlabBranch 实体创建 BranchItem
         *
         * @param branch GitLab 分支实体
         */
        public BranchItem(GitlabBranch branch) {
            // 初始化选中状态为 false
            this.selected = new javafx.beans.property.SimpleBooleanProperty(false);
            this.name = branch.getName();
            this.protectedBranch = branch.getProtectedBranch();
            this.commitShortId = branch.getCommitShortId();
            this.commitTitle = branch.getCommitTitle();
            this.commitCreatedAt = branch.getCommitCreatedAt();
        }

        // ==================== Getter 和 Setter 方法 ====================

        /**
         * 获取选中状态
         * @return 是否选中
         */
        public boolean isSelected() {
            return selected.get();
        }

        /**
         * 获取选中状态属性
         * JavaFX 使用该方法进行属性绑定
         * @return 选中状态属性
         */
        public javafx.beans.property.BooleanProperty selectedProperty() {
            return selected;
        }

        /**
         * 设置选中状态
         * @param selected 是否选中
         */
        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        public String getName() {
            return name;
        }

        public Boolean isProtectedBranch() {
            return protectedBranch;
        }

        public String getCommitShortId() {
            return commitShortId;
        }

        public String getCommitTitle() {
            return commitTitle;
        }

        public Date getCommitCreatedAt() {
            return commitCreatedAt;
        }
    }
}
