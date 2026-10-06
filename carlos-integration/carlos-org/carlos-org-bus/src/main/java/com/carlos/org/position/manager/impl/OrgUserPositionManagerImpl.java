package com.carlos.org.position.manager.impl;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.org.position.convert.OrgUserPositionConvert;
import com.carlos.org.position.manager.OrgUserPositionManager;
import com.carlos.org.position.mapper.OrgUserPositionMapper;
import com.carlos.org.position.pojo.dto.OrgUserPositionDTO;
import com.carlos.org.position.pojo.entity.OrgUserPosition;
import com.carlos.org.position.pojo.param.OrgUserPositionPageParam;
import com.carlos.org.position.pojo.vo.OrgUserPositionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 用户岗位职级关联表（核心任职信息） 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgUserPositionManagerImpl extends BaseServiceImpl<OrgUserPositionMapper, OrgUserPosition> implements OrgUserPositionManager {

    @Override
    public boolean add(OrgUserPositionDTO dto) {
        OrgUserPosition entity = OrgUserPositionConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgUserPosition' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'OrgUserPosition' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgUserPosition' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgUserPosition' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgUserPositionDTO dto) {
        OrgUserPosition entity = OrgUserPositionConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgUserPosition' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'OrgUserPosition' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgUserPositionDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgUserPosition entity = getBaseMapper().selectById(id);
        return OrgUserPositionConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<OrgUserPositionVO> getPage(OrgUserPositionPageParam param) {
        LambdaQueryWrapper<OrgUserPosition> wrapper = queryWrapper();
        wrapper.select(
            OrgUserPosition::getId,
            OrgUserPosition::getUserId,
            OrgUserPosition::getPositionId,
            OrgUserPosition::getLevelId,
            OrgUserPosition::getDepartmentId,
            OrgUserPosition::getMain,
            OrgUserPosition::getPositionStatus,
            OrgUserPosition::getAppointType,
            OrgUserPosition::getAppointDate,
            OrgUserPosition::getProbationEnd,
            OrgUserPosition::getDimissionDate,
            OrgUserPosition::getCostRatio,
            OrgUserPosition::getReportTo,
            OrgUserPosition::getDottedReport,
            OrgUserPosition::getWorkLocation,
            OrgUserPosition::getContractType,
            OrgUserPosition::getPerformanceRating,
            OrgUserPosition::getSalaryLevel,
            OrgUserPosition::getStockGrant,
            OrgUserPosition::getTenantId,
            OrgUserPosition::getCreateBy,
            OrgUserPosition::getCreateTime,
            OrgUserPosition::getUpdateBy,
            OrgUserPosition::getUpdateTime
        );
        PageInfo<OrgUserPosition> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, OrgUserPositionConvert.INSTANCE::toVO);
    }

    @Override
    public boolean removeByUserId(Long userId) {
        if (userId == null) {
            log.warn("userId can't be null");
            return false;
        }
        LambdaQueryWrapper<OrgUserPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgUserPosition::getUserId, userId);
        boolean success = remove(wrapper);
        if (!success) {
            log.warn("Remove 'OrgUserPosition' data by userId fail, userId:{}", userId);
            return false;
        }
        log.debug("Remove 'OrgUserPosition' data by userId:{}", userId);
        return true;
    }

    @Override
    public List<OrgUserPositionDTO> listByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        LambdaQueryWrapper<OrgUserPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgUserPosition::getUserId, userId);
        return OrgUserPositionConvert.INSTANCE.toDTO(list(wrapper));
    }

    @Override
    public List<OrgUserPositionDTO> listMainByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        LambdaQueryWrapper<OrgUserPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgUserPosition::getUserId, userId);
        wrapper.eq(OrgUserPosition::getMain, true);
        return OrgUserPositionConvert.INSTANCE.toDTO(list(wrapper));
    }

    @Override
    public OrgUserPositionDTO getByUserIdAndPositionId(Long userId, Long positionId) {
        if (userId == null || positionId == null) {
            return null;
        }
        LambdaQueryWrapper<OrgUserPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgUserPosition::getUserId, userId);
        wrapper.eq(OrgUserPosition::getPositionId, positionId);
        wrapper.last("LIMIT 1");
        return OrgUserPositionConvert.INSTANCE.toDTO(getBaseMapper().selectOne(wrapper));
    }

    @Override
    public OrgUserPositionDTO getByUserIdAndPositionIdAndDeptId(Long userId, Long positionId, Long departmentId) {
        if (userId == null || positionId == null || departmentId == null) {
            return null;
        }
        LambdaQueryWrapper<OrgUserPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgUserPosition::getUserId, userId)
            .eq(OrgUserPosition::getPositionId, positionId)
            .eq(OrgUserPosition::getDepartmentId, departmentId)
            .last("LIMIT 1");
        return OrgUserPositionConvert.INSTANCE.toDTO(getBaseMapper().selectOne(wrapper));
    }

    @Override
    public boolean clearMainPosition(Long userId) {
        if (userId == null) {
            return false;
        }
        LambdaUpdateWrapper<OrgUserPosition> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrgUserPosition::getUserId, userId)
            .eq(OrgUserPosition::getMain, true)
            .set(OrgUserPosition::getMain, false);
        return update(wrapper);
    }

    @Override
    public boolean updatePositionStatus(Long userId, Long positionId, Integer status, LocalDate dimissionDate) {
        if (userId == null || positionId == null || status == null) {
            return false;
        }
        LambdaUpdateWrapper<OrgUserPosition> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrgUserPosition::getUserId, userId)
            .eq(OrgUserPosition::getPositionId, positionId)
            .set(OrgUserPosition::getPositionStatus, status);
        if (dimissionDate != null) {
            wrapper.set(OrgUserPosition::getDimissionDate, dimissionDate);
        }
        return update(wrapper);
    }

    @Override
    public int countOccupiedByPositionId(Serializable positionId) {
        if (positionId == null) {
            return 0;
        }
        LambdaQueryWrapper<OrgUserPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgUserPosition::getPositionId, positionId)
            .eq(OrgUserPosition::getPositionStatus, 1);
        return (int) count(wrapper);
    }

    @Override
    public List<OrgUserPositionDTO> listByReportTo(Serializable reportTo) {
        Long reportToValue = Convert.toLong(reportTo);
        if (reportToValue == null) {
            return List.of();
        }
        LambdaQueryWrapper<OrgUserPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgUserPosition::getReportTo, reportToValue);
        return OrgUserPositionConvert.INSTANCE.toDTO(list(wrapper));
    }

    @Override
    public List<OrgUserPositionDTO> listByReportToAndTenant(Serializable reportTo, Long tenantId) {
        Long reportToValue = Convert.toLong(reportTo);
        if (reportToValue == null || tenantId == null) {
            return List.of();
        }
        // 绕过租户拦截器后显式按 tenant_id 过滤，避免无请求上下文时回落到平台租户
        IgnoreStrategy ignoreStrategy = IgnoreStrategy.builder().tenantLine(true).build();
        List<OrgUserPosition> entities = InterceptorIgnoreHelper.execute(ignoreStrategy, () -> list(
            new LambdaQueryWrapper<OrgUserPosition>()
                .eq(OrgUserPosition::getReportTo, reportToValue)
                .eq(OrgUserPosition::getTenantId, tenantId)));
        return OrgUserPositionConvert.INSTANCE.toDTO(entities);
    }

    @Override
    public Long getReportToByUserId(Serializable userId) {
        Long userIdValue = Convert.toLong(userId);
        if (userIdValue == null) {
            return null;
        }
        // 用户可能兼岗：优先主岗位配置的汇报对象，其次任意一条已配置的记录
        List<OrgUserPosition> records = list(new LambdaQueryWrapper<OrgUserPosition>()
            .select(OrgUserPosition::getUserId, OrgUserPosition::getMain, OrgUserPosition::getReportTo)
            .eq(OrgUserPosition::getUserId, userIdValue)
            .isNotNull(OrgUserPosition::getReportTo));
        return pickReportTo(records);
    }

    @Override
    public Long getReportToByUserIdAndTenant(Serializable userId, Long tenantId) {
        Long userIdValue = Convert.toLong(userId);
        if (userIdValue == null || tenantId == null) {
            return null;
        }
        // 绕过租户拦截器后显式按 tenant_id 过滤，避免无请求上下文时回落到平台租户
        IgnoreStrategy ignoreStrategy = IgnoreStrategy.builder().tenantLine(true).build();
        List<OrgUserPosition> records = InterceptorIgnoreHelper.execute(ignoreStrategy, () -> list(
            new LambdaQueryWrapper<OrgUserPosition>()
                .select(OrgUserPosition::getUserId, OrgUserPosition::getMain, OrgUserPosition::getReportTo)
                .eq(OrgUserPosition::getUserId, userIdValue)
                .isNotNull(OrgUserPosition::getReportTo)
                .eq(OrgUserPosition::getTenantId, tenantId)));
        return pickReportTo(records);
    }

    @Override
    public List<Long> listUserIdsByPositionId(Serializable positionId) {
        Long positionIdValue = Convert.toLong(positionId);
        if (positionIdValue == null) {
            return List.of();
        }
        // 刻意不过滤 position_status，口径与 listByReportTo 一致（见接口 javadoc）
        List<OrgUserPosition> records = list(new LambdaQueryWrapper<OrgUserPosition>()
            .select(OrgUserPosition::getUserId)
            .eq(OrgUserPosition::getPositionId, positionIdValue));
        return distinctUserIds(records);
    }

    @Override
    public List<Long> listUserIdsByPositionIdAndTenant(Serializable positionId, Long tenantId) {
        Long positionIdValue = Convert.toLong(positionId);
        if (positionIdValue == null || tenantId == null) {
            return List.of();
        }
        // 绕过租户拦截器后显式按 tenant_id 过滤，避免无请求上下文时回落到平台租户
        IgnoreStrategy ignoreStrategy = IgnoreStrategy.builder().tenantLine(true).build();
        List<OrgUserPosition> records = InterceptorIgnoreHelper.execute(ignoreStrategy, () -> list(
            new LambdaQueryWrapper<OrgUserPosition>()
                .select(OrgUserPosition::getUserId)
                .eq(OrgUserPosition::getPositionId, positionIdValue)
                .eq(OrgUserPosition::getTenantId, tenantId)));
        return distinctUserIds(records);
    }

    /**
     * 从用户的任职记录中挑出汇报对象：优先主岗位，其次任意一条已配置的记录
     *
     * @param records 已过滤 {@code report_to} 非空的任职记录
     * @return 汇报对象用户ID；无记录时返回 null
     */
    private Long pickReportTo(List<OrgUserPosition> records) {
        Long mainReportTo = records.stream()
            .filter(record -> Boolean.TRUE.equals(record.getMain()))
            .map(OrgUserPosition::getReportTo)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        if (mainReportTo != null) {
            return mainReportTo;
        }
        return records.stream()
            .map(OrgUserPosition::getReportTo)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * 任职记录去重后的 {@code user_id} 列表
     *
     * @param records 任职记录
     * @return 用户ID列表
     */
    private List<Long> distinctUserIds(List<OrgUserPosition> records) {
        return records.stream()
            .map(OrgUserPosition::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

}
