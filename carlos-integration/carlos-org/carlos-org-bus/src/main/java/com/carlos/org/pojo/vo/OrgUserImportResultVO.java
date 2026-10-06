package com.carlos.org.pojo.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 用户导入结果VO
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户导入结果")
public class OrgUserImportResultVO {

    @Schema(description = "成功数量")
    private Integer successCount;

    @Schema(description = "失败数量")
    private Integer failCount;

    @Schema(description = "总数量")
    private Integer totalCount;

    @Schema(description = "错误信息列表")
    private List<String> errorMessages;

    @Schema(description = "是否成功")
    private Boolean success;

}
