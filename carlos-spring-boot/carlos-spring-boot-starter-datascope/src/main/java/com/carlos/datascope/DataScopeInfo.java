package com.carlos.datascope;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * <p>
 * 数据权限信息
 * </p>
 *
 * @author Carlos
 * @date 2022/11/22 15:21
 */
@Data
public class DataScopeInfo {

    /**
     * 条件字段
     */
    private String column;

    /**
     * 查询取值
     */
    private Set<Serializable> items;
}
