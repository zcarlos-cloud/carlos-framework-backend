package com.carlos.fx.gitlab.controller;

import com.carlos.fx.common.controller.BaseController;
import com.carlos.fx.gitlab.entity.GitlabMergeRequest;
import com.carlos.fx.gitlab.service.MergeRequestService;
import com.carlos.fx.utils.AsyncTaskUtil;
import com.carlos.fx.utils.DialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Component
public class MergeRequestController extends BaseController {

    // ==================== FXML 注入的 UI 组件 ====================

    /**
     * 项目下拉框
     * 用于选择要管理的 GitLab 项目
     * 对应 FXML 中的 fx:id="projectCombo"
     */
    @FXML
    private ComboBox<String> projectCombo;

    /**
     * 状态下拉框
     * 用于过滤合并请求的状态（全部/opened/closed/merged）
     * 对应 FXML 中的 fx:id="stateCombo"
     */
    @FXML
    private ComboBox<String> stateCombo;

    /**
     * 搜索输入框
     * 用于实时过滤合并请求列表，支持按标题、作者、分支名称搜索
     * 对应 FXML 中的 fx:id="searchField"
     */
    @FXML
    private TextField searchField;

    /**
     * 刷新按钮
     * 点击后重新从 GitLab 服务器加载合并请求列表
     * 对应 FXML 中的 fx:id="refreshButton"
     */
    @FXML
    private Button refreshButton;

    /**
     * 合并请求表格
     * 显示所有合并请求的详细信息（ID、标题、分支、作者、创建时间、状态）
     * 对应 FXML 中的 fx:id="mrTable"
     */
    @FXML
    private TableView<GitlabMergeRequest> mrTable;

    /**
     * 合并请求 ID 列
     * 显示合并请求的 IID（项目内唯一 ID）
     * 对应 FXML 中的 fx:id="iidColumn"
     */
    @FXML
    private TableColumn<GitlabMergeRequest, Long> iidColumn;

    /**
     * 标题列
     * 显示合并请求的标题
     * 对应 FXML 中的 fx:id="titleColumn"
     */
    @FXML
    private TableColumn<GitlabMergeRequest, String> titleColumn;

    /**
     * 分支列
     * 显示源分支和目标分支（格式：源分支 → 目标分支）
     * 对应 FXML 中的 fx:id="branchColumn"
     */
    @FXML
    private TableColumn<GitlabMergeRequest, String> branchColumn;

    /**
     * 作者列
     * 显示创建合并请求的用户名
     * 对应 FXML 中的 fx:id="authorColumn"
     */
    @FXML
    private TableColumn<GitlabMergeRequest, String> authorColumn;

    /**
     * 创建时间列
     * 显示合并请求的创建时间
     * 对应 FXML 中的 fx:id="dateColumn"
     */
    @FXML
    private TableColumn<GitlabMergeRequest, Date> dateColumn;

    /**
     * 状态列
     * 显示合并请求的状态（opened/closed/merged）
     * 对应 FXML 中的 fx:id="stateColumn"
     */
    @FXML
    private TableColumn<GitlabMergeRequest, String> stateColumn;

    /**
     * 创建合并请求按钮
     * 点击后弹出对话框创建新的合并请求
     * 对应 FXML 中的 fx:id="createButton"
     */
    @FXML
    private Button createButton;

    /**
     * 查看详情按钮
     * 查看选中合并请求的详细信息（描述、提交、代码变更、评论等）
     * 对应 FXML 中的 fx:id="viewButton"
     */
    @FXML
    private Button viewButton;

    /**
     * 批准按钮
     * 批准选中的合并请求
     * 对应 FXML 中的 fx:id="approveButton"
     */
    @FXML
    private Button approveButton;

    /**
     * 合并按钮
     * 将选中的合并请求合并到目标分支
     * 对应 FXML 中的 fx:id="mergeButton"
     */
    @FXML
    private Button mergeButton;

    /**
     * 关闭按钮
     * 关闭选中的合并请求（不合并）
     * 对应 FXML 中的 fx:id="closeButton"
     */
    @FXML
    private Button closeButton;

    /**
     * 进度指示器
     * 显示加载状态的旋转动画
     * 对应 FXML 中的 fx:id="progressIndicator"
     */
    @FXML
    private ProgressIndicator progressIndicator;

    // ==================== 业务逻辑字段 ====================

    /**
     * 合并请求服务
     * 用于调用 GitLab API 进行合并请求操作
     */
    private MergeRequestService mergeRequestService;

    /**
     * 合并请求列表
     * ObservableList 是 JavaFX 的可观察列表，当列表内容变化时会自动更新 UI
     */
    private ObservableList<GitlabMergeRequest> mergeRequests;

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
        // 创建可观察列表，用于存储合并请求数据
        mergeRequests = FXCollections.observableArrayList();

        // ==================== 初始化状态下拉框 ====================
        // 设置状态选项：全部、opened（打开）、closed（关闭）、merged（已合并）
        stateCombo.setItems(FXCollections.observableArrayList(
                "全部", "opened", "closed", "merged"
        ));
        // 默认选中"全部"
        stateCombo.getSelectionModel().select(0);

        // ==================== 初始化表格列 ====================

        // 合并请求 ID 列：绑定到 GitlabMergeRequest 的 iid 属性
        iidColumn.setCellValueFactory(new PropertyValueFactory<>("iid"));

        // 标题列：绑定到 GitlabMergeRequest 的 title 属性
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

        // 分支列：使用自定义的 cellValueFactory 组合显示源分支和目标分支
        branchColumn.setCellValueFactory(cellData -> {
            // 获取源分支和目标分支，组合成 "源分支 → 目标分支" 格式
            String branch = cellData.getValue().getSourceBranch() + " → " + cellData.getValue().getTargetBranch();
            // 返回 SimpleStringProperty 对象，JavaFX 会自动监听其变化
            return new javafx.beans.property.SimpleStringProperty(branch);
        });

        // 作者列：绑定到 GitlabMergeRequest 的 authorName 属性
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("authorName"));

        // 创建时间列：绑定到 GitlabMergeRequest 的 createdAt 属性
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
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

        // 状态列：绑定到 GitlabMergeRequest 的 state 属性
        stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));

        // 将合并请求列表绑定到表格
        mrTable.setItems(mergeRequests);

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

        // 刷新按钮：点击时重新加载合并请求列表
        refreshButton.setOnAction(e -> loadMergeRequests());

        // 创建合并请求按钮：点击时弹出创建合并请求对话框
        createButton.setOnAction(e -> createMergeRequest());

        // 查看详情按钮：点击时显示合并请求详情对话框
        viewButton.setOnAction(e -> viewMergeRequest());

        // 批准按钮：点击时批准选中的合并请求
        approveButton.setOnAction(e -> approveMergeRequest());

        // 合并按钮：点击时合并选中的合并请求
        mergeButton.setOnAction(e -> mergeMergeRequest());

        // 关闭按钮：点击时关闭选中的合并请求
        closeButton.setOnAction(e -> closeMergeRequest());

        // ==================== 按钮状态绑定 ====================

        // 使用属性绑定自动控制按钮的启用/禁用状态
        // 当表格没有选中项时，这些按钮自动禁用
        // disableProperty() 返回一个 BooleanProperty，可以绑定到其他属性
        // selectedItemProperty().isNull() 返回一个 BooleanBinding，当选中项为 null 时为 true
        viewButton.disableProperty().bind(mrTable.getSelectionModel().selectedItemProperty().isNull());
        approveButton.disableProperty().bind(mrTable.getSelectionModel().selectedItemProperty().isNull());
        mergeButton.disableProperty().bind(mrTable.getSelectionModel().selectedItemProperty().isNull());
        closeButton.disableProperty().bind(mrTable.getSelectionModel().selectedItemProperty().isNull());

        // ==================== 状态下拉框变化监听 ====================

        // 监听状态下拉框的选择变化，当用户选择不同状态时重新加载数据
        stateCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadMergeRequests();
            }
        });

        // ==================== 搜索框文本变化监听 ====================

        // 监听搜索框的文本变化，实现实时搜索功能
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterMergeRequests(newVal);
        });
    }

    /**
     * 设置合并请求服务
     * 由父控制器调用，注入 MergeRequestService 实例
     *
     * @param service 合并请求服务实例
     */
    public void setMergeRequestService(MergeRequestService service) {
        this.mergeRequestService = service;
    }

    /**
     * 设置当前项目 ID
     * 由父控制器调用，设置要管理的项目
     *
     * @param projectId 项目 ID
     */
    public void setCurrentProjectId(Long projectId) {
        this.currentProjectId = projectId;
        // 设置项目后立即加载合并请求列表
        loadMergeRequests();
    }

    /**
     * 加载合并请求列表
     * 从 GitLab 服务器异步加载当前项目的合并请求
     */
    private void loadMergeRequests() {
        // 检查必要的参数是否已设置
        if (mergeRequestService == null || currentProjectId == null) {
            return;
        }

        // 获取选中的状态
        String state = stateCombo.getValue();
        // 如果选中"全部"，则传 null 给 API（表示不过滤状态）
        if ("全部".equals(state)) {
            state = null;
        }

        // 显示进度指示器
        progressIndicator.setVisible(true);
        // 保存 state 到 final 变量，以便在 Lambda 表达式中使用
        String finalState = state;

        // 创建异步任务
        Task<List<GitlabMergeRequest>> task = new Task<>() {
            /**
             * call 方法在后台线程执行
             * @return 合并请求列表
             */
            @Override
            protected List<GitlabMergeRequest> call() throws Exception {
                // 调用 GitLab API 获取合并请求列表
                return mergeRequestService.listMergeRequests(currentProjectId, finalState);
            }
        };

        // 执行异步任务
        AsyncTaskUtil.execute(task,
                result -> {
                    // 成功回调：清空现有列表并添加新数据
                    mergeRequests.clear();
                    mergeRequests.addAll(result);
                    // 隐藏进度指示器
                    progressIndicator.setVisible(false);
                    // 显示成功通知
                    DialogUtil.showSuccessNotification("加载成功", "成功加载 " + result.size() + " 个合并请求");
                },
                error -> {
                    // 失败回调：隐藏进度指示器并显示错误对话框
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("加载错误", "加载合并请求失败", error);
                }
        );
    }

    /**
     * 根据搜索文本过滤合并请求列表
     *
     * @param searchText 搜索文本
     */
    private void filterMergeRequests(String searchText) {
        // 如果搜索文本为空，显示所有合并请求
        if (searchText == null || searchText.trim().isEmpty()) {
            mrTable.setItems(mergeRequests);
            return;
        }

        // 转换为小写以实现不区分大小写的搜索
        String lowerSearch = searchText.toLowerCase();

        // 使用 filtered 方法创建过滤后的列表
        // filtered 返回一个 FilteredList，它会自动监听原列表的变化
        ObservableList<GitlabMergeRequest> filtered = mergeRequests.filtered(mr ->
                // 搜索标题、作者、源分支、目标分支
                mr.getTitle().toLowerCase().contains(lowerSearch) ||
                        mr.getAuthorName().toLowerCase().contains(lowerSearch) ||
                        mr.getSourceBranch().toLowerCase().contains(lowerSearch) ||
                        mr.getTargetBranch().toLowerCase().contains(lowerSearch)
        );

        // 将过滤后的列表设置到表格
        mrTable.setItems(filtered);
    }

    /**
     * 创建合并请求
     * TODO: 实现创建合并请求对话框
     *
     * 实现步骤：
     * 1. 创建自定义对话框（Dialog）
     * 2. 添加输入字段：
     *    - 标题（必填）
     *    - 描述（可选，支持 Markdown）
     *    - 源分支（下拉框，从项目分支列表加载）
     *    - 目标分支（下拉框，默认为 main 或 master）
     *    - 分配给（下拉框，从项目成员列表加载）
     *    - 删除源分支（复选框，合并后是否删除源分支）
     * 3. 验证输入：
     *    - 标题不能为空
     *    - 源分支和目标分支不能相同
     * 4. 调用 mergeRequestService.createMergeRequest() 创建合并请求
     * 5. 创建成功后刷新列表
     */
    private void createMergeRequest() {
        // TODO: 显示创建合并请求对话框
        DialogUtil.showInfo("创建合并请求", "创建合并请求功能开发中...");
    }

    /**
     * 查看合并请求详情
     * TODO: 实现合并请求详情对话框
     *
     * 实现步骤：
     * 1. 创建自定义对话框，使用 TabPane 显示多个标签页：
     *    - 描述标签页：显示合并请求的标题、描述、作者、创建时间等基本信息
     *    - 提交标签页：显示该合并请求包含的所有提交记录
     *    - 代码变更标签页：显示文件差异（使用 DiffViewer 组件）
     *    - 评论标签页：显示所有评论，支持添加新评论
     * 2. 调用 GitLab API 获取详细信息：
     *    - mergeRequestService.getMergeRequest(projectId, mrIid)
     *    - mergeRequestService.getCommits(projectId, mrIid)
     *    - mergeRequestService.getChanges(projectId, mrIid)
     *    - mergeRequestService.getNotes(projectId, mrIid)
     * 3. 支持在对话框中直接进行操作（批准、合并、关闭、添加评论）
     */
    private void viewMergeRequest() {
        // 获取选中的合并请求
        GitlabMergeRequest selected = mrTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        // TODO: 显示合并请求详情对话框
        DialogUtil.showInfo("查看详情", "查看合并请求详情功能开发中...\n\nMR #" + selected.getIid() + ": " + selected.getTitle());
    }

    /**
     * 批准合并请求
     * 批准选中的合并请求，表示代码审查通过
     */
    private void approveMergeRequest() {
        // 获取选中的合并请求
        GitlabMergeRequest selected = mrTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        // 显示确认对话框
        boolean confirmed = DialogUtil.showConfirm(
                "确认批准",
                "确定要批准合并请求 #" + selected.getIid() + " 吗？"
        );

        // 如果用户取消，直接返回
        if (!confirmed) {
            return;
        }

        // 显示进度指示器
        progressIndicator.setVisible(true);

        // 创建异步任务执行批准操作
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 调用 API 批准合并请求
                mergeRequestService.approveMergeRequest(currentProjectId, selected.getIid());
                return null;
            }
        };

        // 执行异步任务
        AsyncTaskUtil.execute(task,
                result -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("批准成功", "合并请求已批准");
                    // 刷新合并请求列表
                    loadMergeRequests();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("批准错误", "批准合并请求失败", error);
                }
        );
    }

    /**
     * 合并合并请求
     * 将选中的合并请求合并到目标分支
     */
    private void mergeMergeRequest() {
        // 获取选中的合并请求
        GitlabMergeRequest selected = mrTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        // 显示确认对话框，显示详细的合并信息
        boolean confirmed = DialogUtil.showConfirm(
                "确认合并",
            """
                确定要合并请求 #%d 吗？

                源分支: %s
                目标分支: %s""".formatted(
                selected.getIid(), selected.getSourceBranch(), selected.getTargetBranch())
        );

        // 如果用户取消，直接返回
        if (!confirmed) {
            return;
        }

        // 显示进度指示器
        progressIndicator.setVisible(true);

        // 创建异步任务执行合并操作
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 调用 API 合并合并请求
                // "Merge via Carlos Tools" 是合并提交的消息
                mergeRequestService.mergeMergeRequest(currentProjectId, selected.getIid(), "Merge via Carlos Tools");
                return null;
            }
        };

        // 执行异步任务
        AsyncTaskUtil.execute(task,
                result -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("合并成功", "合并请求已合并");
                    // 刷新合并请求列表
                    loadMergeRequests();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("合并错误", "合并请求失败", error);
                }
        );
    }

    /**
     * 关闭合并请求
     * 关闭选中的合并请求（不合并）
     */
    private void closeMergeRequest() {
        // 获取选中的合并请求
        GitlabMergeRequest selected = mrTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        // 显示确认对话框
        boolean confirmed = DialogUtil.showConfirm(
                "确认关闭",
                "确定要关闭合并请求 #" + selected.getIid() + " 吗？"
        );

        // 如果用户取消，直接返回
        if (!confirmed) {
            return;
        }

        // 显示进度指示器
        progressIndicator.setVisible(true);

        // 创建异步任务执行关闭操作
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 调用 API 关闭合并请求
                mergeRequestService.closeMergeRequest(currentProjectId, selected.getIid());
                return null;
            }
        };

        // 执行异步任务
        AsyncTaskUtil.execute(task,
                result -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showSuccessNotification("关闭成功", "合并请求已关闭");
                    // 刷新合并请求列表
                    loadMergeRequests();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    DialogUtil.showError("关闭错误", "关闭合并请求失败", error);
                }
        );
    }
}
