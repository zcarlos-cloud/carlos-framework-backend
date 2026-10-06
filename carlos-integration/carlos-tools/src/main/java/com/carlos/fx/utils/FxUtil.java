package com.carlos.fx.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;

/**
 * JavaFX 工具类
 * 提供 JavaFX 常用操作的便捷方法
 *
 * <p>该类封装了 JavaFX 的常用操作，包括：</p>
 * <ul>
 *   <li>FXML 文件加载</li>
 *   <li>模态对话框创建</li>
 *   <li>窗口操作</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 加载 FXML 文件
 * Parent root = FxUtil.loadFxml("/fxml/myview.fxml");
 *
 * // 打开模态对话框
 * FxUtil.openModalAndWait("设置", root, primaryStage);
 *
 * // 关闭当前窗口
 * FxUtil.closeWindow(button);
 * }</pre>
 *
 * @author Carlos
 * @since 3.0.0
 */
public class FxUtil {

    /**
     * 加载 FXML 文件并返回根节点
     * 这是加载 FXML 文件最简单的方法，直接返回根节点
     *
     * <p>使用场景：只需要加载 FXML 文件，不需要访问控制器</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * try {
     *     // 加载 FXML 文件，返回根节点
     *     Parent root = FxUtil.loadFxml("/fxml/main.fxml");
     *
     *     // 创建场景
     *     Scene scene = new Scene(root);
     *     stage.setScene(scene);
     *     stage.show();
     * } catch (IOException e) {
     *     e.printStackTrace();
     * }
     * }</pre>
     *
     * @param <T>      根节点的类型（通常是 Parent 或其子类）
     * @param fxmlPath FXML 文件的路径（相对于 classpath，如 "/fxml/main.fxml"）
     * @return FXML 文件的根节点
     * @throws IOException 如果 FXML 文件不存在或加载失败
     */
    public static <T> T loadFxml(String fxmlPath) throws IOException {
        // 获取 FXML 文件的 URL
        URL resource = FxUtil.class.getResource(fxmlPath);
        if (resource == null) {
            throw new IllegalStateException("FXML file not found: " + fxmlPath);
        }

        // 创建 FXMLLoader 并加载 FXML 文件
        FXMLLoader loader = new FXMLLoader(resource);
        // load() 方法解析 FXML 文件，创建 UI 组件树，并返回根节点
        return loader.load();
    }

    /**
     * 加载 FXML 文件并返回 FXMLLoader
     * 如果需要访问控制器或进行更多配置，使用此方法
     *
     * <p>使用场景：需要获取控制器实例，或需要自定义 FXMLLoader 的配置</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 获取 FXMLLoader
     * FXMLLoader loader = FxUtil.getFxmlLoader("/fxml/main.fxml");
     *
     * // 加载 FXML 文件
     * Parent root = loader.load();
     *
     * // 获取控制器实例
     * MainController controller = loader.getController();
     * controller.setData(data);
     *
     * // 显示界面
     * Scene scene = new Scene(root);
     * stage.setScene(scene);
     * stage.show();
     * }</pre>
     *
     * @param fxmlPath FXML 文件的路径（相对于 classpath）
     * @return FXMLLoader 实例，可以用于加载 FXML 和获取控制器
     * @throws RuntimeException 如果 FXML 文件不存在
     */
    public static FXMLLoader getFxmlLoader(String fxmlPath) {
        // 获取 FXML 文件的 URL
        URL resource = FxUtil.class.getResource(fxmlPath);
        if (resource == null) {
            throw new IllegalStateException("FXML file not found: " + fxmlPath);
        }

        // 创建并返回 FXMLLoader
        return new FXMLLoader(resource);
    }

    /**
     * 打开模态对话框
     * 创建一个模态对话框并显示，但不等待对话框关闭
     *
     * <p>模态对话框（Modal Dialog）：</p>
     * <ul>
     *   <li>APPLICATION_MODAL：阻止用户与应用程序的其他窗口交互</li>
     *   <li>对话框打开时，用户必须先关闭对话框才能操作其他窗口</li>
     *   <li>适用于需要用户立即处理的情况（如设置、确认等）</li>
     * </ul>
     *
     * <p>使用场景：需要打开对话框但不阻塞当前代码执行</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 加载对话框内容
     * Parent root = FxUtil.loadFxml("/fxml/settings.fxml");
     *
     * // 打开模态对话框（不等待）
     * Stage dialog = FxUtil.openModal("设置", root, primaryStage);
     *
     * // 可以继续执行其他代码
     * System.out.println("对话框已打开");
     * }</pre>
     *
     * @param title 对话框标题
     * @param root  对话框的根节点（通常从 FXML 加载）
     * @param owner 父窗口（对话框将显示在父窗口前面）
     * @return 创建的 Stage 对象，可以用于后续操作（如关闭对话框）
     */
    public static Stage openModal(String title, Parent root, Window owner) {
        // 创建新的 Stage（窗口）
        Stage stage = new Stage();
        stage.setTitle(title);

        // 设置为应用程序模态
        // APPLICATION_MODAL：阻止用户与应用程序的其他窗口交互
        stage.initModality(Modality.APPLICATION_MODAL);

        // 设置父窗口
        // 对话框将显示在父窗口前面，并且关闭父窗口时对话框也会关闭
        stage.initOwner(owner);

        // 创建场景
        Scene scene = new Scene(root);

        // 加载 CSS 样式表
        // 使用主样式表，保持界面风格一致
        scene.getStylesheets().add(FxUtil.class.getResource("/css/main.css").toExternalForm());

        // 设置场景并显示窗口
        stage.setScene(scene);
        stage.show();  // show() 不会阻塞，立即返回

        return stage;
    }

    /**
     * 打开模态对话框并等待关闭
     * 创建一个模态对话框并显示，阻塞当前线程直到对话框关闭
     *
     * <p>与 openModal 的区别：</p>
     * <ul>
     *   <li>openModal：不阻塞，立即返回</li>
     *   <li>openModalAndWait：阻塞，等待对话框关闭后才返回</li>
     * </ul>
     *
     * <p>使用场景：需要等待用户完成对话框操作后再继续执行</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 加载对话框内容
     * FXMLLoader loader = FxUtil.getFxmlLoader("/fxml/input.fxml");
     * Parent root = loader.load();
     * InputController controller = loader.getController();
     *
     * // 打开模态对话框并等待
     * FxUtil.openModalAndWait("输入信息", root, primaryStage);
     *
     * // 对话框关闭后才会执行到这里
     * String result = controller.getResult();
     * System.out.println("用户输入: " + result);
     * }</pre>
     *
     * @param title 对话框标题
     * @param root  对话框的根节点（通常从 FXML 加载）
     * @param owner 父窗口（对话框将显示在父窗口前面）
     */
    public static void openModalAndWait(String title, Parent root, Window owner) {
        // 创建新的 Stage（窗口）
        Stage stage = new Stage();
        stage.setTitle(title);

        // 设置为应用程序模态
        stage.initModality(Modality.APPLICATION_MODAL);

        // 设置父窗口
        stage.initOwner(owner);

        // 创建场景
        Scene scene = new Scene(root);

        // 加载 CSS 样式表
        scene.getStylesheets().add(FxUtil.class.getResource("/css/main.css").toExternalForm());

        // 设置场景并显示窗口
        stage.setScene(scene);
        stage.showAndWait();  // showAndWait() 会阻塞当前线程，直到窗口关闭
    }

    /**
     * 关闭包含指定节点的窗口
     * 通过节点获取其所在的窗口并关闭
     *
     * <p>使用场景：在控制器中关闭当前窗口</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 在控制器中
     * @FXML
     * private Button closeButton;
     *
     * @FXML
     * private void handleClose() {
     *     // 关闭当前窗口
     *     FxUtil.closeWindow(closeButton);
     * }
     * }</pre>
     *
     * @param node 窗口中的任意节点（通常是按钮或其他 UI 组件）
     */
    public static void closeWindow(javafx.scene.Node node) {
        // 通过节点获取 Stage
        // node.getScene() 获取节点所在的场景
        // scene.getWindow() 获取场景所在的窗口
        Stage stage = (Stage) node.getScene().getWindow();

        // 关闭窗口
        stage.close();
    }

    /**
     * 从节点获取 Stage
     * 获取包含指定节点的窗口（Stage）
     *
     * <p>使用场景：需要操作当前窗口（如设置标题、大小等）</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 在控制器中
     * @FXML
     * private Button myButton;
     *
     * public void updateTitle() {
     *     // 获取当前窗口
     *     Stage stage = FxUtil.getStage(myButton);
     *
     *     // 修改窗口标题
     *     stage.setTitle("新标题");
     *
     *     // 设置窗口大小
     *     stage.setWidth(800);
     *     stage.setHeight(600);
     * }
     * }</pre>
     *
     * @param node 窗口中的任意节点
     * @return 包含该节点的 Stage 对象
     */
    public static Stage getStage(javafx.scene.Node node) {
        // 通过节点获取 Stage
        // node.getScene() 获取节点所在的场景
        // scene.getWindow() 获取场景所在的窗口，强制转换为 Stage
        return (Stage) node.getScene().getWindow();
    }
}
