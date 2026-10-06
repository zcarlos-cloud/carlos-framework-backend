package com.carlos.datascope.core.engine;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 规则执行上下文
 * <p>
 * 包含执行规则所需的所有变量和参数
 *
 * @author Carlos
 * @version 2.0
 */
@Data
public class ExecutionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 变量映射
     */
    private final Map<String, Object> variables = new HashMap<>();

    /**
     * 当前用户ID
     */
    private Serializable currentUserId;

    /**
     * 当前部门ID
     */
    private Serializable currentDeptId;

    /**
     * 当前角色ID列表
     */
    private Iterable<? extends Serializable> currentRoleIds;

    /**
     * 方法参数
     */
    private Object[] args;

    /**
     * 设置变量
     *
     * @param name  变量名
     * @param value 变量值
     */
    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    /**
     * 获取变量
     *
     * @param name 变量名
     * @return 变量值
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String name) {
        return (T) variables.get(name);
    }

    /**
     * 获取变量，带默认值
     *
     * @param name         变量名
     * @param defaultValue 默认值
     * @return 变量值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String name, T defaultValue) {
        return (T) variables.getOrDefault(name, defaultValue);
    }

    /**
     * 检查是否包含变量
     *
     * @param name 变量名
     * @return true 包含
     */
    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    /**
     * 移除变量
     *
     * @param name 变量名
     */
    public void removeVariable(String name) {
        variables.remove(name);
    }

    /**
     * 清除所有变量
     */
    public void clearVariables() {
        variables.clear();
    }

    /**
     * 批量设置变量
     *
     * @param vars 变量映射
     */
    public void setVariables(Map<String, Object> vars) {
        if (vars != null) {
            variables.putAll(vars);
        }
    }

    /**
     * 创建内置变量
     *
     * @return ExecutionContext
     */
    public static ExecutionContext create() {
        return new ExecutionContext();
    }
}
