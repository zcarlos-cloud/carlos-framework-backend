package com.carlos.fx.codege;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.fx.codege.config.CodeGenerateInfo;
import com.carlos.fx.codege.config.CodegeConstant;
import com.carlos.fx.codege.config.DatabaseInfo;
import com.carlos.fx.codege.entity.TableBaseInfo;
import com.carlos.fx.codege.entity.TemplateBaseInfo;
import com.carlos.fx.codege.entity.ViewItemVO;
import com.carlos.fx.codege.enums.DbTypeEnum;
import com.carlos.fx.codege.enums.FieldNameTypeEnum;
import com.carlos.fx.codege.service.CodeGeneratorService;
import com.carlos.fx.common.controller.BaseController;
import com.carlos.fx.utils.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.stage.DirectoryChooser;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代码生成器控制器
 * <p>
 * 该控制器负责管理数据库代码生成工具的界面和业务逻辑。
 * 支持从数据库表结构自动生成Java代码，包括Entity、Mapper、Service、Controller等层次的代码。
 * </p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>连接数据库并测试连接状态</li>
 *   <li>加载数据库表信息（包括表名和注释）</li>
 *   <li>支持多表选择，批量生成代码</li>
 *   <li>可选择生成的代码层次（Entity、Mapper、Service、Controller）</li>
 *   <li>自动生成符合MyBatis-Plus规范的代码</li>
 *   <li>支持自定义包名、作者和输出路径</li>
 *   <li>异步生成代码，显示进度和状态</li>
 * </ul>
 *
 * <p>生成的代码特点：</p>
 * <ul>
 *   <li>Entity：包含字段注解、Lombok注解、表映射注解</li>
 *   <li>Mapper：继承BaseMapper，支持MyBatis-Plus的CRUD操作</li>
 *   <li>Service：包含Service接口和ServiceImpl实现类</li>
 *   <li>Controller：包含基础的CRUD接口，使用RESTful风格</li>
 * </ul>
 *
 * <p>适用场景：</p>
 * <ul>
 *   <li>新项目快速搭建基础代码</li>
 *   <li>数据库表结构变更后重新生成代码</li>
 *   <li>统一代码风格和规范</li>
 *   <li>减少重复性的CRUD代码编写</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Component
public class CodeGeneratorController extends BaseController {

    /** Spring应用上下文，用于获取prototype scope的Bean */
    private final ApplicationContext applicationContext;

    /** 数据库类型下拉框（MySQL、PostgreSQL等） */
    @FXML
    private ComboBox<DbTypeEnum> databaseTypeCombo;

    /** 数据库主机地址输入框 */
    @FXML
    private TextField hostField;

    /** 数据库端口输入框 */
    @FXML
    private TextField portField;

    /** 数据库用户名输入框 */
    @FXML
    private TextField usernameField;

    /** 数据库密码输入框（密码框，输入内容不可见） */
    @FXML
    private PasswordField passwordField;

    /** 数据库名称输入框 */
    @FXML
    private TextField databaseField;

    /** 测试连接按钮 */
    @FXML
    private Button testConnectionButton;

    /** 加载表按钮 */
    @FXML
    private Button loadTablesButton;

    /** 表树形视图（支持复选框选择多个表） */
    @FXML
    private TreeView<ViewItemVO> tableTreeView;


    /** 表字段命名方式 */
    @FXML
    private ComboBox<FieldNameTypeEnum> fieldNameTypeCombo;

    /** 项目名称（生成代码的基础包名） */
    @FXML
    private TextField projectNameField;

    /** Java包名输入框（生成代码的基础包名） */
    @FXML
    private TextField packageField;

    /** 作者名称输入框（会添加到生成代码的@author注解中） */
    @FXML
    private TextField authorField;

    /** 输出路径输入框（代码生成的目标目录） */
    @FXML
    private TextField outputPathField;

    /** 浏览按钮（用于选择输出路径） */
    @FXML
    private Button browseButton;

    /** 代码模板选择下拉框 */
    @FXML
    private ComboBox<ViewItemVO> templateCombo;

    /** 表名前缀开关复选框 */
    @FXML
    private CheckBox tablePrefixCheck;

    /** 生成按钮 */
    @FXML
    private Button generateButton;

    /** 进度条（显示代码生成进度） */
    @FXML
    private ProgressBar progressBar;

    /** 进度条（显示代码生成进度） */
    @FXML
    private ProgressBar databaseProgressBar;

    /** 状态标签（显示当前操作状态） */
    @FXML
    private Label statusLabel;

    /** 数据库操作状态 */
    @FXML
    private Label databaseStatus;

    /** 所有表的列表（用于保存从数据库加载的表信息） */
    private List<TableBaseInfo> allTables;

    List<TemplateBaseInfo> templatesBaseInfo;

    /**
     * 构造函数，注入Spring应用上下文
     *
     * @param applicationContext Spring应用上下文
     */
    public CodeGeneratorController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化界面组件
     * <p>
     * 设置表单的默认值、初始化数据库类型列表、配置树形视图和生成选项。
     * </p>
     */
    @Override
    protected void initializeComponents() {
        // 初始化表列表
        allTables = new ArrayList<>();
        ResourceUtil.copyResourceFile2Temp("codege", CodegeConstant.TEMP_DIR);

        // 初始化数据库类型下拉框，添加所有支持的数据库类型
        databaseTypeCombo.setItems(FXCollections.observableArrayList(DbTypeEnum.values()));
        // 默认选中MySQL
        databaseTypeCombo.getSelectionModel().select(DbTypeEnum.MYSQL);

        // 初始化字段命名方式
        fieldNameTypeCombo.setItems(FXCollections.observableArrayList(FieldNameTypeEnum.values()));
        fieldNameTypeCombo.getSelectionModel().select(FieldNameTypeEnum.NOT_PREFIX_AND_UNDERLINE);

        // 初始化代码模板下拉框
        // TODO: Carlos 2026-02-14
        File templateRootPath = FileUtil.file(CodegeConstant.TEMP_DIR + File.separator + CodegeConstant.TEMPLATE_ROOT_PATH);

        templatesBaseInfo = TemplateUtil.getTemplatesBaseInfo(templateRootPath);
        if (CollUtil.isEmpty(templatesBaseInfo)) {
            throw new IllegalStateException("模板为空");
        }
        List<ViewItemVO> collect = templatesBaseInfo.stream().map(templateBaseInfo -> new ViewItemVO(templateBaseInfo.getName(), templateBaseInfo.getName())).collect(Collectors.toList());
        templateCombo.setItems(FXCollections.observableArrayList(collect));
        templateCombo.getSelectionModel().select(0);

        // 设置默认值
        hostField.setText("100.73.89.3");
        portField.setText("13306");
        usernameField.setText("carlos");
        databaseField.setText("carlos-framework");
        passwordField.setText("Hadoop@2022");

        authorField.setText("Carlos");
        packageField.setText("com.carlos.demo");
        // 默认启用表名前缀
        tablePrefixCheck.setSelected(true);
        // 当前路径作为默认路径
        outputPathField.setText(new File("").getAbsolutePath());

        // 初始化树形视图，使用复选框树项
        CheckBoxTreeItem<ViewItemVO> rootItem = new CheckBoxTreeItem<>(new ViewItemVO("所有表", "所有表"));
        rootItem.setExpanded(true); // 默认展开根节点
        tableTreeView.setRoot(rootItem);
        tableTreeView.setShowRoot(false); // 隐藏根节点
        tableTreeView.setCellFactory(CheckBoxTreeCell.forTreeView());

        // 初始隐藏进度条
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        statusLabel.setText("");
        databaseStatus.setText("");
        databaseProgressBar.setVisible(false);
        databaseProgressBar.setManaged(false);
    }

    /**
     * 设置事件处理器
     * <p>
     * 配置各种用户交互事件的处理逻辑，包括按钮点击和表单验证。
     * </p>
     */
    @Override
    protected void setupEventHandlers() {
        // 测试连接按钮：测试数据库连接是否正常
        testConnectionButton.setOnAction(e -> testConnection());
        // 加载表按钮：从数据库加载所有表信息
        loadTablesButton.setOnAction(e -> loadTables());
        // 浏览按钮：打开目录选择对话框
        browseButton.setOnAction(e -> browseOutputPath());
        // 生成按钮：执行代码生成操作
        generateButton.setOnAction(e -> generateCode());

        // 表单验证：只有填写了必要的数据库连接信息后才启用"加载表"按钮
        loadTablesButton.disableProperty().bind(
                hostField.textProperty().isEmpty()
                        .or(portField.textProperty().isEmpty())
                        .or(usernameField.textProperty().isEmpty())
                        .or(databaseField.textProperty().isEmpty())
        );

        // 表单验证：只有填写了包名和输出路径后才启用"生成"按钮
        generateButton.disableProperty().bind(
                projectNameField.textProperty().isEmpty()
                        .or(outputPathField.textProperty().isEmpty())
                        .or(packageField.textProperty().isEmpty())
                        .or(authorField.textProperty().isEmpty())
        );
    }

    /**
     * 测试数据库连接
     * <p>
     * 使用用户输入的数据库连接信息尝试连接数据库，验证连接是否成功。
     * 该操作在后台线程中异步执行，不会阻塞UI。
     * </p>
     */
    private void testConnection() {
        // 构建数据库连接信息对象
        DatabaseInfo dbInfo = buildDatabaseInfo();

        // 创建异步任务
        Task<DatabaseUtil.ConnectionTestResult> task = new Task<>() {
            @Override
            protected DatabaseUtil.ConnectionTestResult call() {
                return DatabaseUtil.testConnection(dbInfo.getDataSource());
            }
        };

        task.messageProperty().addListener((obs, oldVal, newVal) -> databaseStatus.setText(newVal));
        task.progressProperty().addListener((obs, oldVal, newVal) -> databaseProgressBar.setProgress(newVal.doubleValue()));

        // 执行异步任务
        AsyncTaskUtil.execute(task,
                // 成功回调
                result -> {
                    if (result.isSuccess()) {
                        databaseProgressBar.setVisible(false);
                        databaseProgressBar.setManaged(false);
                        databaseStatus.setText("连接成功，" + result.toString());
                    } else {
                        databaseProgressBar.setVisible(false);
                        databaseProgressBar.setManaged(false);
                        databaseStatus.setText("连接失败，" + result.toString());
                    }
                },
                // 失败回调
                error -> DialogUtil.showError("连接错误", "数据库连接失败", error)
        );
    }

    /**
     * 从数据库加载表信息
     * <p>
     * 连接数据库并获取所有表的详细信息（包括表名、注释、字段等），
     * 然后在树形视图中显示。该操作在后台线程中异步执行。
     * </p>
     */
    private void loadTables() {
        DatabaseInfo dbInfo = buildDatabaseInfo();

        // 显示进度条和状态信息
        databaseProgressBar.setVisible(true);
        databaseProgressBar.setManaged(true);
        databaseStatus.setText("正在加载表信息...");

        // 创建异步任务
        Task<List<TableBaseInfo>> task = new Task<>() {
            @Override
            protected List<TableBaseInfo> call() throws Exception {
                return DatabaseUtil.getTables(dbInfo);
            }
        };

        task.messageProperty().addListener((obs, oldVal, newVal) -> databaseStatus.setText(newVal));
        task.progressProperty().addListener((obs, oldVal, newVal) -> databaseProgressBar.setProgress(newVal.doubleValue()));

        // 执行异步任务
        AsyncTaskUtil.execute(task,
                // 成功回调
                tables -> {
                    // 保存表信息
                    allTables = tables;
                    // 填充树形视图
                    populateTableTree(tables);
                    // 隐藏进度条
                    databaseProgressBar.setVisible(false);
                    databaseProgressBar.setManaged(false);
                    databaseStatus.setText("加载完成，共 " + tables.size() + " 张表");
                },
                // 失败回调
                error -> {
                    databaseProgressBar.setVisible(false);
                    databaseProgressBar.setManaged(false);
                    databaseStatus.setText("加载失败：" + error.getMessage());
                }
        );
    }

    /**
     * 填充表树形视图
     * <p>
     * 将数据库表信息以树形结构显示在界面上。
     * 每个表作为一个节点，显示表名和注释。
     * 所有节点都支持复选框选择。
     * </p>
     *
     * @param tables 表列表
     */
    private void populateTableTree(List<TableBaseInfo> tables) {
        ObservableList<TreeItem<ViewItemVO>> root = tableTreeView.getRoot().getChildren();
        // 根据表名前缀进行分组
        Map<String, List<TableBaseInfo>> groupedTables = tables.stream()
                .collect(Collectors.groupingBy(i -> {
                    String name = i.getName();
                    String prefix = StrUtil.subBefore(name, "_", false);
                    if (StrUtil.isBlank(prefix)) {
                        prefix = "无前缀";
                    }
                    return prefix;
                }));

        // 前缀根据字母进行排序
        groupedTables.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String prefix = entry.getKey();
                    List<TableBaseInfo> childrens = entry.getValue();
                    // 创建树形视图项
                    CheckBoxTreeItem<ViewItemVO> prefixItem = new CheckBoxTreeItem<>(new ViewItemVO(prefix, prefix));
                    prefixItem.setExpanded(false); // 默认展开根节点
                    root.add(prefixItem);
                    for (TableBaseInfo child : childrens) {
                        String name = child.getName();
                        String comment = StrUtil.blankToDefault(child.getComment(), name);
                        String viewName = name + "【" + comment + "】";
                        CheckBoxTreeItem<ViewItemVO> childItem = new CheckBoxTreeItem<>(new ViewItemVO(child.getName(), viewName));
                        prefixItem.getChildren().add(childItem);
                    }
                });
    }

    /**
     * 浏览输出路径
     * <p>
     * 打开目录选择对话框，让用户选择代码生成的目标目录。
     * 选择后将路径填充到输出路径输入框中。
     * </p>
     */
    private void browseOutputPath() {
        // 创建目录选择器
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择输出目录");

        // 显示目录选择对话框
        File selectedDir = chooser.showDialog(browseButton.getScene().getWindow());
        if (selectedDir != null) {
            // 将选中的目录路径填充到输入框
            outputPathField.setText(selectedDir.getAbsolutePath());
        }
    }

    /**
     * 生成代码
     * <p>
     * 执行代码生成的核心方法，包含以下步骤：
     * </p>
     * <ol>
     *   <li>获取用户选中的表列表</li>
     *   <li>验证生成选项（至少选择一个代码层次）</li>
     *   <li>显示确认对话框</li>
     *   <li>创建异步任务，在后台线程中生成代码</li>
     *   <li>显示进度条和状态信息</li>
     *   <li>生成完成后显示成功提示</li>
     * </ol>
     */
    private void generateCode() {
        // 获取用户选中的表
        List<TableBaseInfo> selectedTables = getSelectedTables();
        if (selectedTables.isEmpty()) {
            DialogUtil.showWarning("未选择表", "请至少选择一张表进行代码生成！");
            return;
        }

        // 显示确认对话框
        boolean confirmed = DialogUtil.showConfirm(
                "确认生成",
                "将为 " + selectedTables.size() + " 张表生成代码，是否继续？"
        );

        if (!confirmed) {
            return;
        }

        // 收集表单中的配置信息
        DatabaseInfo dbInfo = buildDatabaseInfo();
        String projectName = projectNameField.getText().trim();
        String packageName = packageField.getText().trim();
        String author = authorField.getText().trim();
        String outputPath = outputPathField.getText().trim();

        // 显示进度条和状态信息
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(0);
        statusLabel.setText("正在生成代码...");
        generateButton.disableProperty().bind(
                new SimpleBooleanProperty(false)
        );

        // 创建异步任务
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 创建项目配置对象
                CodeGenerateInfo codeGenerateInfo = new CodeGenerateInfo();
                codeGenerateInfo.setProjectName(projectName);
                codeGenerateInfo.setPackageName(packageName);
                codeGenerateInfo.setOutputPath(outputPath);
                codeGenerateInfo.setAuthor(author);

                // 设置表名前缀配置
                codeGenerateInfo.setUseTablePrefix(tablePrefixCheck.isSelected());

                // 创建模板配置对象
                // 设置代码模板类型
                ViewItemVO selectTemplate = templateCombo.getValue();
                if (selectTemplate != null) {
                    templatesBaseInfo.stream()
                            .filter(i -> i.getName().equals(selectTemplate.getKey()))
                            .findFirst()
                            .ifPresent(codeGenerateInfo::setTemplateBaseInfo);
                }

                // 设置字段命名规则
                codeGenerateInfo.setNameType(fieldNameTypeCombo.getValue());

                codeGenerateInfo.setTables(selectedTables);

                // 使用Spring获取prototype scope的Generator bean
                CodeGeneratorService generator = applicationContext.getBean(CodeGeneratorService.class);

                // 更新状态信息
                updateMessage("正在生成代码...");
                updateProgress(0, 1);

                // 执行代码生成
                generator.createObject(dbInfo, codeGenerateInfo);
                updateProgress(1, 1);
                return null;
            }
        };

        // 监听任务的消息和进度变化
        task.messageProperty().addListener((obs, oldVal, newVal) -> statusLabel.setText(newVal));
        task.progressProperty().addListener((obs, oldVal, newVal) -> progressBar.setProgress(newVal.doubleValue()));

        // 执行异步任务
        AsyncTaskUtil.execute(task,
                // 成功回调
                result -> {
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                    statusLabel.setText("代码生成完成！");
                    generateButton.disableProperty().bind(
                            new SimpleBooleanProperty(false)
                    );
                    // 使用新的对话框方法，询问是否打开输出目录
                    DialogUtil.showSuccessWithOpenDirectory(
                            "生成成功",
                            "代码已成功生成！",
                            Paths.get(outputPath)
                    );
                },
                // 失败回调
                error -> {
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                    statusLabel.setText("代码生成失败");
                    generateButton.disableProperty().bind(
                            new SimpleBooleanProperty(false)
                    );
                    // 使用新的对话框方法，询问是否打开日志
                    DialogUtil.showErrorWithOpenLog(
                            "生成失败",
                            "代码生成过程中发生错误",
                            error
                    );
                }
        );
    }

    /**
     * 从树形视图中获取选中的表
     * <p>
     * 遍历树形视图，收集所有被选中的表。
     * </p>
     *
     * @return 选中的表列表
     */
    private List<TableBaseInfo> getSelectedTables() {
        List<TableBaseInfo> selectedTables = new ArrayList<>();
        CheckBoxTreeItem<ViewItemVO> root = (CheckBoxTreeItem<ViewItemVO>) tableTreeView.getRoot();
        Map<String, String> commentMap = allTables.stream().collect(Collectors.toMap(TableBaseInfo::getName, TableBaseInfo::getComment));

        ObservableList<TreeItem<ViewItemVO>> prefixItems = root.getChildren();
        for (TreeItem<ViewItemVO> prefixItem : prefixItems) {
            ObservableList<TreeItem<ViewItemVO>> children = prefixItem.getChildren();
            for (TreeItem<ViewItemVO> child : children) {
                if (((CheckBoxTreeItem<ViewItemVO>) child).isSelected()) {
                    ViewItemVO value = child.getValue();
                    selectedTables.add(new TableBaseInfo(value.getKey(), commentMap.get(value.getKey())));
                }
            }
        }
        return selectedTables;
    }

    /**
     * 根据表单字段构建数据库连接信息对象
     * <p>
     * 从界面的输入框中收集数据库连接信息，构建DatabaseInfo对象。
     * </p>
     *
     * @return 数据库连接信息对象
     */
    private DatabaseInfo buildDatabaseInfo() {
        DatabaseInfo dbInfo = new DatabaseInfo();
        dbInfo.setDbType(databaseTypeCombo.getValue());
        dbInfo.setIp(hostField.getText().trim());
        dbInfo.setPort(portField.getText().trim());
        dbInfo.setUser(usernameField.getText().trim());
        dbInfo.setPwd(passwordField.getText());
        dbInfo.setDbName(databaseField.getText().trim());
        return dbInfo;
    }
}
