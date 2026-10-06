package com.carlos.datasource.config;

/**
 * <p>
 * mybatis通用信息获取接口
 * </p>
 *
 * @author carlos
 * @date 2020/6/3 11:09
 */
public abstract class AbstractMybatisCommonField implements MybatisCommonField {

    @Override
    public String primaryKeyFiledName() {
        return null;
    }

    @Override
    public String updateTimeFiledName() {
        return null;
    }

    @Override
    public String updateUserFiledName() {
        return null;
    }

    @Override
    public String createTimeFiledName() {
        return null;
    }

    @Override
    public String createUserFiledName() {
        return null;
    }

    @Override
    public String logicDeleteFiledName() {
        return null;
    }

    @Override
    public String versionFiledName() {
        return null;
    }
}
