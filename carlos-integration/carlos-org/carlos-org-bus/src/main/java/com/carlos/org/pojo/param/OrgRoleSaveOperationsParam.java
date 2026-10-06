package com.carlos.org.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 角色保存操作权限参数
 * </p>
 * <p>保存角色的操作权限（全量覆盖）</p>
 *
 * @author Carlos
 * @date 2026-07-11
 */
@Data
public class OrgRoleSaveOperationsParam {

    /**
     * 操作id列表
     *
     * <p>允许为空集合：本接口语义为「全量覆盖」，清空即代表撤销该角色的全部操作权限。
     * 若使用 @NotEmpty，前端取消勾选全部后保存会直接 400，权限无法回收。</p>
     */
    @NotNull(message = "操作id列表不能为空")
    @Schema(description = "操作id列表（sys_operation.id）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> operationIds;

}
