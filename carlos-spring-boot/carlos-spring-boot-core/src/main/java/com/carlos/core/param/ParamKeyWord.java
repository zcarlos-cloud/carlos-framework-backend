package com.carlos.core.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>
 * 关键词搜索对象（包含基本分页参数）
 * </p>
 *
 * @author carlos
 * @date 2020/4/14 17:07
 */
@Data
@Schema(description = "关键词搜索对象")
public class ParamKeyWord implements Param {

    @Schema(description = "搜索字符串")
    private String keyword;

}
