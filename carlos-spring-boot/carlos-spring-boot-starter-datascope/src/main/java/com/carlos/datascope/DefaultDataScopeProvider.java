package com.carlos.datascope;

import com.carlos.core.interfaces.ApplicationExtend;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Set;

/**
 * 权限数据处理器
 *
 * @author Carlos
 * @date 2022/11/22 13:32
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultDataScopeProvider implements DataScopeProvider {

    private final ApplicationExtend applicationExtend;

    @Override
    public Serializable currentUserId() {
        return this.applicationExtend.getUserId();
    }

    @Override
    public Set<Serializable> currentRoleUserIds() {
        return null;
    }

    @Override
    public Set<Serializable> currentDeptUserIds() {
        return null;
    }

    @Override
    public Set<Serializable> currentDeptAllUserIds(final Serializable departmentId) {
        return null;
    }

    @Override
    public Set<Serializable> currentDeptAllIds(final Serializable departmentId) {
        return null;
    }

    @Override
    public Set<String> currentRegionAllCodes() {
        return null;
    }
}
