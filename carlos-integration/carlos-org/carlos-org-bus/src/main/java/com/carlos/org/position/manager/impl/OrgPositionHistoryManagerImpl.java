package com.carlos.org.position.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.org.position.convert.OrgPositionHistoryConvert;
import com.carlos.org.position.manager.OrgPositionHistoryManager;
import com.carlos.org.position.mapper.OrgPositionHistoryMapper;
import com.carlos.org.position.pojo.dto.OrgPositionHistoryDTO;
import com.carlos.org.position.pojo.entity.OrgPositionHistory;
import com.carlos.org.position.pojo.param.OrgPositionHistoryPageParam;
import com.carlos.org.position.pojo.vo.OrgPositionHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * <p>
 * 岗位变更历史表 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgPositionHistoryManagerImpl extends BaseServiceImpl<OrgPositionHistoryMapper, OrgPositionHistory> implements OrgPositionHistoryManager {

    @Override
    public boolean add(OrgPositionHistoryDTO dto) {
        OrgPositionHistory entity = OrgPositionHistoryConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgPositionHistory' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'OrgPositionHistory' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgPositionHistory' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgPositionHistory' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgPositionHistoryDTO dto) {
        OrgPositionHistory entity = OrgPositionHistoryConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgPositionHistory' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'OrgPositionHistory' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgPositionHistoryDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgPositionHistory entity = getBaseMapper().selectById(id);
        return OrgPositionHistoryConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<OrgPositionHistoryVO> getPage(OrgPositionHistoryPageParam param) {
        LambdaQueryWrapper<OrgPositionHistory> wrapper = queryWrapper();
        wrapper.select(

            OrgPositionHistory::getId,
            OrgPositionHistory::getUserId,
            OrgPositionHistory::getChangeType,
            OrgPositionHistory::getOldPositionId,
            OrgPositionHistory::getNewPositionId,
            OrgPositionHistory::getOldLevelId,
            OrgPositionHistory::getNewLevelId,
            OrgPositionHistory::getOldDeptId,
            OrgPositionHistory::getNewDeptId,
            OrgPositionHistory::getOldSalary,
            OrgPositionHistory::getNewSalary,
            OrgPositionHistory::getChangeReason,
            OrgPositionHistory::getChangeDate,
            OrgPositionHistory::getApprovalNo,
            OrgPositionHistory::getAttachments,
            OrgPositionHistory::getRemark,
            OrgPositionHistory::getOperatorId,
            OrgPositionHistory::getOperatorName,
            OrgPositionHistory::getTenantId,
            OrgPositionHistory::getCreateTime
        );
        PageInfo<OrgPositionHistory> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, OrgPositionHistoryConvert.INSTANCE::toVO);
    }

}
