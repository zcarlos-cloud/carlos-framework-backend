package com.carlos.datasource.config;

/**
 * <p>
 * mybatis通用信息获取接口
 * </p>
 *
 * @author carlos
 * @date 2020/6/3 11:09
 */
public interface MybatisCommonField {


    /**
     * 主键字段名称
     *
     * @return java.lang.String
     * @author carlos
     * @date 2021/11/12 10:37
     */
    String primaryKeyFiledName();

    /**
     * 修改时间字段名称
     *
     * @return java.lang.String
     * @author carlos
     * @date 2021/11/12 10:38
     */
    String updateTimeFiledName();

    /**
     * 修改人字段名称
     *
     * @return java.lang.String
     * @author carlos
     * @date 2021/11/12 10:38
     */
    String updateUserFiledName();

    /**
     * 创建时间字段名称
     *
     * @return java.lang.String
     * @author carlos
     * @date 2021/11/12 10:38
     */
    String createTimeFiledName();

    /**
     * 创建人字段名称
     *
     * @return java.lang.String
     * @author carlos
     * @date 2021/11/12 10:38
     */
    String createUserFiledName();

    /**
     * 逻辑删除字段名称
     *
     * @return java.lang.String
     * @author carlos
     * @date 2021/11/12 10:38
     */
    String logicDeleteFiledName();

    /**
     * 版本号字段名称
     *
     * @return java.lang.String
     * @author carlos
     * @date 2021/11/12 10:38
     */
    String versionFiledName();
}
