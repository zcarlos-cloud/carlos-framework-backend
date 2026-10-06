package com.carlos.sample.disruptor.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事件
 * <p>
 * 用于演示 Disruptor 事件处理的业务数据对象
 * </p>
 *
 * @author Carlos
 * @date 2026-03-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 订单状态枚举
     */
    public enum Status {
        PENDING(0, "待处理"),
        VALIDATED(1, "已验证"),
        PROCESSING(2, "处理中"),
        COMPLETED(3, "已完成"),
        FAILED(4, "处理失败");

        private final int code;
        private final String desc;

        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }
}
