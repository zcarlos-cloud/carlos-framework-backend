package com.carlos.system.menu.pojo.dto;


import com.carlos.system.enums.OperationStatus;
import com.carlos.system.enums.OperationType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 统一操作表 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@Accessors(chain = true)
public class SysOperationDTO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 操作类型：1=按钮, 2=API
     */
    private OperationType opType;

    /**
     * 所属菜单ID（按钮必填，API可选）
     */
    private Long menuId;

    /**
     * 操作名称，如"新增用户"
     */
    private String opName;

    /**
     * 权限码，如 user:create
     */
    private String permCode;

    /**
     * 图标（仅按钮）
     */
    private String icon;

    /**
     * 排序
     */
    private Integer orderNum;

    /**
     * API路径（仅API）
     */
    private String apiPath;

    /**
     * HTTP方法（仅API）
     */
    private String apiMethod;

    /**
     * 是否自动扫描生成：0否 1是（仅API）
     */
    private Boolean autoScan;

    /**
     * 状态：0禁用 1启用
     */
    private OperationStatus opStatus;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 修改人
     */
    private Long updateBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

}
