package com.carlos.fx.common.controller;

import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * JavaFX 控制器基类
 * 所有 JavaFX 控制器都应该继承此类，提供统一的初始化流程
 *
 * <p>该类实现了 JavaFX 的 Initializable 接口，在 FXML 文件加载完成后自动调用 initialize 方法。
 * 初始化流程分为三个阶段：
 * <ol>
 *   <li>initializeComponents() - 初始化 UI 组件的状态</li>
 *   <li>setupEventHandlers() - 设置事件处理器</li>
 *   <li>loadData() - 加载初始数据</li>
 * </ol>
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * public class MyController extends BaseController {
 *     @FXML
 *     private Button myButton;
 *
 *     @Override
 *     protected void initializeComponents() {
 *         // 初始化组件状态
 *         myButton.setDisable(false);
 *     }
 *
 *     @Override
 *     protected void setupEventHandlers() {
 *         // 设置事件处理器
 *         myButton.setOnAction(e -> handleButtonClick());
 *     }
 *
 *     @Override
 *     protected void loadData() {
 *         // 加载初始数据
 *         loadDataFromServer();
 *     }
 * }
 * }</pre>
 *
 * @author Carlos
 * @since 3.0.0
 */
public abstract class BaseController implements Initializable {

    /**
     * JavaFX 初始化方法
     * 该方法在 FXML 文件加载完成后自动调用，此时所有 @FXML 注解的字段已经被注入
     *
     * <p>初始化顺序：</p>
     * <ol>
     *   <li>FXML 文件解析，创建 UI 组件</li>
     *   <li>@FXML 字段注入</li>
     *   <li>调用 initialize 方法</li>
     * </ol>
     *
     * @param location  FXML 文件的位置，通常不需要使用
     * @param resources 资源包，用于国际化，通常不需要使用
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 按顺序执行初始化步骤
        // 1. 初始化 UI 组件（设置初始状态、绑定数据等）
        initializeComponents();

        // 2. 设置事件处理器（按钮点击、文本变化等）
        setupEventHandlers();

        // 3. 加载初始数据（从服务器或本地加载数据）
        loadData();
    }

    /**
     * 初始化 UI 组件
     * 在此方法中进行 UI 组件的初始化工作，例如：
     * <ul>
     *   <li>设置组件的初始状态（启用/禁用、可见/隐藏）</li>
     *   <li>初始化表格列（setCellValueFactory、setCellFactory）</li>
     *   <li>创建 ObservableList 并绑定到 UI 组件</li>
     *   <li>设置下拉框的选项</li>
     *   <li>配置表格、列表等组件的属性</li>
     * </ul>
     *
     * <p>注意：此时不应该进行网络请求或耗时操作，这些应该放在 loadData() 方法中。</p>
     */
    protected void initializeComponents() {
        // 子类可以重写此方法来初始化 UI 组件
        // 默认实现为空，不做任何操作
    }

    /**
     * 设置事件处理器
     * 在此方法中绑定 UI 组件的事件处理器，例如：
     * <ul>
     *   <li>按钮点击事件：button.setOnAction(e -> handleClick())</li>
     *   <li>文本变化监听：textField.textProperty().addListener(...)</li>
     *   <li>选择变化监听：comboBox.getSelectionModel().selectedItemProperty().addListener(...)</li>
     *   <li>表格行选择监听：tableView.getSelectionModel().selectedItemProperty().addListener(...)</li>
     *   <li>属性绑定：button.disableProperty().bind(...)</li>
     * </ul>
     *
     * <p>建议将事件处理逻辑提取为独立的方法，保持此方法简洁清晰。</p>
     */
    protected void setupEventHandlers() {
        // 子类可以重写此方法来设置事件处理器
        // 默认实现为空，不做任何操作
    }

    /**
     * 加载初始数据
     * 在此方法中加载界面需要的初始数据，例如：
     * <ul>
     *   <li>从服务器加载数据列表</li>
     *   <li>从配置文件读取设置</li>
     *   <li>从数据库查询数据</li>
     *   <li>初始化下拉框选项（如果需要从服务器加载）</li>
     * </ul>
     *
     * <p>注意：</p>
     * <ul>
     *   <li>如果是耗时操作，应该使用异步任务（Task）在后台线程执行</li>
     *   <li>加载数据时应该显示进度指示器，完成后隐藏</li>
     *   <li>处理加载失败的情况，显示错误提示</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>{@code
     * @Override
     * protected void loadData() {
     *     progressIndicator.setVisible(true);
     *
     *     Task<List<Data>> task = new Task<>() {
     *         @Override
     *         protected List<Data> call() throws Exception {
     *             return dataService.loadData();
     *         }
     *     };
     *
     *     AsyncTaskUtil.execute(task,
     *         result -> {
     *             dataList.addAll(result);
     *             progressIndicator.setVisible(false);
     *         },
     *         error -> {
     *             progressIndicator.setVisible(false);
     *             DialogUtil.showError("加载错误", "加载数据失败", error);
     *         }
     *     );
     * }
     * }</pre>
     */
    protected void loadData() {
        // 子类可以重写此方法来加载初始数据
        // 默认实现为空，不做任何操作
    }

    /**
     * 清理资源
     * 在控制器销毁前调用，用于释放资源，例如：
     * <ul>
     *   <li>关闭数据库连接</li>
     *   <li>取消正在执行的异步任务</li>
     *   <li>移除事件监听器</li>
     *   <li>释放大对象的引用</li>
     * </ul>
     *
     * <p>注意：JavaFX 不会自动调用此方法，需要在适当的时机手动调用。</p>
     *
     * <p>示例：</p>
     * <pre>{@code
     * @Override
     * public void cleanup() {
     *     // 取消正在执行的任务
     *     if (currentTask != null && currentTask.isRunning()) {
     *         currentTask.cancel();
     *     }
     *
     *     // 关闭连接
     *     if (connection != null) {
     *         connection.close();
     *     }
     * }
     * }</pre>
     */
    public void cleanup() {
        // 子类可以重写此方法来清理资源
        // 默认实现为空，不做任何操作
    }
}
