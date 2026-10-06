package com.carlos.boot.translation.core;

import com.carlos.core.base.DepartmentInfo;
import com.carlos.core.base.Dict;
import com.carlos.core.base.RegionInfo;
import com.carlos.core.base.UserInfo;
import com.carlos.core.enums.EnumInfo;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * <p>
 * 翻译结果数据容器
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
public class TranslationData {

    /**
     * 用户数据 (userId -> UserInfo)
     */
    private Map<Serializable, UserInfo> users = Collections.emptyMap();

    /**
     * 字典数据 (type:code -> Dict)
     */
    private Map<String, Dict> dicts = Collections.emptyMap();

    /**
     * 部门数据 (deptId -> DepartmentInfo)
     */
    private Map<Serializable, DepartmentInfo> depts = Collections.emptyMap();

    /**
     * 区域数据 (regionCode -> RegionInfo)
     */
    private Map<String, RegionInfo> regions = Collections.emptyMap();

    /**
     * 枚举数据 (enumClass:code -> Enum)
     */
    private Map<String, EnumInfo> enums = Collections.emptyMap();

    public UserInfo getUser(Serializable id) {
        return id != null ? users.get(id) : null;
    }

    public Dict getDict(String type, String code) {
        return type != null && code != null ? dicts.get(type + ":" + code) : null;
    }

    public DepartmentInfo getDept(Serializable id) {
        return id != null ? depts.get(id) : null;
    }

    public RegionInfo getRegion(String code) {
        return code != null ? regions.get(code) : null;
    }

    @SuppressWarnings("unchecked")
    public <E extends EnumInfo> EnumInfo getEnum(Class<?> enumClass, Object code) {
        if (enumClass == null || code == null) {
            return null;
        }
        return enums.get(enumClass.getName() + ":" + code);
    }

    public void setUsers(Map<Serializable, UserInfo> users) {
        this.users = users != null ? users : Collections.emptyMap();
    }

    public void setDicts(Map<String, Dict> dicts) {
        this.dicts = dicts != null ? dicts : Collections.emptyMap();
    }

    public void setDepts(Map<Serializable, DepartmentInfo> depts) {
        this.depts = depts != null ? depts : Collections.emptyMap();
    }

    public void setRegions(Map<String, RegionInfo> regions) {
        this.regions = regions != null ? regions : Collections.emptyMap();
    }

    public void setEnums(Map<String, EnumInfo> enums) {
        this.enums = enums != null ? enums : Collections.emptyMap();
    }

    public Map<Serializable, UserInfo> getUsers() {
        return Map.copyOf(users);
    }

    public Map<String, Dict> getDicts() {
        return Map.copyOf(dicts);
    }

    public Map<Serializable, DepartmentInfo> getDepts() {
        return Map.copyOf(depts);
    }

    public Map<String, RegionInfo> getRegions() {
        return Map.copyOf(regions);
    }

    public Map<String, EnumInfo> getEnums() {
        return Map.copyOf(enums);
    }
}
