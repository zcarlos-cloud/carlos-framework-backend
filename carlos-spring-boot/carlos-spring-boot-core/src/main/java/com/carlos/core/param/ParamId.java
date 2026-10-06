package com.carlos.core.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 * 公用ID参数
 * </p>
 *
 * @author carlos
 * @date 2020/4/14 11:41
 */
@Data
@Schema(description = "ID参数")
public class ParamId<T> implements Param {

    @NotNull(message = "ID不能为空")
    private T id;

}
