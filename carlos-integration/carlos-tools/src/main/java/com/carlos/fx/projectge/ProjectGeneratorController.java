package com.carlos.fx.projectge;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.fx.codege.entity.TemplateBaseInfo;
import com.carlos.fx.common.controller.BaseController;
import com.carlos.fx.projectge.config.ProjectGeConstant;
import com.carlos.fx.projectge.entity.ProjectInfo;
import com.carlos.fx.projectge.service.ProjectGeneratorService;
import com.carlos.fx.utils.AsyncTaskUtil;
import com.carlos.fx.utils.DialogUtil;
import com.carlos.fx.utils.TemplateUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 项目脚手架生成器控制器
 * <p>
 * 该控制器负责管理项目脚手架生成工具的界面和业务逻辑。
 * 用户可以通过该工具快速创建基于Spring Boot的项目模板，支持多种预定义的项目类型。
 * </p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>配置项目基本信息（项目名称、GroupId、ArtifactId、包名等）</li>
 *   <li>选择项目模板（基础项目、MyBatis-Plus、Redis、微服务、完整项目）</li>
 *   <li>自动生成项目结构和配置文件</li>
 *   <li>支持自定义输出路径</li>
 *   <li>异步生成项目，显示进度和状态</li>
 *   <li>生成完成后可直接打开项目目录</li>
 * </ul>
 *
 * <p>支持的项目模板：</p>
 * <ul>
 *   <li>Spring Boot 基础项目：包含Web和Test基础依赖</li>
 *   <li>Spring Boot + MyBatis-Plus：包含数据库访问层和代码生成器</li>
 *   <li>Spring Boot + Redis：包含Redis缓存支持</li>
 *   <li>Spring Boot 微服务：包含Spring Cloud Alibaba组件</li>
 *   <li>Spring Boot 完整项目：包含企业级项目的所有常用组件</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Component
@RequiredArgsConstructor
public class ProjectGeneratorController extends BaseController {

    /** Spring应用上下文，用于获取prototype scope的Bean */
    private final ApplicationContext applicationContext;

    /** 项目名称输入框 */
    @FXML
    private TextField projectNameField;

    /** Maven GroupId输入框（如：com.carlos） */
    @FXML
    private TextField groupIdField;

    /** Maven ArtifactId输入框（通常与项目名称相同） */
    @FXML
    private TextField artifactIdField;

    /** Java包名输入框（自动根据GroupId和ArtifactId生成） */
    @FXML
    private TextField packageField;

    /** 项目版本号输入框（默认：1.0.0-SNAPSHOT） */
    @FXML
    private TextField versionField;

    /** 项目描述输入框 */
    @FXML
    private TextField descriptionField;

    /** 作者名称输入框（默认：Carlos） */
    @FXML
    private TextField authorField;

    /** 输出路径输入框（项目生成的目标目录） */
    @FXML
    private TextField outputPathField;

    /** 浏览按钮（用于选择输出路径） */
    @FXML
    private Button browseButton;

    /** 项目模板下拉框 */
    @FXML
    private ComboBox<String> templateCombo;

    /** 模板描述文本区域（显示选中模板的详细信息） */
    @FXML
    private TextArea templateDescArea;

    /** 生成按钮 */
    @FXML
    private Button generateButton;

    /** 进度条（显示项目生成进度） */
    @FXML
    private ProgressBar progressBar;

    /** 状态标签（显示当前操作状态） */
    @FXML
    private Label statusLabel;

    private List<TemplateBaseInfo> templatesBaseInfo;


    /**
     * 初始化界面组件
     * <p>
     * 设置表单的默认值、初始化模板列表、配置进度条的初始状态。
     * </p>
     */
    @Override
    protected void initializeComponents() {
        // 设置默认值
        groupIdField.setText("com.carlos");
        versionField.setText("1.0.0-SNAPSHOT");
        authorField.setText("Carlos");

        // 初始化项目模板下拉框
        URL templateUrl = ResourceUtil.getResource(ProjectGeConstant.TEMPLATE_ROOT_PATH);
        File templateRootPath = FileUtil.file(templateUrl);
        templatesBaseInfo = TemplateUtil.getTemplatesBaseInfo(templateRootPath);
        // 提供5种常用的Spring Boot项目模板
        templateCombo.getItems().addAll(
                templatesBaseInfo.stream().map(TemplateBaseInfo::getName).collect(Collectors.toSet())
        );
        // 默认选中第一个模板（基础项目）
        templateCombo.getSelectionModel().select(0);

        // 当前路径作为默认路径
        outputPathField.setText(new File("").getAbsolutePath());

        // 初始隐藏进度条
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        statusLabel.setText("");
    }

    /**
     * 设置事件处理器
     * <p>
     * 配置各种用户交互事件的处理逻辑，包括按钮点击、文本变化监听等。
     * 实现了智能的字段联动和表单验证。
     * </p>
     */
    @Override
    protected void setupEventHandlers() {
        // 浏览按钮：打开目录选择对话框
        browseButton.setOnAction(e -> browseOutputPath());
        // 生成按钮：执行项目生成操作
        generateButton.setOnAction(e -> generateProject());

        // 智能联动：当项目名称改变时，自动更新ArtifactId
        // 只有在ArtifactId为空或与旧项目名称相同时才自动更新
        projectNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (artifactIdField.getText().isEmpty() || artifactIdField.getText().equals(oldVal)) {
                artifactIdField.setText(newVal);
            }
        });

        // 智能联动：当GroupId或ArtifactId改变时，自动更新包名
        groupIdField.textProperty().addListener((obs, oldVal, newVal) -> updatePackage());
        artifactIdField.textProperty().addListener((obs, oldVal, newVal) -> updatePackage());

        // 模板选择监听：更新模板描述信息
        templateCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateTemplateDescription(newVal);
        });

        // 表单验证：只有所有必填字段都填写后才启用生成按钮
        generateButton.disableProperty().bind(
                projectNameField.textProperty().isEmpty()
                        .or(groupIdField.textProperty().isEmpty())
                        .or(artifactIdField.textProperty().isEmpty())
                        .or(packageField.textProperty().isEmpty())
                        .or(outputPathField.textProperty().isEmpty())
        );
    }

    /**
     * 加载数据
     * <p>
     * 在界面初始化完成后调用，加载默认选中模板的描述信息。
     * </p>
     */
    @Override
    protected void loadData() {
        updateTemplateDescription(templateCombo.getValue());
    }

    /**
     * 更新包名字段
     * <p>
     * 根据GroupId和ArtifactId自动生成Java包名。
     * 规则：groupId + "." + artifactId（将artifactId中的"-"替换为""）
     * </p>
     * <p>示例：</p>
     * <ul>
     *   <li>GroupId: com.carlos</li>
     *   <li>ArtifactId: demo-project</li>
     *   <li>生成的包名: com.carlos.demoproject</li>
     * </ul>
     */
    private void updatePackage() {
        String groupId = groupIdField.getText().trim();
        String artifactId = artifactIdField.getText().trim();

        if (!groupId.isEmpty() && !artifactId.isEmpty()) {
            // 将ArtifactId中的"-"替换为空字符串，生成合法的Java包名
            packageField.setText(groupId + "." + artifactId.replace("-", ""));
        }
    }

    /**
     * 更新模板描述信息
     * <p>
     * 根据选中的模板类型，在文本区域显示该模板的详细描述和包含的模块。
     * </p>
     *
     * @param template 选中的模板名称
     */
    private void updateTemplateDescription(String template) {
        if (template == null) {
            return;
        }
        // 根据模板类型返回对应的描述信息
        for (TemplateBaseInfo selectTemplate : templatesBaseInfo) {
            if (selectTemplate.getName().equals(template)) {
                // 将描述信息显示在文本区域
                templateDescArea.setText(selectTemplate.getDescribe());
            }
        }
    }

    /**
     * 浏览输出路径
     * <p>
     * 打开目录选择对话框，让用户选择项目生成的目标目录。
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
     * 生成项目
     * <p>
     * 执行项目生成的核心方法，包含以下步骤：
     * </p>
     * <ol>
     *   <li>收集表单中的所有项目配置信息</li>
     *   <li>显示确认对话框，让用户确认生成操作</li>
     *   <li>创建异步任务，在后台线程中生成项目</li>
     *   <li>显示进度条和状态信息</li>
     *   <li>生成完成后显示成功提示，并询问是否打开项目目录</li>
     *   <li>如果发生错误，显示错误信息</li>
     * </ol>
     */
    private void generateProject() {
        // 收集表单中的所有配置信息
        String projectName = projectNameField.getText().trim();
        String groupId = groupIdField.getText().trim();
        String artifactId = artifactIdField.getText().trim();
        String packageName = packageField.getText().trim();
        String version = versionField.getText().trim();
        String description = descriptionField.getText().trim();
        String author = authorField.getText().trim();
        String outputPath = outputPathField.getText().trim();
        String template = templateCombo.getValue();

        // 显示确认对话框
        boolean confirmed = DialogUtil.showConfirm(
                "确认生成",
                "将在 " + outputPath + " 目录下生成项目 " + projectName + "，是否继续？"
        );

        if (!confirmed) {
            return;
        }

        // 显示进度条和状态信息
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(-1); // 不确定进度（无限循环动画）
        statusLabel.setText("正在生成项目...");
        generateButton.disableProperty().unbind();
        generateButton.setDisable(true);

        // 创建异步任务，在后台线程中生成项目
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 创建项目配置对象
                ProjectInfo projectInfo = new ProjectInfo();
                projectInfo.setProjectName(projectName);
                projectInfo.setGroupId(groupId);
                projectInfo.setArtifactId(artifactId);
                projectInfo.setOutputPath(outputPath);
                projectInfo.setAuthor(author);
                projectInfo.setDescribe(description);
                projectInfo.setPackageName(packageName);
                projectInfo.setVersion(version);

                // 设置GroupId的各个部分（按"."分割）
                // 例如：com.carlos -> ["com", "carlos"]
                projectInfo.setPackageNameItems(StrUtil.split(packageName, "."));

                // 创建模板配置对象
                TemplateBaseInfo selectTemplate = new TemplateBaseInfo();
                selectTemplate.setName(template);
                // 设置模板路径（指向模板文件所在目录）
                Optional<TemplateBaseInfo> first = templatesBaseInfo.stream().filter(templateBaseInfo -> templateBaseInfo.getName().equals(template)).findFirst();
                selectTemplate.setPath(first.get().getPath());
                projectInfo.setSelectTemplate(selectTemplate);

                // 更新状态消息
                updateMessage("正在创建项目结构...");

                // 使用Spring获取prototype scope的ProjectGeneratorService bean
                ProjectGeneratorService projectGeneratorService = applicationContext.getBean(ProjectGeneratorService.class);
                projectGeneratorService.createObject(projectInfo);

                updateMessage("项目生成完成！");
                return null;
            }
        };

        // 监听任务的消息变化，更新状态标签
        task.messageProperty().addListener((obs, oldVal, newVal) -> statusLabel.setText(newVal));

        // 执行异步任务
        AsyncTaskUtil.execute(task,
                // 成功回调：任务成功完成时执行
                result -> {
                    // 隐藏进度条
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                    statusLabel.setText("项目生成完成！");
                    generateButton.setDisable(false);

                    // 构建项目完整路径
                    String projectPath = outputPath + File.separator + projectName;

                    // 使用新的对话框方法，询问是否打开输出目录
                    DialogUtil.showSuccessWithOpenDirectory(
                            "生成成功",
                            "项目已成功生成！",
                            Paths.get(projectPath)
                    );
                },
                // 失败回调：任务执行失败时执行
                error -> {
                    // 隐藏进度条
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                    statusLabel.setText("项目生成失败");
                    generateButton.setDisable(false);

                    // 使用新的对话框方法，询问是否打开日志
                    DialogUtil.showErrorWithOpenLog(
                            "生成失败",
                            "项目生成过程中发生错误",
                            error
                    );
                }
        );
    }
}
