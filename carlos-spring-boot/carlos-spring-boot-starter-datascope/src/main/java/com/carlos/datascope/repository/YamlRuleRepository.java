package com.carlos.datascope.repository;

import com.carlos.datascope.core.model.DataScopeRule;
import com.carlos.datascope.core.model.ScopeDimension;
import com.carlos.datascope.properties.DataScopeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * YAML 配置文件规则存储实现
 * <p>
 * 从YAML配置文件加载规则
 *
 * @author Carlos
 * @version 2.0
 */
@Slf4j
@RequiredArgsConstructor
public class YamlRuleRepository implements RuleRepository {

    private final DataScopeProperties properties;

    @Override
    public DataScopeRule findById(String ruleId) {
        return null;
    }

    @Override
    public List<DataScopeRule> findAll() {
        List<DataScopeRule> rules = new ArrayList<>();

        if (properties.getGlobalRules() != null) {
            for (DataScopeProperties.RuleConfig config : properties.getGlobalRules()) {
                rules.add(convertToRule(config, "global"));
            }
        }

        if (properties.getTableRules() != null) {
            for (Map.Entry<String, DataScopeProperties.RuleConfig> entry : properties.getTableRules().entrySet()) {
                rules.add(convertToRule(entry.getValue(), entry.getKey()));
            }
        }

        return rules;
    }

    @Override
    public List<DataScopeRule> findByMapperMethod(String mapperMethod) {
        return findAll();
    }

    @Override
    public List<DataScopeRule> findByTableName(String tableName) {
        return findAll().stream()
            .filter(rule -> rule.matchesTable(tableName))
            .collect(Collectors.toList());
    }

    @Override
    public void save(DataScopeRule rule) {
        throw new UnsupportedOperationException("YAML repository is read-only");
    }

    @Override
    public void delete(String ruleId) {
        throw new UnsupportedOperationException("YAML repository is read-only");
    }

    private DataScopeRule convertToRule(DataScopeProperties.RuleConfig config, String tableName) {
        DataScopeRule rule = new DataScopeRule();
        rule.setRuleId("yaml:" + tableName + ":" + config.getDimension());
        rule.setRuleName("YAML Rule for " + tableName);
        rule.setType(DataScopeRule.RuleType.ROW);
        rule.setDimension(ScopeDimension.valueOf(config.getDimension()));
        rule.setField(config.getField());
        rule.setPriority(config.getPriority());
        rule.setEnabled(config.isEnabled());
        rule.setSource(DataScopeRule.RuleSource.YAML);

        Set<String> tables = new HashSet<>();
        if (config.getTables() != null) {
            tables.addAll(config.getTables());
        } else if (!"global".equals(tableName)) {
            tables.add(tableName);
        }
        rule.setTableNames(tables);

        return rule;
    }
}
