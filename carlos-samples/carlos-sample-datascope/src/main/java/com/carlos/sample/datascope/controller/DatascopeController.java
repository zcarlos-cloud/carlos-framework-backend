package com.carlos.sample.datascope.controller;

import com.carlos.datascope.DataScope;
import com.carlos.datascope.DataScopeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据权限演示 Controller
 *
 * <p>演示 @DataScope 注解的各种使用方式</p>
 *
 * @author Carlos
 * @date 2026/3/15
 */
@RestController
@RequestMapping("/datascope")
public class DatascopeController {

    /**
     * 全部数据权限演示
     */
    @GetMapping("/all")
    @DataScope(type = DataScopeType.ALL, caller = DatascopeController.class)
    public Map<String, Object> all() {
        Map<String, Object> result = new HashMap<>();
        result.put("scope", "ALL");
        result.put("description", "全部数据权限 - 可查看所有数据");
        return result;
    }

    /**
     * 本部门数据权限演示
     */
    @GetMapping("/current-dept")
    @DataScope(type = DataScopeType.CURRENT_DEPT, caller = DatascopeController.class)
    public Map<String, Object> currentDept() {
        Map<String, Object> result = new HashMap<>();
        result.put("scope", "CURRENT_DEPT");
        result.put("description", "本部门数据权限 - 仅查看本部门数据");
        return result;
    }

    /**
     * 本部门及以下数据权限演示
     */
    @GetMapping("/dept-and-sub")
    @DataScope(type = DataScopeType.DEPT_AND_SUB, caller = DatascopeController.class)
    public Map<String, Object> deptAndSub() {
        Map<String, Object> result = new HashMap<>();
        result.put("scope", "DEPT_AND_SUB");
        result.put("description", "本部门及以下数据权限 - 查看本部门及子部门数据");
        return result;
    }

    /**
     * 本岗位数据权限演示
     */
    @GetMapping("/current-role")
    @DataScope(type = DataScopeType.CURRENT_ROLE, caller = DatascopeController.class)
    public Map<String, Object> currentRole() {
        Map<String, Object> result = new HashMap<>();
        result.put("scope", "CURRENT_ROLE");
        result.put("description", "本岗位数据权限 - 仅查看本岗位相关数据");
        return result;
    }

    /**
     * 仅本人数据权限演示
     */
    @GetMapping("/current-user")
    @DataScope(type = DataScopeType.CURRENT_USER, caller = DatascopeController.class)
    public Map<String, Object> currentUser() {
        Map<String, Object> result = new HashMap<>();
        result.put("scope", "CURRENT_USER");
        result.put("description", "仅本人数据权限 - 仅查看自己创建的数据");
        return result;
    }

    /**
     * 自定义字段数据权限演示
     */
    @GetMapping("/custom-field")
    @DataScope(type = DataScopeType.CURRENT_USER, field = "user_id", caller = DatascopeController.class)
    public Map<String, Object> customField() {
        Map<String, Object> result = new HashMap<>();
        result.put("scope", "CURRENT_USER");
        result.put("field", "user_id");
        result.put("description", "自定义字段数据权限 - 使用 user_id 字段过滤");
        return result;
    }

    /**
     * 无数据权限演示
     */
    @GetMapping("/none")
    @DataScope(type = DataScopeType.NONE, caller = DatascopeController.class)
    public Map<String, Object> none() {
        Map<String, Object> result = new HashMap<>();
        result.put("scope", "NONE");
        result.put("description", "无数据权限 - 无法查看任何数据");
        return result;
    }

    /**
     * 未加数据权限注解（不受数据权限控制）
     */
    @GetMapping("/no-scope")
    public Map<String, Object> noScope() {
        Map<String, Object> result = new HashMap<>();
        result.put("scope", "NONE");
        result.put("description", "无数据权限控制 - 方法未添加 @DataScope 注解");
        return result;
    }
}
