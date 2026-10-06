package com.carlos.system.pojo.ao;

import com.carlos.system.enums.OperationStatus;
import com.carlos.system.enums.OperationType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 统一操作 API 传输对象
 * </p>
 *
 * @author carlos
 * @date 2026-07-11
 */
@Data
@Accessors(chain = true)
public class SysOperationAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 操作名称 */
    private String opName;

    /** 权限码 */
    private String permCode;

    /** 操作类型：1=按钮, 2=API */
    private OperationType opType;

    /** 状态：0禁用 1启用 */
    private OperationStatus opStatus;

}
