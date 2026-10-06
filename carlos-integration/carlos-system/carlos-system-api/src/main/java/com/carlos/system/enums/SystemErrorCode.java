package com.carlos.system.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 系统管理模块错误码
 * <p>
 * 模块编码：09（系统管理服务）
 * <p>
 * 错误码格式：A-BB-CC
 * <ul>
 *   <li>A - 错误级别：1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：09系统管理</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SystemErrorCode implements ErrorCode {

    // ==================== 客户端错误 (1-09-xx) ====================
    SYS_PARAM_DICT_TYPE_EMPTY("10901", "字典类型不能为空", 400),
    SYS_PARAM_DICT_CODE_EMPTY("10902", "字典编码不能为空", 400),
    SYS_PARAM_CONFIG_KEY_EMPTY("10903", "配置键不能为空", 400),
    SYS_PARAM_FILE_EMPTY("10904", "上传文件不能为空", 400),
    SYS_PARAM_FILE_NAME_EMPTY("10905", "文件名不能为空", 400),
    SYS_PARAM_SEQUENCE_RULE_CODE_EMPTY("10906", "编码规则编码不能为空", 400),
    SYS_PARAM_SEQUENCE_BATCH_SIZE_INVALID("10907", "批量预取数量必须在 1~1000 之间", 400),
    SYS_PARAM_SEQUENCE_RESET_CYCLE_INVALID("10908", "重置周期取值不合法（1-永不重置 2-按日 3-按月 4-按年）", 400),
    SYS_PARAM_SEQUENCE_STEP_INVALID("10909", "步长必须为不小于 1 的整数", 400),

    // ==================== 业务错误 (2-09-xx) ====================
    // 字典相关
    SYS_DICT_NOT_FOUND("20901", "字典不存在", 404),
    SYS_DICT_ALREADY_EXISTS("20902", "字典已存在", 409),
    SYS_DICT_TYPE_NOT_FOUND("20903", "字典类型不存在", 404),
    SYS_DICT_TYPE_ALREADY_EXISTS("20904", "字典类型已存在", 409),
    SYS_DICT_ITEM_NOT_FOUND("20905", "字典项不存在", 404),
    SYS_DICT_ITEM_CODE_ALREADY_EXISTS("20906", "字典项编码已存在", 409),
    SYS_DICT_SYSTEM_CANNOT_DELETE("20907", "系统内置字典不能删除", 403),

    // 配置相关
    SYS_CONFIG_NOT_FOUND("20911", "系统配置不存在", 404),
    SYS_CONFIG_KEY_ALREADY_EXISTS("20912", "配置键已存在", 409),
    SYS_CONFIG_VALUE_TYPE_INVALID("20913", "配置值类型无效", 400),
    SYS_CONFIG_SYSTEM_CANNOT_DELETE("20914", "系统内置配置不能删除", 403),

    // i18n 相关
    SYS_I18N_NOT_FOUND("20971", "国际化消息不存在", 404),
    SYS_I18N_ALREADY_EXISTS("20972", "国际化消息已存在", 409),
    SYS_I18N_KEY_ALREADY_EXISTS("20973", "同语言同消息键已存在", 409),

    // 文件相关
    SYS_FILE_NOT_FOUND("20921", "文件不存在", 404),
    SYS_FILE_UPLOAD_FAILED("20922", "文件上传失败", 500),
    SYS_FILE_DOWNLOAD_FAILED("20923", "文件下载失败", 500),
    SYS_FILE_DELETE_FAILED("20924", "文件删除失败", 500),
    SYS_FILE_TYPE_NOT_ALLOWED("20925", "文件类型不允许", 400),
    SYS_FILE_SIZE_EXCEEDED("20926", "文件大小超出限制", 400),
    SYS_FILE_NAME_TOO_LONG("20927", "文件名过长", 400),
    SYS_FILE_CONTENT_EMPTY("20928", "文件内容为空", 400),

    // 菜单相关
    SYS_MENU_NOT_FOUND("20931", "菜单不存在", 404),
    SYS_MENU_ALREADY_EXISTS("20932", "菜单已存在", 409),
    SYS_MENU_CODE_ALREADY_EXISTS("20933", "菜单编码已存在", 409),
    SYS_MENU_HAS_CHILDREN("20934", "该菜单存在子菜单，不能删除", 400),
    SYS_MENU_PARENT_NOT_FOUND("20935", "上级菜单不存在", 404),
    SYS_MENU_PARENT_CANNOT_BE_SELF("20936", "上级菜单不能是自己", 400),
    SYS_MENU_SYSTEM_CANNOT_DELETE("20937", "系统内置菜单不能删除", 403),
    SYS_MENU_PARENT_CANNOT_BE_DESCENDANT("20938", "上级菜单不能是自己的子孙菜单", 400),

    // 资源相关
    SYS_RESOURCE_NOT_FOUND("20941", "资源不存在", 404),
    SYS_RESOURCE_ALREADY_EXISTS("20942", "资源已存在", 409),
    SYS_RESOURCE_CODE_ALREADY_EXISTS("20943", "资源编码已存在", 409),
    SYS_RESOURCE_HAS_DEPENDENCIES("20944", "资源存在依赖，不能删除", 400),

    // 地区相关
    SYS_REGION_NOT_FOUND("20951", "地区不存在", 404),
    SYS_REGION_CODE_ALREADY_EXISTS("20952", "地区编码已存在", 409),
    SYS_REGION_HAS_CHILDREN("20953", "该地区存在子地区，不能删除", 400),
    SYS_REGION_PARENT_NOT_FOUND("20954", "上级地区不存在", 404),
    SYS_REGION_PARENT_CANNOT_BE_SELF("20955", "上级地区不能是自己", 400),
    SYS_REGION_PARENT_CANNOT_BE_DESCENDANT("20956", "上级地区不能是自己的下级地区", 400),

    // 公告相关
    SYS_NEWS_NOT_FOUND("20961", "公告不存在", 404),
    SYS_NEWS_SCOPE_ROLES_EMPTY("20962", "指定角色发布时必须选择角色", 400),
    SYS_NEWS_STATUS_NOT_ALLOWED("20963", "已发布公告不可编辑或删除，请先下线", 400),
    SYS_NEWS_ALREADY_PUBLISHED("20964", "公告已发布", 409),
    SYS_NEWS_NOT_PUBLISHED("20965", "公告未发布", 400),
    SYS_NEWS_EXPIRED("20966", "公告已过有效期，请先修改有效期后再发布", 400),

    // 序列号/编码规则相关
    SYS_SEQUENCE_RULE_NOT_FOUND("20981", "编码规则不存在", 404),
    SYS_SEQUENCE_RULE_CODE_EXISTS("20982", "编码规则编码已存在", 409),
    SYS_SEQUENCE_DATE_PATTERN_INVALID("20983", "日期段格式不支持", 400),
    SYS_SEQUENCE_RESET_WITHOUT_DATE_PATTERN("20984", "未配置日期段时重置周期必须为「永不重置」", 400),
    SYS_SEQUENCE_OVERFLOW("20985", "序列号已超出位数上限，请增大流水位数或调整重置周期", 400),
    SYS_SEQUENCE_CYCLE_SHORTER_THAN_PERIOD("20986", "重置周期不能短于日期段粒度（按月的日期段不能配按日重置）", 400),

    // ==================== 第三方错误 (3-09-xx) ====================
    SYS_STORAGE_OSS_ERROR("30901", "对象存储服务异常", 500),
    SYS_STORAGE_MINIO_ERROR("30902", "MinIO 存储服务异常", 500),
    SYS_STORAGE_COS_ERROR("30903", "腾讯云存储服务异常", 500),
    SYS_STORAGE_OBS_ERROR("30904", "华为云存储服务异常", 500),

    // ==================== 系统错误 (5-09-xx) ====================
    SYS_SYSTEM_ERROR("50901", "系统管理内部错误", 500),
    SYS_CACHE_ERROR("50902", "系统缓存异常", 500),
    SYS_LOCK_ERROR("50903", "分布式锁异常", 500),
    SYS_RATE_LIMIT_ERROR("50904", "限流异常", 500),
    SYS_REGION_CACHE_INIT_FAILED("50907", "初始化区域缓存失败", 500),
    SYS_SEQUENCE_GENERATE_FAILED("50908", "序列号生成失败", 500),
    SYS_SEQUENCE_SAVE_FAILED("50909", "编码规则保存失败", 500),
    SYS_API_BEAN_MISSING("50910", "系统服务 Feign 客户端未装配", 500),
    SYS_API_INVOKE_FAILED("50911", "系统服务调用失败", 500),
    NOT_FOUND("50905", "数据不存在", 404);

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
    public BusinessException exception(String customMessage) {
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
    public BusinessException exception(String format, Object... args) {
        return new BusinessException(this, String.format(format, args));
    }
}
