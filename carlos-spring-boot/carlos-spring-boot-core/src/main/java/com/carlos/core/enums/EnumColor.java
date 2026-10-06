package com.carlos.core.enums;

/**
 * 枚举的语义色档位（5 档）。
 *
 * <p>枚举字典把「值 → 文案」交给后端之后，颜色也必须一并下发。否则前端仍要为每个枚举值
 * 维护一张颜色表，加一个枚举值照样得改前端，「前端零改动」就不成立。</p>
 *
 * <p>这里只定义 5 档<strong>语义</strong>，具体渲染成什么颜色由前端决定
 * （例如 antd Tag 的 processing / success / warning / error / default）。
 * 语义集合是稳定的，因此前端那张映射表永远不需要改。</p>
 *
 * <p>本枚举<strong>不实现 {@link BaseEnum}</strong>、也不标注 {@code @AppEnum} ——
 * 它是渲染语义而非业务字典，不应出现在 {@code /app/enums} 里。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public enum EnumColor {

    /** 进行中 / 中性偏主色 */
    PRIMARY,

    /** 成功 / 已完成 */
    SUCCESS,

    /** 警告 / 待处理 */
    WARNING,

    /** 失败 / 危险 */
    DANGER,

    /** 无强调 / 已归档 / 未激活 */
    MUTED
}
