package com.carlos.core.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 部门信息
 * </p>
 *
 * @author carlos
 * @date 2020/4/12 2:58
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentInfo implements Serializable {

    /**
     * 部门id
     */
    private Serializable id;

    /**
     * 部门code
     */
    private String code;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 部门全名
     */
    private List<String> fullName;

}
