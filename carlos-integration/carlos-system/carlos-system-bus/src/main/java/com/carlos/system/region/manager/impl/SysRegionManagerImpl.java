package com.carlos.system.region.manager.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.redis.ICacheManager;
import com.carlos.redis.util.RedisUtil;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.region.config.RegionConstant;
import com.carlos.system.region.convert.SysRegionConvert;
import com.carlos.system.region.manager.SysRegionManager;
import com.carlos.system.region.mapper.SysRegionMapper;
import com.carlos.system.region.pojo.dto.SysRegionDTO;
import com.carlos.system.region.pojo.entity.SysRegion;
import com.carlos.system.region.pojo.param.SysRegionPageParam;
import com.carlos.system.region.pojo.vo.SysRegionVO;
import com.google.common.collect.Lists;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 行政区域划分 查询封装实现类
 * </p>
 *
 * @author carlos
 * @date 2022-11-8 19:30:24
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysRegionManagerImpl extends BaseServiceImpl<SysRegionMapper, SysRegion> implements SysRegionManager, ICacheManager<SysRegionDTO> {


    /** 缓存空间 */
    public static final String PREFIX = "system:region:";


    /** 参数：区划编码 */
    public static final String SELF_KEY = PREFIX + "hash:%s";
    /**
     * 直接子列表 → Set
     * 业务最常问“某节点下一级是谁”，SMEMBERS 一次返回，时间复杂度 O(N) 且 N 就是子节点数（通常 <200）。
     * 插入/删除子节点只需  SADD/SREM ，O(1) 原子完成；若用 List 会留下空洞或需全表重排。
     * 需要去重场景（批量导入可能重复）Set 天然幂等。
     * 参数：编码
     */
    public static final String CHILDREN_KEY = PREFIX + "children:%s";
    /**
     * 全部子孙列表 → Set（可选，用于“查下级所有”）
     * 参数：编码
     */
    public static final String DESC_KEY = PREFIX + "desc:%s";

    /**
     * 祖先链 / 面包屑 → List
     * 参数：区划编码
     */
    public static final String ANC_KEY = PREFIX + "ancestors:%s";

    @Override
    public boolean add(SysRegionDTO dto) {
        SysRegion entity = SysRegionConvert.INSTANCE.toDO(dto);
        boolean success = this.save(entity);
        if (!success) {
            log.warn("Insert 'SysRegion' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'SysRegion' data: id:{}", entity.getId());
        SysRegionDTO region = getDtoById(dto.getId());
        this.putCache(region);
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        SysRegionDTO region = this.getDtoById(id);
        if (region == null) {
            throw SystemErrorCode.SYS_REGION_NOT_FOUND.exception();
        }
        boolean success = this.removeById(id);
        if (!success) {
            log.warn("Remove 'SysRegion' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'SysRegion' data by id:{}", id);
        this.deleteCache(region);
        return true;
    }

    @Override
    public boolean modify(SysRegionDTO dto) {
        SysRegion entity = SysRegionConvert.INSTANCE.toDO(dto);
        boolean success = this.updateById(entity);
        if (!success) {
            log.warn("Update 'SysRegion' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'SysRegion' data by id:{}", dto.getId());
        SysRegionDTO region = getDtoById(dto.getId());
        SysRegionConvert.INSTANCE.merge(dto, region);
        this.updateCache(region);
        return true;
    }

    @Override
    public SysRegionDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        SysRegion entity = this.getBaseMapper().selectById(id);
        return SysRegionConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<SysRegionVO> getPage(SysRegionPageParam param) {
        LambdaQueryWrapper<SysRegion> wrapper = this.queryWrapper();
        wrapper.select(
            SysRegion::getId,
            SysRegion::getRegionCode,
            SysRegion::getRegionName,
            SysRegion::getParentCode,
            SysRegion::getParents,
            SysRegion::getRegionType,
            SysRegion::getRegionArea,
            SysRegion::getRegionPeopleNumber,
            SysRegion::getRegionLevel,
            SysRegion::getRegionLeader,
            SysRegion::getExtend,
            SysRegion::getLongitude,
            SysRegion::getLatitude,
            SysRegion::getBoundLeft,
            SysRegion::getBoundRight,
            SysRegion::getBoundBottom,
            SysRegion::getBoundTop,
            SysRegion::getGisOid,
            SysRegion::getCreateBy,
            SysRegion::getCreateTime,
            SysRegion::getUpdateBy,
            SysRegion::getUpdateTime
        ).eq(StrUtil.isNotBlank(param.getRegionCode()), SysRegion::getRegionCode, param.getRegionCode())
            .like(StrUtil.isNotBlank(param.getRegionName()), SysRegion::getRegionName, param.getRegionName())
            .eq(StrUtil.isNotBlank(param.getParentCode()), SysRegion::getParentCode, param.getParentCode())
            .eq(StrUtil.isNotBlank(param.getRegionType()), SysRegion::getRegionType, param.getRegionType())
            .eq(param.getRegionLevel() != null, SysRegion::getRegionLevel, param.getRegionLevel())
            .like(StrUtil.isNotBlank(param.getRegionLeader()), SysRegion::getRegionLeader, param.getRegionLeader());
        PageInfo<SysRegion> page = this.page(this.pageInfo(param), wrapper);
        Paging<SysRegionVO> result = MybatisPage.convert(page, SysRegionConvert.INSTANCE::toVO);
        this.fillParentName(result);
        return result;
    }

    /**
     * 回填「上级区域名称」
     *
     * <p>
     * 列表行上只有 parent_code（区域**编码**），直接展示编码对使用者没有意义。
     * 这里按**当前页**出现过的上级编码做一次 in 查询（最多 size 个编码）把编码翻成名称，
     * 不整表加载 —— sys_region 支持按 code 全量导入行政区划，行数可达数万。
     * </p>
     *
     * <p>
     * 顶级区域的 parent_code 是哨兵 "0"，查不到对应行，回填后保持 null，由前端显示为「顶级区域」。
     * </p>
     */
    private void fillParentName(Paging<SysRegionVO> result) {
        List<SysRegionVO> records = result == null ? null : result.getRecords();
        if (CollUtil.isEmpty(records)) {
            return;
        }
        Set<String> parentCodes = records.stream()
            .map(SysRegionVO::getParentCode)
            .filter(code -> StrUtil.isNotBlank(code) && !RegionConstant.TOP_REGION_PARENT_CODE.equals(code))
            .collect(Collectors.toSet());
        if (parentCodes.isEmpty()) {
            return;
        }
        Map<String, String> nameByCode = this.lambdaQuery()
            .select(SysRegion::getRegionCode, SysRegion::getRegionName)
            .in(SysRegion::getRegionCode, parentCodes)
            .list()
            .stream()
            .collect(Collectors.toMap(SysRegion::getRegionCode, SysRegion::getRegionName, (a, b) -> a));
        records.forEach(vo -> vo.setParentName(nameByCode.get(vo.getParentCode())));
    }


    @Override
    public SysRegionDTO getByRegionCode(String regionCode) {
        if (regionCode.equals(RegionConstant.TOP_REGION_PARENT_CODE)) {
            return null;
        }
        SysRegionDTO region = RedisUtil.getHash(String.format(regionCode), SysRegionDTO.class);
        if (region != null) {
            return region;
        }
        SysRegion one = this.lambdaQuery()
            .select(
                SysRegion::getId,
                SysRegion::getRegionCode,
                SysRegion::getRegionName,
                SysRegion::getParentCode,
                SysRegion::getRegionType
            ).eq(SysRegion::getRegionCode, regionCode)
            .one();
        if (one == null) {
            return null;
        }
        region = SysRegionConvert.INSTANCE.toDTO(one);
        this.putCache(region);
        return region;
    }

    @Override
    public List<SysRegionDTO> listByParentCode(String parentCode) {
        Set<String> childrenCodes = RedisUtil.getSet(String.format(CHILDREN_KEY, parentCode));
        List<SysRegionDTO> regions = Collections.emptyList();
        if (CollUtil.isNotEmpty(childrenCodes)) {
            Set<String> keys = childrenCodes.stream().map(code -> String.format(SELF_KEY, code)).collect(Collectors.toSet());
            Map<String, SysRegionDTO> maps = RedisUtil.hashMultiGetAll(keys, SysRegionDTO.class);
            if (CollUtil.isNotEmpty(maps)) {
                regions = new ArrayList<>(maps.values());
            }
        }

        if (CollUtil.isNotEmpty(regions)) {
            return regions;
        }
        List<SysRegion> list = this.lambdaQuery().select(
                SysRegion::getId,
                SysRegion::getRegionCode,
                SysRegion::getRegionName,
                SysRegion::getParentCode,
                SysRegion::getRegionType
            )
            .eq(SysRegion::getParentCode, parentCode)
            .list();
        return SysRegionConvert.INSTANCE.toDTO(list);
    }

    @Override
    public List<SysRegionDTO> listByParentCodes(List<String> codes) {
        List<SysRegion> list = this.lambdaQuery().select(
                SysRegion::getId,
                SysRegion::getRegionCode,
                SysRegion::getRegionName,
                SysRegion::getParentCode,
                SysRegion::getRegionType
            )
            .in(SysRegion::getParentCode, codes)
            .list();
        return SysRegionConvert.INSTANCE.toDTO(list);
    }

    @Override
    public Long countByParentCode(String parentCode) {
        if (StrUtil.isBlank(parentCode)) {
            return 0L;
        }
        return RedisUtil.getSetSize(String.format(CHILDREN_KEY, parentCode));
    }

    @Override
    public List<SysRegionDTO> listRegionFromCache(List<String> codes, List<String> fields) {
        if (CollUtil.isEmpty(codes)) {
            return Collections.emptyList();
        }
        List<String> keys = codes.stream().map(code -> String.format(SELF_KEY, code)).collect(Collectors.toList());
        Map<String, SysRegionDTO> maps = RedisUtil.hashMultiGet(keys, fields, SysRegionDTO.class);
        if (CollUtil.isEmpty(maps)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(maps.values());
    }

    @Override
    public List<SysRegionDTO> getList(String parentId, String name, String code, boolean detail) {
        LambdaQueryWrapper<SysRegion> wrapper = this.queryWrapper()
            .eq(CharSequenceUtil.isNotBlank(parentId), SysRegion::getParentCode, parentId)
            .eq(CharSequenceUtil.isNotBlank(code), SysRegion::getRegionCode, code)
            .like(CharSequenceUtil.isNotBlank(name), SysRegion::getRegionName, name);

        if (detail) {
            wrapper.select(
                SysRegion::getId,
                SysRegion::getRegionCode,
                SysRegion::getRegionName,
                SysRegion::getParentCode,
                SysRegion::getParents,
                SysRegion::getRegionType,
                SysRegion::getRegionArea,
                SysRegion::getRegionPeopleNumber,
                SysRegion::getRegionLevel,
                SysRegion::getRegionLeader,
                SysRegion::getExtend,
                SysRegion::getLongitude,
                SysRegion::getLatitude,
                SysRegion::getBoundLeft,
                SysRegion::getBoundRight,
                SysRegion::getBoundBottom,
                SysRegion::getBoundTop,
                SysRegion::getGisOid,
                SysRegion::getCreateBy,
                SysRegion::getCreateTime,
                SysRegion::getUpdateBy,
                SysRegion::getUpdateTime
            );
        } else {
            wrapper.select(
                SysRegion::getId,
                SysRegion::getRegionCode,
                SysRegion::getRegionName,
                SysRegion::getParentCode,
                SysRegion::getParents
            );
        }
        List<SysRegion> menus = this.list(wrapper);
        return SysRegionConvert.INSTANCE.toDTO(menus);
    }

    @Override
    public List<SysRegionDTO> listAll() {
        Set<String> keys = RedisUtil.scanKeys(String.format(SELF_KEY, RedisUtil.ALL), 500);
        if (CollUtil.isNotEmpty(keys)) {
            Map<String, SysRegionDTO> depts = RedisUtil.hashMultiGetAll(Lists.newArrayList(keys), 2000, SysRegionDTO.class);
            if (CollUtil.isNotEmpty(depts)) {
                return new ArrayList<>(depts.values());
            }
        }
        List<SysRegion> list = this.list();
        return SysRegionConvert.INSTANCE.toDTO(list);
    }

    @Override
    public boolean addBatch(List<SysRegionDTO> dtos) {
        List<SysRegion> regions = SysRegionConvert.INSTANCE.toDOS(dtos);
        boolean success = saveBatch(regions, 5000);
        if (!success) {
            log.warn("add batch 'SysRegion' data fail, entity:{}", regions);
            return false;
        }
        // 新增成功的后续操作
        log.debug("add batch 'SysRegion' data");
        return true;
    }

    @Override
    public void initCache() {
        try {
            // 分页参数
            int pageSize = 5000; // 每批处理1000条记录
            int current = 1;
            boolean hasMore = true;

            // 使用分页方式处理大量数据
            while (hasMore) {
                PageInfo<SysRegion> pageInfo = this.page(new PageInfo<>(current, pageSize, false),
                    queryWrapper().select(
                        SysRegion::getId,
                        SysRegion::getRegionCode,
                        SysRegion::getRegionName,
                        SysRegion::getParentCode,
                        SysRegion::getParents,
                        SysRegion::getRegionType,
                        SysRegion::getOrderNum
                    )
                );

                List<SysRegion> records = pageInfo.getRecords();
                if (CollUtil.isEmpty(records)) {
                    break;
                }

                RedisUtil.executePipelined(new SessionCallback<Object>() {
                    @Override
                    public Object execute(RedisOperations operations) throws DataAccessException {
                        records.forEach(region -> {
                            SysRegionDTO dto = SysRegionConvert.INSTANCE.toDTO(region);
                            addCache(operations, dto);
                        });
                        return null;
                    }
                });
                // 判断是否还有更多数据
                hasMore = records.size() >= pageSize;
                current++;
            }
            log.info("Region cache init success with pagination");
        } catch (Exception e) {
            log.error("Failed to init region cache with pagination", e);
            throw SystemErrorCode.SYS_REGION_CACHE_INIT_FAILED.exception(e.getMessage());
        }
    }


    @Override

    @SuppressWarnings("unchecked")
    public void putCache(SysRegionDTO bean) {
        RedisUtil.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(@NotNull RedisOperations ops) throws DataAccessException {
                ops.multi();
                addCache(ops, bean);
                return ops.exec();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void addCache(RedisOperations ops, SysRegionDTO bean) {
        String code = bean.getRegionCode();
        String parent = bean.getParentCode();
        String selfKey = String.format(SELF_KEY, code);
        String childrenKey = String.format(CHILDREN_KEY, parent);
        // 父级链
        String ancKey = String.format(ANC_KEY, code);
        // 1. 写本体
        ops.opsForHash().putAll(selfKey, BeanUtil.beanToMap(bean));
        // 2. 加入父的 children
        ops.opsForSet().add(childrenKey, code);
        String parents = bean.getParents();
        if (StrUtil.isNotBlank(parents)) {
            // 当前节点的父节点链
            List<String> ancestors = StrUtil.split(parents, StrUtil.COMMA);
            // 3. 加入所有祖先的 desc 集合加入当前节点（沿祖先链向上）
            ancestors.forEach(a -> ops.opsForSet().add(String.format(DESC_KEY, a), code));
            // 4. 写自己的祖先链
            ops.opsForList().rightPushAll(ancKey, ancestors);
        }
    }

    @Override
    public List<String> getParentsCode(String code) {
        // FIXME: Carlos 2025-12-05 是否需要考虑缓存失效的情况
        return RedisUtil.getList(String.format(ANC_KEY, code));
    }

    @Override
    public void updateCache(SysRegionDTO bean) {
        // FIXME: Carlos 2025-12-08 当前只考虑更改基本信息，不更改层级等内容
        RedisUtil.putHash(String.format(SELF_KEY, bean.getRegionCode()), bean);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deleteCache(SysRegionDTO bean) {
        // 由于业务中已经保证当该节点拥有子节点时，不允许删除，因此，只需处理本体即可
        RedisUtil.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(@NotNull RedisOperations ops) throws DataAccessException {
                ops.multi();
                String code = bean.getRegionCode();
                String parent = bean.getParentCode();
                String selfKey = String.format(SELF_KEY, code);
                String childrenKey = String.format(CHILDREN_KEY, parent);
                String descKey = String.format(DESC_KEY, code);
                String ancKey = String.format(ANC_KEY, code);
                // 1. 删除本体
                ops.delete(selfKey);
                // 2. 删除自己的children集合
                ops.delete(childrenKey);
                // 3. 删除自己的祖先链
                ops.delete(ancKey);
                // 4. 删除当前节点的子孙集合
                ops.delete(descKey);
                // 5. 移除父节点children缓存中的自己
                String childrenKeyItem = String.format(CHILDREN_KEY, parent);
                ops.opsForSet().remove(childrenKeyItem, code);

                // 6. 获取祖先链 从所有祖先的 desc 集合里移除自己
                List<String> ancestors = getAncestorIdsFromCache(code, 0);
                ancestors.forEach(descCode -> {
                    String descKeyItem = String.format(DESC_KEY, descCode);
                    // 祖先中移除自己
                    ops.opsForSet().remove(descKeyItem, code);
                });
                return ops.exec();
            }
        });
    }

    @Override
    public List<String> getAncestorIdsFromCache(String code, long limit) {

        // 当前级数
        if (limit <= 0) {
            limit = Integer.MAX_VALUE;
        }
        List<String> list = RedisUtil.getList(String.format(ANC_KEY, code), -limit, -1L);
        return list;
    }

    @Override
    public Set<String> getDescIdsFromCache(String code) {
        return RedisUtil.getSet(String.format(DESC_KEY, code));
    }

    @Override
    public long clearCache() {
        long deleteCount = RedisUtil.deleteSpace(PREFIX);
        log.info("Region cache has been cleaned, delete count: {}", deleteCount);
        return deleteCount;
    }
}
