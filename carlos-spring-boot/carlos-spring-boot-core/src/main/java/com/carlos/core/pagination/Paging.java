package com.carlos.core.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 自定义分页结果对象
 * </p>
 *
 * @author carlos
 * @date 2020/6/16 13:40
 */
@Data
@Schema(description = "分页结果对象")
@Accessors(chain = true)
public class Paging<T> implements Serializable {

    private static final long serialVersionUID = 4784961132604516495L;

    @Schema(description = "总行数")
    private int total;

    @Schema(description = "数据列表")
    private List<T> records;

    @Schema(description = "当前页码")
    private int current;

    @Schema(description = "页大小")
    private int size;

    @Schema(description = "总页数")
    private int pages;

}
