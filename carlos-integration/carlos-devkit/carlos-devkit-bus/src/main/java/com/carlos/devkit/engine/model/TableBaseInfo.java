package com.carlos.devkit.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p>
 * 表基本信息（表清单：表名 + 备注）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Accessors(chain = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableBaseInfo {

    /**
     * 表名
     */
    private String name;

    /**
     * 表备注
     */
    private String comment;

    @Override
    public String toString() {
        return name + " [" + comment + "]";
    }
}
