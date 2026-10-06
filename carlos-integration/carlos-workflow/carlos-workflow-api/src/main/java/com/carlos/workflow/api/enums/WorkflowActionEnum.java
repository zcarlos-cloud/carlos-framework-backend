package com.carlos.workflow.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 审批动作枚举，统一审批接口按本枚举分派到 Warm-Flow 的对应方法。
 * </p>
 * <p>
 * PASS / REJECT / FREE_JUMP 对应 {@code TaskService#skip}（skipType 取 PASS 或 REJECT，
 * FREE_JUMP 必须携带目标节点编码）；REJECT_LAST 对应 {@code TaskService#rejectLast}，
 * TAKE_BACK 对应 {@code TaskService#taskBack}；TRANSFER / DEPUTE / ADD_SIGNATURE / REDUCTION_SIGNATURE
 * 对应 {@code TaskService} 的办理人调整方法。
 * </p>
 * <p>
 * 本枚举位于 API 模块：它既是前端/调用方提交的入参类型（{@code ApiWorkflowHandleParam#action}），
 * 也是响应类型（{@code WorkflowHandleResultAO#action}），因此必须随契约一同发布。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "WorkflowAction")
@Getter
@AllArgsConstructor
public enum WorkflowActionEnum implements BaseEnum {

    PASS(1, "审批通过"),
    REJECT(2, "驳回"),
    FREE_JUMP(3, "自由跳转"),
    TRANSFER(4, "转办"),
    DEPUTE(5, "委托"),
    ADD_SIGNATURE(6, "加签"),
    REDUCTION_SIGNATURE(7, "减签"),
    REJECT_LAST(8, "退回上一节点"),
    TAKE_BACK(9, "拿回");

    @EnumValue
    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    /**
     * 是否为流程流转动作（走 {@code TaskService#skip}）
     *
     * @return true 表示流转动作
     */
    public boolean flowSkip() {
        return this == PASS || this == REJECT || this == FREE_JUMP;
    }

    /**
     * 是否为办理人调整动作（增加办理人或减少办理人）
     *
     * @return true 表示需要目标办理人
     */
    public boolean handlerAdjust() {
        return this == TRANSFER || this == DEPUTE || this == ADD_SIGNATURE || this == REDUCTION_SIGNATURE;
    }

    /**
     * 是否为「退回上一节点」动作（走 {@code TaskService#rejectLast}）
     *
     * @return true 表示退回上一节点
     */
    public boolean rejectLast() {
        return this == REJECT_LAST;
    }

    /**
     * 是否为「拿回」动作（走 {@code TaskService#taskBack}）
     *
     * @return true 表示拿回
     */
    public boolean takeBack() {
        return this == TAKE_BACK;
    }

    /**
     * 按枚举标识查询
     *
     * @param code 枚举标识
     * @return 枚举，未匹配返回 null
     */
    public static WorkflowActionEnum of(Integer code) {
        for (WorkflowActionEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

}
