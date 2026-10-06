package com.carlos.fx.common.controller;

import com.carlos.fx.ToolsApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 主窗口控制器
 * <p>
 * 这是Carlos工具集的主界面控制器，负责管理整个应用程序的主窗口布局和工具导航。
 * 采用左侧工具列表 + 右侧内容区域的经典布局模式。
 * </p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>初始化工具列表，包括代码生成器、项目脚手架、加密工具、GitLab工具等</li>
 *   <li>管理工具列表的显示，使用图标和描述增强用户体验</li>
 *   <li>处理工具选择事件，动态加载对应的工具界面</li>
 *   <li>在内容区域动态切换不同工具的FXML视图</li>
 * </ul>
 *
 * <p>界面布局：</p>
 * <pre>
 * +------------------+---------------------------+
 * |                  |                           |
 * |   工具列表        |      内容区域              |
 * |   (ListView)     |    (StackPane)            |
 * |                  |                           |
 * |  - 代码生成器     |   [动态加载的工具界面]      |
 * |  - 项目脚手架     |                           |
 * |  - 加密工具       |                           |
 * |  - GitLab工具    |                           |
 * |  - 设置          |                           |
 * |                  |                           |
 * +------------------+---------------------------+
 * </pre>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@Component
public class MainController extends BaseController {

    /**
     * 工具列表视图
     * <p>
     * 显示所有可用工具的列表，每个工具项包含图标、名称和描述。
     * 用户点击列表项时，会在右侧内容区域加载对应的工具界面。
     * </p>
     */
    @FXML
    private ListView<ToolItem> toolListView;

    /**
     * 内容面板
     * <p>
     * 使用StackPane作为容器，用于动态加载和显示不同工具的界面。
     * 当用户选择不同的工具时，会清空当前内容并加载新的FXML视图。
     * </p>
     */
    @FXML
    private StackPane contentPane;

    /**
     * 初始化界面组件
     * <p>
     * 该方法在控制器加载时自动调用，负责初始化工具列表和自定义单元格渲染器。
     * </p>
     *
     * <p>初始化步骤：</p>
     * <ol>
     *   <li>创建并添加所有工具项到列表中</li>
     *   <li>设置自定义单元格工厂，为每个工具项添加图标和样式</li>
     *   <li>默认选中第一个工具项</li>
     * </ol>
     */
    @Override
    protected void initializeComponents() {
        // 设置工具列表，添加所有可用的工具项
        // 每个工具项包含：中文名称、英文描述、图标、FXML文件路径
        toolListView.getItems().addAll(
                new ToolItem("代码生成器", "Code Generator", FontAwesomeSolid.CODE, "/fxml/codege/codegenerator.fxml"),
                new ToolItem("项目脚手架", "Project Scaffold", FontAwesomeSolid.PROJECT_DIAGRAM, "/fxml/projectge/projectgenerator.fxml"),
                new ToolItem("GitLab工具", "GitLab Tools", FontAwesomeSolid.CODE_BRANCH, "/fxml/gitlab/gitlabmain.fxml"),
                new ToolItem("设置", "Settings", FontAwesomeSolid.COG, null)
        );

        // 自定义单元格工厂，为每个工具项创建美观的显示效果
        // 每个单元格包含图标、工具名称和描述
        toolListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ToolItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    // 空单元格不显示任何内容
                    setText(null);
                    setGraphic(null);
                } else {
                    // 创建垂直布局容器，间距为5像素
                    VBox vbox = new VBox(5);

                    // 创建并配置图标
                    FontIcon icon = new FontIcon(item.getIcon());
                    icon.setIconSize(24);

                    // 创建工具名称标签（粗体，14px）
                    Label nameLabel = new Label(item.getName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                    // 创建工具描述标签（灰色，12px）
                    Label descLabel = new Label(item.getDescription());
                    descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");

                    // 将图标、名称、描述添加到容器中
                    vbox.getChildren().addAll(icon, nameLabel, descLabel);
                    setGraphic(vbox);
                }
            }
        });

        // 默认选中第一个工具项（代码生成器）
        if (!toolListView.getItems().isEmpty()) {
            toolListView.getSelectionModel().select(0);
        }
    }

    /**
     * 设置事件处理器
     * <p>
     * 为工具列表添加选择监听器，当用户选择不同的工具时，
     * 自动加载对应的工具界面到内容区域。
     * </p>
     */
    @Override
    protected void setupEventHandlers() {
        // 监听工具列表的选择变化
        // 当用户点击不同的工具项时，触发加载对应的工具界面
        toolListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadTool(newVal);
            }
        });
    }

    /**
     * 加载数据
     * <p>
     * 在界面初始化完成后调用，加载默认选中的第一个工具界面。
     * </p>
     */
    @Override
    protected void loadData() {
        // 加载第一个工具（默认选中的工具）
        ToolItem firstItem = toolListView.getSelectionModel().getSelectedItem();
        if (firstItem != null) {
            loadTool(firstItem);
        }
    }

    /**
     * 加载工具内容
     * <p>
     * 根据选中的工具项，动态加载对应的FXML界面到内容区域。
     * 如果工具没有对应的FXML文件（如设置），则显示"功能开发中"提示。
     * </p>
     *
     * @param toolItem 要加载的工具项
     */
    private void loadTool(ToolItem toolItem) {
        if (toolItem.getFxmlPath() == null) {
            // 对于没有FXML文件的工具（如设置），显示开发中提示
            contentPane.getChildren().clear();
            Label label = new Label("功能开发中...");
            label.setStyle("-fx-font-size: 24px; -fx-text-fill: #757575;");
            contentPane.getChildren().add(label);
            return;
        }

        try {
            // 加载工具的FXML文件
            FXMLLoader loader = new FXMLLoader(getClass().getResource(toolItem.getFxmlPath()));
            // 使用Spring作为控制器工厂
            loader.setControllerFactory(ToolsApplication.getSpringContext()::getBean);
            // 清空内容区域
            contentPane.getChildren().clear();
            // 将加载的工具界面添加到内容区域
            contentPane.getChildren().add(loader.load());
        } catch (IOException e) {
            // 加载失败时显示错误信息
            log.error("加载工具失败: ", e);
            Label errorLabel = new Label("加载失败: " + e.getMessage());
            errorLabel.setWrapText(true);
            errorLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #f44336;");
            contentPane.getChildren().clear();
            contentPane.getChildren().add(errorLabel);
        }
    }

    /**
     * 工具项模型类
     * <p>
     * 表示工具列表中的一个工具项，包含工具的基本信息。
     * 这是一个不可变的数据类，用于在工具列表中显示工具信息。
     * </p>
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ToolItem {
        /** 工具的中文名称 */
        private String name;

        /** 工具的英文描述 */
        private String description;

        /** 工具的图标（FontAwesome图标） */
        private FontAwesomeSolid icon;

        /** 工具对应的FXML文件路径，如果为null表示该工具尚未实现 */
        private String fxmlPath;
    }
}
