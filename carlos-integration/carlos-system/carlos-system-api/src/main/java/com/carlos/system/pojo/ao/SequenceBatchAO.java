package com.carlos.system.pojo.ao;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 序列号批量预取结果 API 对象
 * </p>
 *
 * <p>S5 序列号：一次预取一个号段（段内编码已由服务方分配，调用方自行拆分使用）。</p>
 *
 * <p><b>段内取值语义</b>：段内**可用**编码共 {@code count} 个，从 {@code startCode} 起
 * 每隔 {@code step - 1} 个流水值取一个，最后一个是 {@code endCode}，即</p>
 *
 * <pre>
 *   startCode, startCode + step, startCode + 2*step, …, endCode
 * </pre>
 *
 * <p>{@code step = 1} 时即连续号段。调用方若只关心"不重复"，取 {@code startCode} 即可；
 * 若要把段内每个编码都发出去，必须按 {@code step} 等步长推进（这也是本对象**必须**回传
 * {@code step} 的原因 —— 早期只有 start/end，两个调用方可能一个按连续、一个按步长拆分，
 * 从而推出互不相同的编码集合）。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SequenceBatchAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 号段第一个已分配编码（含）
     */
    private String startCode;
    /**
     * 号段最后一个已分配编码（含）
     */
    private String endCode;
    /**
     * 号段可用编码数量
     */
    private Integer count;
    /**
     * 段内编码的流水步长（1 表示连续号段）
     */
    private Integer step;
    /**
     * 号段所属日期段（无日期段的规则为空串）
     */
    private String period;

}
