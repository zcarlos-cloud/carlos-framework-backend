package com.carlos.core.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import lombok.Data;

/**
 * <p>
 * 分页请求参数（不带字段查询）
 * </p>
 *
 * @author carlos
 * @date 2020/4/14 17:07
 */
@Data
@Schema(description = "分页参数")
public class ParamPage implements Param {

    private static final long serialVersionUID = -3263921252635611410L;

    @Schema(description = "页码,默认为1", example = "1")
    private int current = 1;

    @Schema(description = "页大小,默认为10", example = "10")
    @Max(value = 1000, message = "页大小不能超过1000")
    private int size = 10;

    public void setCurrent(int current) {
        if (current <= 0) {
            this.current = 1;
        } else {
            this.current = current;
        }
    }

    public void setSize(int size) {
        if (size <= 0) {
            this.size = 10;
        } else {
            this.size = size;
        }
    }

}
