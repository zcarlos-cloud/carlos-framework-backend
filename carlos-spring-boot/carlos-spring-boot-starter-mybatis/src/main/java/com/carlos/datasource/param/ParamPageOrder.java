package com.carlos.datasource.param;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 接口时间参数封装
 * </p>
 *
 * @author carlos
 * @date 2020/4/14 17:31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParamPageOrder extends ParamPage {

    @Schema(description = "排序字段")
    private List<OrderItem> sorts;
}
