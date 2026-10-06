package com.carlos.monitor.instance.manager.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.monitor.instance.convert.MonitorGroupConvert;
import com.carlos.monitor.instance.manager.MonitorGroupAuthManager;
import com.carlos.monitor.instance.manager.MonitorInstanceGroupManager;
import com.carlos.monitor.instance.mapper.MonitorInstanceGroupMapper;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceGroupDTO;
import com.carlos.monitor.instance.pojo.entity.MonitorInstanceGroup;
import com.carlos.monitor.instance.pojo.param.GroupPageParam;
import com.carlos.monitor.instance.pojo.vo.InstanceGroupVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 监控实例分组 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorInstanceGroupManagerImpl extends BaseServiceImpl<MonitorInstanceGroupMapper, MonitorInstanceGroup>
    implements MonitorInstanceGroupManager {

    private final MonitorGroupAuthManager groupAuthManager;

    @Override
    public boolean add(MonitorInstanceGroupDTO dto) {
        MonitorInstanceGroup entity = MonitorGroupConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'MonitorInstanceGroup' data fail, groupName:{}", dto.getGroupName());
            return false;
        }
        dto.setId(entity.getId());
        log.debug("Insert 'MonitorInstanceGroup' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean modify(MonitorInstanceGroupDTO dto) {
        MonitorInstanceGroup entity = MonitorGroupConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'MonitorInstanceGroup' data fail, id:{}", dto.getId());
            return false;
        }
        log.debug("Update 'MonitorInstanceGroup' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        boolean success = removeById(id);
        if (!success) {
            log.warn("Remove 'MonitorInstanceGroup' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'MonitorInstanceGroup' data by id:{}", id);
        return true;
    }

    @Override
    public MonitorInstanceGroupDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        MonitorInstanceGroup entity = getBaseMapper().selectById(id);
        return MonitorGroupConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<InstanceGroupVO> getPage(GroupPageParam param) {
        LambdaQueryWrapper<MonitorInstanceGroup> wrapper = queryWrapper();
        wrapper.like(StrUtil.isNotBlank(param.getGroupName()), MonitorInstanceGroup::getGroupName, param.getGroupName());
        wrapper.orderByAsc(MonitorInstanceGroup::getOrderNum).orderByDesc(MonitorInstanceGroup::getCreateTime);

        PageInfo<MonitorInstanceGroup> page = page(pageInfo(param), wrapper);
        Paging<InstanceGroupVO> paging = MybatisPage.map(page,
            entity -> MonitorGroupConvert.INSTANCE.toVO(MonitorGroupConvert.INSTANCE.toDTO(entity)));
        // 回填授权角色ID，供列表/编辑回显（批量查询避免 N+1）
        List<Long> groupIds = paging.getRecords().stream().map(InstanceGroupVO::getId).toList();
        Map<Long, List<Long>> roleIdMap = groupAuthManager.listRoleIdMap(groupIds);
        paging.getRecords().forEach(vo -> vo.setRoleIds(roleIdMap.getOrDefault(vo.getId(), List.of())));
        return paging;
    }

    @Override
    public List<MonitorInstanceGroupDTO> listAll() {
        LambdaQueryWrapper<MonitorInstanceGroup> wrapper = queryWrapper();
        wrapper.orderByAsc(MonitorInstanceGroup::getOrderNum).orderByDesc(MonitorInstanceGroup::getCreateTime);
        List<MonitorInstanceGroup> entities = getBaseMapper().selectList(wrapper);
        return MonitorGroupConvert.INSTANCE.toDTO(entities);
    }

    @Override
    public long countByName(String groupName, Serializable excludeId) {
        LambdaQueryWrapper<MonitorInstanceGroup> wrapper = queryWrapper();
        wrapper.eq(MonitorInstanceGroup::getGroupName, groupName);
        wrapper.ne(ObjUtil.isNotEmpty(excludeId), MonitorInstanceGroup::getId, excludeId);
        return count(wrapper);
    }
}
