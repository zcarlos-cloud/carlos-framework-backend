package com.carlos.core.interfaces;

import com.carlos.core.auth.UserContext;
import com.carlos.core.base.DepartmentInfo;
import com.carlos.core.base.Dict;
import com.carlos.core.base.RegionInfo;
import com.carlos.core.base.UserInfo;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 系统扩展信息
 * </p>
 *
 * @author carlos
 * @date 2020/6/3 11:09
 */
public interface ApplicationExtend extends ApplicationExtendBatch {

    /**
     * 获取请求的token信息
     *
     * @return 当前访问者的token信息
     * @author carlos
     * @date 2020/6/3 11:15
     */
    default String getToken() {
        return null;
    }

    /**
     * 获取当前访问用户的Id
     *
     * @return 当前访问用户的id
     * @author carlos
     * @date 2020/6/3 11:16
     */
    default Serializable getUserId() {
        return null;
    }

    /**
     * 获取当前访问用户的用户名
     *
     * @return 当前用户的用户名
     * @author carlos
     * @date 2020/6/3 11:16
     */
    default String getUserName() {
        return null;
    }

    /**
     * 根据字典code获取字典详细信息
     *
     * @param code 字典选项code
     * @return com.carlos.core.base.DictVo
     * @author carlos
     * @date 2021/11/25 14:10
     */
    @Override
    default Dict getDictVo(String code) {
        return null;
    }

    /**
     * 根据字典id获取字典详细信息
     *
     * @param id 字典选项id
     * @return com.carlos.core.base.DictVo
     * @author carlos
     * @date 2021/11/25 14:10
     */
    default Dict getDictById(Serializable id) {
        return null;
    }

    /**
     * 根据用户id获取用户信息
     *
     * @param userId 用户id
     * @return java.lang.String
     * @author carlos
     * @date 2021/11/25 15:01
     */
    @Override
    default UserInfo getUserById(Serializable userId) {
        return null;
    }

    /**
     * 获取用户上下文信息
     *
     * @return com.carlos.core.auth.UserContext
     * @author Carlos
     * @date 2022/11/14 19:01
     */
    default UserContext getUserContext() {
        return null;
    }


    /**
     * 根据部门id获取部门信息
     *
     * @param departmentId 部门id
     * @return java.lang.String
     * @author carlos
     * @date 2022/12/30 11:01
     */
    @Override
    default DepartmentInfo getDepartmentById(Serializable departmentId, Integer limit) {
        return null;
    }

    /**
     * 获取区域信息
     *
     * @param regionCode 区域编码
     * @param limit      上级数目
     * @return com.carlos.core.base.RegionInfo
     * @author Carlos
     * @date 2022/12/30 13:54
     */
    @Override
    default RegionInfo getRegionInfo(String regionCode, Integer limit) {
        return null;
    }


    /**
     * 根据部门code获取部门信息
     *
     * @param code  部门code
     * @param limit 预览级数
     * @return com.carlos.core.base.DepartmentInfo
     * @author Carlos
     * @date 2023/8/9 16:49
     */
    default DepartmentInfo getDepartmentByCode(String code, int limit) {
        return null;
    }


    /**
     * 根据用户id批量获取用户信息
     *
     * @param ids 用户id
     * @return java.util.Map<java.io.Serializable, com.carlos.core.base.UserInfo>
     * @author carlos
     * @date 2023/8/9 16:49
     */
    @Override
    default Map<Serializable, UserInfo> getUserByIds(Set<Serializable> ids) {
        return null;
    }

    /**
     * 根据字典code批量获取字典信息
     *
     * @param type   字典类型
     * @param value  字典code
     * @return java.util.Map<java.lang.String, com.carlos.core.base.Dict>
     * @author carlos
     * @date 2023/8/9 16:49
     */
    @Override
    default Map<String, Dict> getDictVos(String type, Set<String> value) {
        return null;
    }

    /**
     * 根据部门id批量获取部门信息
     *
     * @param missIds 部门id
     * @return java.util.Map<java.io.Serializable, com.carlos.core.base.DepartmentInfo>
     * @author carlos
     * @date 2023/8/9 16:49
     */
    @Override
    default Map<Serializable, DepartmentInfo> getDepartmentByIds(Set<Serializable> missIds) {
        return null;
    }

    /**
     * 根据区域code批量获取区域信息
     *
     * @param missCodes 区域code
     * @return java.util.Map<java.lang.String, com.carlos.core.base.RegionInfo>
     * @author carlos
     * @date 2023/8/9 16:49
     */
    @Override
    default Map<String, RegionInfo> getRegionByCodes(Set<String> missCodes) {
        return null;
    }
}
