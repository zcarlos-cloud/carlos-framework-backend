package com.carlos.core.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

/**
 * <p>
 * 公用ID参数
 * </p>
 *
 * @author carlos
 * @date 2020/4/14 11:41
 */
@Data
@Schema(description = "Set集合参数")
public class ParamIdSet<T> implements Param {

    @NotEmpty(message = "ID集合不能为空")
    private Set<T> ids;
}
