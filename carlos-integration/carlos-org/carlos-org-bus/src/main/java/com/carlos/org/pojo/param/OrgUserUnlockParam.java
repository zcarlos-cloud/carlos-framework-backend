package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户解锁参数
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户解锁参数")
public class OrgUserUnlockParam {

    @NotNull(message = "用户id不能为空")
    @Schema(description = "用户主键")
    private Long id;

}
