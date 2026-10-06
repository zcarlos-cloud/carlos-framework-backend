package com.carlos.datascope.repository;

import com.carlos.datascope.core.model.DataScopeRule;

import java.util.List;

/**
 * 规则存储接口
 * <p>
 * 定义规则的存储和查询操作
 *
 * @author Carlos
 * @version 2.0
 */
public interface RuleRepository {

    /**
     * 根据ID查询规则
     *
     * @param ruleId 规则ID
     * @return 规则
     */
    DataScopeRule findById(String ruleId);

    /**
     * 查询所有规则
     *
     * @return 规则列表
     */
    List<DataScopeRule> findAll();

    /**
     * 根据Mapper方法查询规则
     *
     * @param mapperMethod Mapper方法全名（类名.方法名）
     * @return 规则列表
     */
    List<DataScopeRule> findByMapperMethod(String mapperMethod);

    /**
     * 根据表名查询规则
     *
     * @param tableName 表名
     * @return 规则列表
     */
    List<DataScopeRule> findByTableName(String tableName);

    /**
     * 保存规则
     *
     * @param rule 规则
     */
    void save(DataScopeRule rule);

    /**
     * 删除规则
     *
     * @param ruleId 规则ID
     */
    void delete(String ruleId);
}
