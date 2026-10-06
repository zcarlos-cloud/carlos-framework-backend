package com.carlos.core.interfaces;

import com.carlos.core.base.DepartmentInfo;
import com.carlos.core.base.Dict;
import com.carlos.core.base.RegionInfo;
import com.carlos.core.base.UserInfo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * ApplicationExtend 批量查询扩展方法
 * </p>
 *
 * <p>建议在 ApplicationExtend 接口中添加以下默认方法</p>
 *
 * @author carlos
 * @date 2025/01/20
 */
public interface ApplicationExtendBatch {

    /**
     * 批量查询用户信息
     *
     * @param ids 用户ID集合
     * @return 用户数据 (userId -> UserInfo)
     */
    default Map<Serializable, UserInfo> getUserByIds(Set<Serializable> ids) {
        Map<Serializable, UserInfo> result = new HashMap<>();
        for (Serializable id : ids) {
            UserInfo user = getUserById(id);
            if (user != null) {
                result.put(id, user);
            }
        }
        return result;
    }

    /**
     * 批量查询字典信息
     *
     * @param type  字典类型
     * @param codes 字典编码集合
     * @return 字典数据 (code -> Dict)
     */
    default Map<String, Dict> getDictVos(String type, Set<String> codes) {
        Map<String, Dict> result = new HashMap<>();
        for (String code : codes) {
            Dict dict = getDictVo(code);
            if (dict != null) {
                result.put(code, dict);
            }
        }
        return result;
    }

    /**
     * 批量查询部门信息
     *
     * @param ids 部门ID集合
     * @return 部门数据 (deptId -> DepartmentInfo)
     */
    default Map<Serializable, DepartmentInfo> getDepartmentByIds(Set<Serializable> ids) {
        Map<Serializable, DepartmentInfo> result = new HashMap<>();
        for (Serializable id : ids) {
            DepartmentInfo dept = getDepartmentById(id, Integer.MAX_VALUE);
            if (dept != null) {
                result.put(id, dept);
            }
        }
        return result;
    }

    /**
     * 批量查询区域信息
     *
     * @param codes 区域编码集合
     * @return 区域数据 (regionCode -> RegionInfo)
     */
    default Map<String, RegionInfo> getRegionByCodes(Set<String> codes) {
        Map<String, RegionInfo> result = new HashMap<>();
        for (String code : codes) {
            RegionInfo region = getRegionInfo(code, Integer.MAX_VALUE);
            if (region != null) {
                result.put(code, region);
            }
        }
        return result;
    }

    // ========== 需要 ApplicationExtend 原有方法 ==========

    /**
     * 根据用户id获取用户信息（ApplicationExtend 原有方法）
     */
    UserInfo getUserById(Serializable userId);

    /**
     * 根据字典code获取字典详细信息（ApplicationExtend 原有方法）
     */
    Dict getDictVo(String code);

    /**
     * 根据部门id获取部门信息（ApplicationExtend 原有方法）
     */
    DepartmentInfo getDepartmentById(Serializable departmentId, Integer limit);

    /**
     * 获取区域信息（ApplicationExtend 原有方法）
     */
    RegionInfo getRegionInfo(String regionCode, Integer limit);
}
