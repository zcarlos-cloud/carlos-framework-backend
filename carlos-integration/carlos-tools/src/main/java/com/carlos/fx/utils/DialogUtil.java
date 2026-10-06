package com.carlos.fx.utils;

import com.carlos.fx.ToolsApplication;
import javafx.application.HostServices;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import org.controlsfx.control.Notifications;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * 对话框工具类
 * 提供各种类型的对话框和通知的便捷方法
 *
 * <p>该类封装了 JavaFX 的 Alert 和 ControlsFX 的 Notifications，
 * 提供统一的对话框和通知接口，简化 UI 交互代码。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>信息对话框 - 显示普通信息</li>
 *   <li>警告对话框 - 显示警告信息</li>
 *   <li>错误对话框 - 显示错误信息</li>
 *   <li>确认对话框 - 需要用户确认的操作</li>
 *   <li>输入对话框 - 获取用户输入</li>
 *   <li>通知消息 - 右下角弹出的通知</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 显示信息对话框
 * DialogUtil.showInfo("提示", "操作成功");
 *
 * // 显示确认对话框
 * boolean confirmed = DialogUtil.showConfirm("确认删除", "确定要删除这条记录吗？");
 * if (confirmed) {
 *     // 执行删除操作
 * }
 *
 * // 显示成功通知（右下角弹出）
 * DialogUtil.showSuccessNotification("成功", "数据已保存");
 *
 * // 显示错误对话框（带异常信息）
 * try {
 *     // 执行操作
 * } catch (Exception e) {
 *     DialogUtil.showError("错误", "操作失败", e);
 * }
 * }</pre>
 *
 * @author Carlos
 * @since 3.0.0
 */
public class DialogUtil {

    /**
     * 显示信息对话框
     * 用于显示普通的提示信息，用户点击"确定"后关闭
     *
     * <p>对话框类型：INFORMATION（蓝色图标）</p>
     * <p>按钮：确定</p>
     *
     * @param title   对话框标题
     * @param message 要显示的消息内容
     */
    public static void showInfo(String title, String message) {
        // 创建信息类型的对话框
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        // 不显示头部文本（headerText），只显示内容文本
        alert.setHeaderText(null);
        alert.setContentText(message);
        // showAndWait() 会阻塞当前线程，直到用户关闭对话框
        alert.showAndWait();
    }

    /**
     * 显示警告对话框
     * 用于显示警告信息，提醒用户注意某些情况
     *
     * <p>对话框类型：WARNING（黄色图标）</p>
     * <p>按钮：确定</p>
     *
     * @param title   对话框标题
     * @param message 警告消息内容
     */
    public static void showWarning(String title, String message) {
        // 创建警告类型的对话框
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示错误对话框
     * 用于显示错误信息，告知用户操作失败
     *
     * <p>对话框类型：ERROR（红色图标）</p>
     * <p>按钮：确定</p>
     *
     * @param title   对话框标题
     * @param message 错误消息内容
     */
    public static void showError(String title, String message) {
        // 创建错误类型的对话框
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示错误对话框（带异常详情）
     * 用于显示错误信息和异常的详细信息
     *
     * <p>对话框类型：ERROR（红色图标）</p>
     * <p>按钮：确定</p>
     * <p>显示内容：headerText 显示自定义消息，contentText 显示异常消息</p>
     *
     * <p>使用场景：捕获异常后显示给用户</p>
     * <pre>{@code
     * try {
     *     // 执行可能抛出异常的操作
     *     service.deleteData(id);
     * } catch (Exception e) {
     *     DialogUtil.showError("删除失败", "无法删除数据", e);
     * }
     * }</pre>
     *
     * @param title     对话框标题
     * @param message   错误消息（显示在 headerText）
     * @param throwable 异常对象（异常消息显示在 contentText）
     */
    public static void showError(String title, String message, Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        // headerText 显示自定义的错误消息
        alert.setHeaderText(message);
        // contentText 显示异常的详细消息
        alert.setContentText(throwable.getMessage());
        alert.showAndWait();
    }

    /**
     * 显示确认对话框
     * 用于需要用户确认的操作，返回用户的选择
     *
     * <p>对话框类型：CONFIRMATION（问号图标）</p>
     * <p>按钮：确定、取消</p>
     *
     * <p>使用场景：删除操作、覆盖文件、退出程序等需要用户确认的操作</p>
     * <pre>{@code
     * boolean confirmed = DialogUtil.showConfirm("确认删除", "确定要删除这条记录吗？");
     * if (confirmed) {
     *     // 用户点击了"确定"，执行删除操作
     *     deleteRecord();
     * } else {
     *     // 用户点击了"取消"，不执行任何操作
     * }
     * }</pre>
     *
     * @param title   对话框标题
     * @param message 确认消息内容
     * @return true 表示用户点击了"确定"，false 表示用户点击了"取消"或关闭了对话框
     */
    public static boolean showConfirm(String title, String message) {
        // 创建确认类型的对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // showAndWait() 返回 Optional<ButtonType>
        Optional<ButtonType> result = alert.showAndWait();
        // 检查用户是否点击了"确定"按钮
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * 显示文本输入对话框
     * 用于获取用户输入的文本
     *
     * <p>对话框包含一个文本输入框，用户可以输入文本并点击"确定"或"取消"</p>
     *
     * <p>使用场景：获取用户输入的名称、描述、备注等文本信息</p>
     * <pre>{@code
     * Optional<String> result = DialogUtil.showInput("输入名称", "请输入分支名称", "feature/");
     * result.ifPresent(branchName -> {
     *     // 用户输入了文本并点击了"确定"
     *     createBranch(branchName);
     * });
     * }</pre>
     *
     * @param title        对话框标题
     * @param header       对话框头部文本（提示信息）
     * @param defaultValue 输入框的默认值
     * @return Optional<String> 如果用户点击了"确定"，返回包含输入文本的 Optional；
     *         如果用户点击了"取消"或关闭了对话框，返回空的 Optional
     */
    public static Optional<String> showInput(String title, String header, String defaultValue) {
        // 创建文本输入对话框
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("请输入:");

        // showAndWait() 返回 Optional<String>
        return dialog.showAndWait();
    }

    /**
     * 显示成功通知
     * 在屏幕右下角显示一个成功通知，几秒后自动消失
     *
     * <p>通知类型：INFORMATION（绿色背景）</p>
     * <p>显示位置：屏幕右下角</p>
     * <p>自动消失：是（约 5 秒后）</p>
     *
     * <p>使用场景：操作成功后的提示，不需要用户点击确认</p>
     * <pre>{@code
     * // 保存数据后显示成功通知
     * saveData();
     * DialogUtil.showSuccessNotification("保存成功", "数据已成功保存到服务器");
     * }</pre>
     *
     * @param title   通知标题
     * @param message 通知消息内容
     */
    public static void showSuccessNotification(String title, String message) {
        // 使用 ControlsFX 的 Notifications 创建通知
        Notifications.create()
                .title(title)
                .text(message)
                .showInformation();  // 显示信息类型的通知（绿色）
    }

    /**
     * 显示错误通知
     * 在屏幕右下角显示一个错误通知，几秒后自动消失
     *
     * <p>通知类型：ERROR（红色背景）</p>
     * <p>显示位置：屏幕右下角</p>
     * <p>自动消失：是（约 5 秒后）</p>
     *
     * <p>使用场景：操作失败后的提示，不需要用户点击确认</p>
     *
     * @param title   通知标题
     * @param message 通知消息内容
     */
    public static void showErrorNotification(String title, String message) {
        Notifications.create()
                .title(title)
                .text(message)
                .showError();  // 显示错误类型的通知（红色）
    }

    /**
     * 显示警告通知
     * 在屏幕右下角显示一个警告通知，几秒后自动消失
     *
     * <p>通知类型：WARNING（黄色背景）</p>
     * <p>显示位置：屏幕右下角</p>
     * <p>自动消失：是（约 5 秒后）</p>
     *
     * <p>使用场景：需要提醒用户注意的情况</p>
     *
     * @param title   通知标题
     * @param message 通知消息内容
     */
    public static void showWarningNotification(String title, String message) {
        Notifications.create()
                .title(title)
                .text(message)
                .showWarning();  // 显示警告类型的通知（黄色）
    }

    /**
     * 显示信息通知
     * 在屏幕右下角显示一个信息通知，几秒后自动消失
     *
     * <p>通知类型：INFORMATION（蓝色背景）</p>
     * <p>显示位置：屏幕右下角</p>
     * <p>自动消失：是（约 5 秒后）</p>
     *
     * <p>使用场景：普通的提示信息</p>
     *
     * @param title   通知标题
     * @param message 通知消息内容
     */
    public static void showInfoNotification(String title, String message) {
        Notifications.create()
                .title(title)
                .text(message)
                .showInformation();  // 显示信息类型的通知（蓝色）
    }

    /**
     * 显示成功对话框，询问是否打开输出目录
     * <p>
     * 用于代码生成或项目生成成功后，询问用户是否打开输出目录。
     * 如果用户点击"打开目录"，则使用系统默认文件管理器打开该目录。
     * </p>
     *
     * <p>对话框类型：INFORMATION（蓝色图标）</p>
     * <p>按钮：打开目录、关闭</p>
     *
     * <p>使用场景：</p>
     * <ul>
     *   <li>代码生成完成后</li>
     *   <li>项目生成完成后</li>
     *   <li>文件导出完成后</li>
     * </ul>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Path outputPath = Paths.get("D:/projects/generated");
     * DialogUtil.showSuccessWithOpenDirectory(
     *     "生成成功",
     *     "代码已成功生成！",
     *     outputPath
     * );
     * }</pre>
     *
     * @param title      对话框标题
     * @param message    成功消息内容
     * @param outputPath 输出目录路径
     */
    public static void showSuccessWithOpenDirectory(String title, String message, Path outputPath) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText("输出位置: " + outputPath);

        ButtonType openButton = new ButtonType("打开目录");
        ButtonType closeButton = new ButtonType("关闭", ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(openButton, closeButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == openButton) {
            try {
                HostServices hostServices = ToolsApplication.getApplicationHostServices();
                if (hostServices == null) {
                    showWarning("警告", """
                        无法获取HostServices，无法打开目录
                        目录位置: %s""".formatted(outputPath));
                    return;
                }
                // 使用JavaFX的HostServices打开目录
                hostServices.showDocument(outputPath.toUri().toString());
            } catch (Exception e) {
                showError("错误", "打开目录时发生异常", e);
            }
        }
    }

    /**
     * 显示错误对话框，询问是否打开日志文件
     * <p>
     * 用于操作失败后，显示错误信息并询问用户是否打开日志文件查看详细错误。
     * 对话框包含可展开的异常堆栈信息，方便用户查看详细错误。
     * </p>
     *
     * <p>对话框类型：ERROR（红色图标）</p>
     * <p>按钮：打开日志、关闭</p>
     * <p>可展开内容：完整的异常堆栈信息</p>
     *
     * <p>使用场景：</p>
     * <ul>
     *   <li>代码生成失败</li>
     *   <li>项目生成失败</li>
     *   <li>数据库连接失败</li>
     *   <li>文件操作失败</li>
     * </ul>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * try {
     *     generateCode();
     * } catch (Exception e) {
     *     DialogUtil.showErrorWithOpenLog(
     *         "生成失败",
     *         "代码生成过程中发生错误",
     *         e
     *     );
     * }
     * }</pre>
     *
     * @param title   对话框标题
     * @param message 错误消息内容
     * @param error   异常对象
     */
    public static void showErrorWithOpenLog(String title, String message, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(error.getMessage());

        // 添加可展开的异常堆栈
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);

        TextArea textArea = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        alert.getDialogPane().setExpandableContent(textArea);

        ButtonType openLogButton = new ButtonType("打开日志");
        ButtonType closeButton = new ButtonType("关闭", ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(openLogButton, closeButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == openLogButton) {
            try {
                Path logFile = Paths.get(System.getProperty("user.home"),
                        ".carlos-tools", "logs", "application.log");
                if (Files.exists(logFile)) {
                    HostServices hostServices = ToolsApplication.getApplicationHostServices();
                    if (hostServices == null) {
                        showWarning("警告", """
                            无法获取HostServices，无法打开日志文件
                            日志位置: %s""".formatted(logFile));
                        return;
                    }
                    // 使用JavaFX的HostServices打开日志文件
                    hostServices.showDocument(logFile.toUri().toString());
                } else {
                    showWarning("警告", "日志文件不存在: " + logFile);
                }
            } catch (Exception e) {
                showError("错误", "打开日志文件时发生异常", e);
            }
        }
    }
}
