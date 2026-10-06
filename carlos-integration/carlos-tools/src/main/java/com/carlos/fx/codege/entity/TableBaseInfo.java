package com.carlos.fx.codege.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p>
 * 表基本信息
 * </p>
 *
 * @author Carlos
 * @date 2021/6/27 23:27
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


    // 重写toString方法
    @Override
    public String toString() {
        return name + " [" + comment + "]";
    }

}
