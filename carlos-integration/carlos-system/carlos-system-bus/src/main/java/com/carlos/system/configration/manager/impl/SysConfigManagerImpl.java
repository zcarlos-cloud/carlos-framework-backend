package com.carlos.system.configration.manager.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.system.configration.convert.SysConfigConvert;
import com.carlos.system.configration.manager.SysConfigManager;
import com.carlos.system.configration.mapper.SysConfigMapper;
import com.carlos.system.configration.pojo.dto.SysConfigDTO;
import com.carlos.system.configration.pojo.entity.SysConfig;
import com.carlos.system.configration.pojo.param.SysConfigPageParam;
import com.carlos.system.configration.pojo.vo.SysConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 系统配置 查询封装实现类
 * </p>
 *
 * @author carlos
 * @date 2022-11-3 13:47:54
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysConfigManagerImpl extends BaseServiceImpl<SysConfigMapper, SysConfig> implements SysConfigManager {


    @Override
    public boolean add(SysConfigDTO dto) {
        SysConfig entity = SysConfigConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'SysConfig' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'SysConfig' data: id:{}", entity.getId());
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
            log.warn("Remove 'SysConfig' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'SysConfig' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(SysConfigDTO dto) {
        SysConfig entity = SysConfigConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'SysConfig' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'SysConfig' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public SysConfigDTO getDtoById(Long id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        SysConfig entity = getBaseMapper().selectById(id);
        return SysConfigConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<SysConfigVO> getPage(SysConfigPageParam param) {
        PageInfo<SysConfig> page = page(pageInfo(param), buildQueryWrapper(param));
        return MybatisPage.convert(page, SysConfigConvert.INSTANCE::toVO);
    }

    @Override
    public List<SysConfigDTO> listByParam(SysConfigPageParam param) {
        return SysConfigConvert.INSTANCE.toDTO(list(buildQueryWrapper(param)));
    }

    /**
     * 构造列表查询条件
     *
     * <p>
     * 抽成独立方法是为了让「分页查询」与「导出」共用**同一份**过滤逻辑。
     * 此前导出走的是 {@code configManager.list()}（整表），于是「先筛选再导出」
     * 永远导出全表 —— 而且就算当时补上条件，两份写法迟早会写歪。
     * </p>
     *
     * @param param 查询条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<SysConfig> buildQueryWrapper(SysConfigPageParam param) {
        LambdaQueryWrapper<SysConfig> wrapper = queryWrapper();
        wrapper.select(
            SysConfig::getId,
            SysConfig::getConfigName,
            SysConfig::getConfigCode,
            SysConfig::getConfigValue,
            SysConfig::getValueType,
            SysConfig::getConfigStatus,
            SysConfig::getRemark,
            SysConfig::getCreateBy,
            SysConfig::getCreateTime,
            SysConfig::getUpdateBy,
            SysConfig::getUpdateTime
        );
        wrapper.orderByDesc(SysConfig::getCreateTime);

        // 参数名称：名称或编码任一模糊匹配即可。用 and(...) 包裹 OR 组，
        // 否则与后续条件组合时会变成 a OR b AND c 的优先级错误
        if (StrUtil.isNotBlank(param.getConfigName())) {
            String keyword = param.getConfigName();
            wrapper.and(w -> w.like(SysConfig::getConfigName, keyword).or().like(SysConfig::getConfigCode, keyword));
        }
        wrapper.like(StrUtil.isNotBlank(param.getConfigCode()), SysConfig::getConfigCode, param.getConfigCode());
        wrapper.eq(null != param.getValueType(), SysConfig::getValueType, param.getValueType());
        wrapper.eq(null != param.getConfigStatus(), SysConfig::getConfigStatus, param.getConfigStatus());

        return wrapper;
    }


    @Override
    public List<SysConfigDTO> getAllConfig(Set<String> excludes) {
        List<SysConfig> list = lambdaQuery()
            .select(
                SysConfig::getConfigName,
                SysConfig::getConfigCode,
                SysConfig::getConfigValue
            ).eq(SysConfig::getConfigStatus, true)
            .notIn(CollUtil.isNotEmpty(excludes), SysConfig::getConfigCode, excludes)
            .list();
        return SysConfigConvert.INSTANCE.toDTO(list);
    }

    @Override
    public List<SysConfigDTO> listByCodes(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return null;
        }
        return SysConfigConvert.INSTANCE.toDTO(lambdaQuery().select(
            SysConfig::getConfigName,
            SysConfig::getConfigCode,
            SysConfig::getConfigValue,
            SysConfig::getValueType,
            SysConfig::getConfigStatus
        ).in(SysConfig::getConfigCode, codes).list());
    }
}
