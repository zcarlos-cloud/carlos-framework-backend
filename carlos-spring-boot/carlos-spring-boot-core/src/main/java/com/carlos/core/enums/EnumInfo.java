package com.carlos.core.enums;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 枚举类型VO, 用于列举枚举字典
 * </p>
 *
 * @author carlos
 * @date 2020/4/12 2:58
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class EnumInfo implements Serializable {

    /**
     * 枚举code
     */
    private Object code;

    /**
     * 枚举描述
     */
    private String desc;

    /**
     * 枚举名称
     */
    private String name;

    /**
     * 英文描述（未提供时回落 {@link #desc}）
     */
    private String descEn;

    /**
     * 语义色档位（见 {@link EnumColor}）
     */
    private EnumColor color;
}
