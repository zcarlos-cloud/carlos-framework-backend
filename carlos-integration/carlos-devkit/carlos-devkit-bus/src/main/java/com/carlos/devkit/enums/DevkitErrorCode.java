package com.carlos.devkit.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 开发工具平台错误码
 * <p>
 * 模块编码：10（开发工具平台，10-99 预留段）
 * <p>
 * 错误码格式：A-BB-CC
 * <ul>
 *   <li>A - 错误级别：1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：10开发工具平台</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 * 对齐《DESIGN-20261006-开发工具平台功能设计》第 4.6 节
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Getter
@RequiredArgsConstructor
public enum DevkitErrorCode implements ErrorCode {

    // ==================== 模板（1-10-xx 客户端 / 2-10-xx 业务） ====================
    TEMPLATE_GROUP_NOT_FOUND("21001", "模板组不存在或已删除", 404),
    TEMPLATE_FILE_NOT_FOUND("21002", "模板文件不存在或已删除", 404),
    TEMPLATE_PATH_INVALID("21003", "模板路径非法", 400),
    TEMPLATE_RENDER_FAILED("21004", "模板渲染失败", 400),
    TEMPLATE_GROUP_CODE_EXISTS("21005", "模板组编码已存在", 400),
    TEMPLATE_GROUP_DISABLED("21006", "模板组已停用，无法生成", 400),
    TEMPLATE_FILE_PATH_EXISTS("21007", "同路径的模板文件已存在", 400),
    TEMPLATE_BUILT_IN_READONLY("21008", "内置模板只读，不可删除", 400),
    TEMPLATE_CONTENT_TOO_LARGE("11009", "模板正文超出大小上限", 400),
    TEMPLATE_RENDER_TIMEOUT("21010", "模板渲染超时", 400),

    // ==================== 数据源 ====================
    DATASOURCE_NOT_FOUND("21101", "数据源不存在或已删除", 404),
    DATASOURCE_CONNECT_FAILED("21102", "数据源连接失败", 400),
    DATASOURCE_DISABLED("21103", "数据源已停用", 400),
    DATASOURCE_DB_TYPE_UNSUPPORTED("21104", "不支持的数据库类型", 400),
    DATASOURCE_PRIVACY_READONLY("21105", "只读数据源不允许执行非元数据操作", 400),

    // ==================== 生成 ====================
    CODEGEN_NO_TABLE_SELECTED("21201", "请至少选择一张数据表", 400),
    GENERATE_OUTPUT_TOO_LARGE("21202", "生成产物超出大小上限，请减少表数量", 400),
    GENERATE_PARAM_INVALID("21203", "生成参数不合法", 400),
    GENERATE_FAILED("21204", "生成失败", 400),

    // ==================== 系统错误 ====================
    GENERATE_ENGINE_ERROR("51001", "生成引擎内部错误", 500);

    private final String code;

    private final String message;

    private final int httpStatus;

    /**
     * 创建业务异常（使用默认消息）
     *
     * @return BusinessException
     */
    @Override
    public BusinessException exception() {
        return new BusinessException(this);
    }

    /**
     * 创建业务异常（自定义消息）
     *
     * @param customMessage 自定义消息
     * @return BusinessException
     */
    @Override
    public BusinessException exception(final String customMessage) {
        return new BusinessException(this, customMessage);
    }

    /**
     * 创建业务异常（格式化消息）
     *
     * @param format 格式字符串
     * @param args   参数
     * @return BusinessException
     */
    @Override
    public BusinessException exception(final String format, final Object... args) {
        return new BusinessException(this, String.format(format, args));
    }
}
