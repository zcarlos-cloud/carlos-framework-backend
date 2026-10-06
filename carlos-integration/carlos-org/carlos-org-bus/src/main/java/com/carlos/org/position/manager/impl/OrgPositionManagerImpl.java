package com.carlos.org.position.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.org.position.convert.OrgPositionConvert;
import com.carlos.org.position.manager.OrgPositionManager;
import com.carlos.org.position.mapper.OrgPositionMapper;
import com.carlos.org.position.pojo.dto.OrgPositionDTO;
import com.carlos.org.position.pojo.entity.OrgPosition;
import com.carlos.org.position.pojo.param.OrgPositionPageParam;
import com.carlos.org.position.pojo.vo.OrgPositionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * <p>
 * 岗位表 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgPositionManagerImpl extends BaseServiceImpl<OrgPositionMapper, OrgPosition> implements OrgPositionManager {

    @Override
    public boolean add(OrgPositionDTO dto) {
        OrgPosition entity = OrgPositionConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgPosition' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'OrgPosition' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgPosition' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgPosition' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgPositionDTO dto) {
        OrgPosition entity = OrgPositionConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgPosition' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'OrgPosition' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgPositionDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgPosition entity = getBaseMapper().selectById(id);
        return OrgPositionConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<OrgPositionVO> getPage(OrgPositionPageParam param) {
        PageInfo<OrgPosition> page = page(pageInfo(param), buildQueryWrapper(param));
        return MybatisPage.convert(page, OrgPositionConvert.INSTANCE::toVO);
    }

    /**
     * 构造岗位列表查询条件
     *
     * <p>
     * ⚠️ 此前 {@code getPage} **完全没有应用任何查询条件** —— 搜索栏的
     * 岗位编码 / 岗位名称 / 岗位类别 / 岗位性质 / 岗位状态五个筛选全部不生效。
     * 实测：{@code GET /org/position/page?positionName=zzz-nomatch} 仍返回全部岗位。
     * </p>
     *
     * <p>
     * 只覆盖「可作为查询条件」的字段：文本类用 like，外键/枚举码用 eq，时间用区间。
     * 刻意**不**参与筛选的有：{@code duty} / {@code requirement} / {@code qualification} /
     * {@code kpiIndicator}（富文本或 JSON，不是检索维度），以及 {@code headcount} /
     * {@code currentCount} / {@code vacancyCount}（编制与在岗人数是冗余计算值）。
     * </p>
     *
     * @param param 查询条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<OrgPosition> buildQueryWrapper(OrgPositionPageParam param) {
        LambdaQueryWrapper<OrgPosition> wrapper = queryWrapper();
        wrapper.select(

            OrgPosition::getId,
            OrgPosition::getPositionCode,
            OrgPosition::getPositionName,
            OrgPosition::getPositionShort,
            OrgPosition::getCategoryId,
            OrgPosition::getDefaultLevelId,
            OrgPosition::getLevelRange,
            OrgPosition::getPositionType,
            OrgPosition::getPositionNature,
            OrgPosition::getDepartmentId,
            OrgPosition::getParentId,
            OrgPosition::getHeadcount,
            OrgPosition::getCurrentCount,
            OrgPosition::getVacancyCount,
            OrgPosition::getDuty,
            OrgPosition::getRequirement,
            OrgPosition::getQualification,
            OrgPosition::getKpiIndicator,
            OrgPosition::getCostCenter,
            OrgPosition::getLocation,
            OrgPosition::getSalaryGrade,
            OrgPosition::getPositionStatus,
            OrgPosition::getTenantId,
            OrgPosition::getCreateBy,
            OrgPosition::getCreateTime,
            OrgPosition::getUpdateBy,
            OrgPosition::getUpdateTime
        );

        wrapper.like(StrUtil.isNotBlank(param.getPositionCode()), OrgPosition::getPositionCode, param.getPositionCode())
            .like(StrUtil.isNotBlank(param.getPositionName()), OrgPosition::getPositionName, param.getPositionName())
            .like(StrUtil.isNotBlank(param.getPositionShort()), OrgPosition::getPositionShort, param.getPositionShort())
            .like(StrUtil.isNotBlank(param.getLevelRange()), OrgPosition::getLevelRange, param.getLevelRange())
            .like(StrUtil.isNotBlank(param.getCostCenter()), OrgPosition::getCostCenter, param.getCostCenter())
            .like(StrUtil.isNotBlank(param.getLocation()), OrgPosition::getLocation, param.getLocation())
            .like(StrUtil.isNotBlank(param.getSalaryGrade()), OrgPosition::getSalaryGrade, param.getSalaryGrade())
            .eq(param.getCategoryId() != null, OrgPosition::getCategoryId, param.getCategoryId())
            .eq(param.getDefaultLevelId() != null, OrgPosition::getDefaultLevelId, param.getDefaultLevelId())
            .eq(param.getPositionType() != null, OrgPosition::getPositionType, param.getPositionType())
            .eq(param.getPositionNature() != null, OrgPosition::getPositionNature, param.getPositionNature())
            .eq(param.getDepartmentId() != null, OrgPosition::getDepartmentId, param.getDepartmentId())
            .eq(param.getParentId() != null, OrgPosition::getParentId, param.getParentId())
            // 状态 0 是合法值，故判 null 而不是判空
            .eq(param.getPositionStatus() != null, OrgPosition::getPositionStatus, param.getPositionStatus())
            .eq(param.getTenantId() != null, OrgPosition::getTenantId, param.getTenantId())
            .between(param.getStart() != null && param.getEnd() != null, OrgPosition::getCreateTime, param.getStart(), param.getEnd());

        // 分页必须有稳定排序：PostgreSQL 下没有 ORDER BY 的翻页结果可能重复或漏行
        wrapper.orderByDesc(OrgPosition::getCreateTime).orderByDesc(OrgPosition::getId);

        return wrapper;
    }

}
