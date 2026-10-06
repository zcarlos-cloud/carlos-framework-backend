package com.carlos.system.menu.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 功能模块 数据传输对象
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Data
@Accessors(chain = true)
public class SysModuleDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 模块编码
     */
    private String moduleCode;

    /**
     * 排序
     */
    private Integer orderNum;

    /**
     * 状态
     */
    private Boolean moduleStatus;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
