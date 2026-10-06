package com.carlos.system.region.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.base.RegionInfo;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.region.config.RegionConstant;
import com.carlos.system.region.convert.SysRegionConvert;
import com.carlos.system.region.manager.SysRegionManager;
import com.carlos.system.region.pojo.dto.SysRegionDTO;
import com.carlos.system.region.pojo.entity.SysRegion;
import com.carlos.system.region.pojo.excel.RegionExcel;
import com.carlos.system.region.pojo.param.SysRegionConvertParam;
import com.carlos.system.region.pojo.vo.SysRegionVO;
import com.carlos.util.easyexcel.ExcelUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * <p>
 * 行政区域划分 业务接口实现类
 * </p>
 *
 * @author carlos
 * @date 2022-11-8 19:30:25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRegionService {

    /**
     * 行政区域层级深度的兜底上限
     *
     * <p>
     * 实际业务最深为「省/直辖市 → 市 → 区/县 → 街道/乡镇 → 社区/村 → 村下级」6 级，
     * 这里只作为**脏数据成环时**的死循环保护（沿 parentCode 上溯不会停），
     * 与 {@code SysMenuService#MAX_MENU_DEPTH} 同义。
     * </p>
     */
    private static final int MAX_REGION_DEPTH = 32;

    private final SysRegionManager regionManager;

    /**
     * 新增行政区域划分
     *
     * @param dto 行政区域划分数据
     * @author carlos
     * @date 2022-11-8 19:30:25
     */
    public void addSysRegion(SysRegionDTO dto) {
        // 顶级区域的上级编码归一化为 "0"
        dto.setParentCode(this.normalizeParentCode(dto.getParentCode()));
        // 新增区域校验
        this.checkRegion(dto);
        // 设置区域类型
        setRegionType(dto);
        // 父级的父级编码
        List<String> ancestors = this.collectAncestors(dto.getParentCode());
        ancestors.add(dto.getParentCode());
        dto.setParents(String.join(",", ancestors));
        boolean success = this.regionManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            log.error("region save failed");
            throw CommonErrorCode.BUSINESS_ERROR.exception("区域新增失败");
        }
        Serializable id = dto.getId();
        // 保存完成的后续业务
    }

    /**
     * 归一化上级区域编码：空值一律落到顶级哨兵 {@link RegionConstant#TOP_REGION_PARENT_CODE}（"0"）
     *
     * <p>
     * 后端全链路都以字面量 "0" 认根 —— {@link #loadRegionStep} 对空入参默认 "0"、
     * {@code importCustomRegions} 对缺失父级写 "0"、{@code getRegionTree} 取 {@code map.get("0")} 作根、
     * {@link #collectAncestors} 遇到 "0" 即终止上溯。
     * </p>
     *
     * <p>
     * 而 {@code SysRegionCreateParam.parentCode} 此前带 {@code @NotBlank}，顶级区域在接口层就被拒；
     * 放开必填后必须在这里归一化，否则空串会原样写进库，树形视图再也认不出它。
     * </p>
     *
     * @param parentCode 原始上级编码（可为 null / 空串 / 空白）
     * @return 非空的上级编码
     */
    private String normalizeParentCode(String parentCode) {
        return StrUtil.isBlank(parentCode) ? RegionConstant.TOP_REGION_PARENT_CODE : parentCode.trim();
    }

    private void checkRegion(SysRegionDTO dto) {
        long getByCode = this.regionManager.count(new LambdaQueryWrapper<SysRegion>()
            .eq(SysRegion::getRegionCode, dto.getRegionCode())
            .eq(SysRegion::getParentCode, dto.getParentCode()));
        if (getByCode > 0) {
            throw SystemErrorCode.SYS_REGION_CODE_ALREADY_EXISTS.exception("区域码已存在，添加失败");
        }
        // 上级必须是已存在的区域；否则会产出一行任何树形视图都到不了的孤儿数据
        if (!RegionConstant.TOP_REGION_PARENT_CODE.equals(dto.getParentCode())
            && this.regionManager.getByRegionCode(dto.getParentCode()) == null) {
            throw SystemErrorCode.SYS_REGION_PARENT_NOT_FOUND.exception("上级地区不存在：" + dto.getParentCode());
        }
    }


    /**
     * 删除行政区域划分
     *
     * @param ids 行政区域划分id
     * @author carlos
     * @date 2022-11-8 19:30:25
     */
    public void deleteSysRegion(Set<Long> ids) {
        for (Serializable id : ids) {
            SysRegionDTO region = this.regionManager.getDtoById(id);
            long subCount = this.regionManager.countByParentCode(region.getRegionCode());
            if (subCount > 0) {
                throw SystemErrorCode.SYS_REGION_HAS_CHILDREN.exception(region.getRegionName() + "删除失败,请确保该区域下不存在子区域");
            }
            boolean success = this.regionManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改行政区域划分信息
     *
     * @param dto 对象信息
     * @author carlos
     * @date 2022-11-8 19:30:25
     */
    public void updateSysRegion(SysRegionDTO dto) {
        if (StrUtil.isBlank(dto.getRegionCode())) {
            throw CommonErrorCode.PARAM_MISSING.exception("区域编码为空，更新失败");
        }
        SysRegionDTO dtoByRegionCode = this.regionManager.getByRegionCode(dto.getRegionCode());
        if (dtoByRegionCode != null) {
            if (!ObjectUtil.equals(dto.getId(), dtoByRegionCode.getId())) {
                throw SystemErrorCode.SYS_REGION_CODE_ALREADY_EXISTS.exception("更新失败，行政区域编码" + dto.getRegionCode() + "已存在");
            }
        }
        // 上级区域：**留空表示「不修改」**，只有显式传顶级哨兵 "0" 才是移动到顶级。
        //
        // 这里刻意不把空值归一化成 "0"：编辑一个下级区域时若漏传 parentCode，
        // 归一化会把它静默提升为顶级区域，同时 regionType 被重算成 "5"，一次误操作就改错了层级。
        if (StrUtil.isNotBlank(dto.getParentCode())) {
            dto.setParentCode(dto.getParentCode().trim());
            this.checkParentChange(dto);
            // 设置区域类型（仅父级确实变化时重算，避免无谓覆盖）
            setRegionType(dto);
        }
        regionManager.modify(dto);
    }

    /**
     * 校验「改挂上级区域」这一次变更是否成立
     *
     * <p>
     * 两件事：目标上级必须存在；且不能把区域挂到自身或自己的子孙下。
     * 后者不只是层级语义问题 —— 父子链一旦成环，{@link #collectAncestors} 会沿
     * parentCode 无限上溯，创建/更新请求直接挂死；环上的节点也回溯不到根，
     * 在树形视图里整棵子树凭空消失。
     * </p>
     *
     * <p>
     * 做法与 {@code SysMenuService#checkParentCycle} 一致：从目标上级沿 parentCode
     * 向上回溯，途中命中当前区域即判定成环；回溯到顶级哨兵即终止，深度天然有界，
     * 再用 {@link #MAX_REGION_DEPTH} 兜底历史上可能已存在的脏数据环。
     * </p>
     *
     * @param dto 待更新的区域（parentCode 非空且已 trim）
     */
    private void checkParentChange(SysRegionDTO dto) {
        String newParentCode = dto.getParentCode();
        if (RegionConstant.TOP_REGION_PARENT_CODE.equals(newParentCode)) {
            return;
        }
        SysRegionDTO self = this.regionManager.getDtoById(dto.getId());
        if (self == null) {
            throw SystemErrorCode.SYS_REGION_NOT_FOUND.exception();
        }
        String cursor = newParentCode;
        for (int depth = 0; cursor != null && !RegionConstant.TOP_REGION_PARENT_CODE.equals(cursor); depth++) {
            if (depth >= MAX_REGION_DEPTH) {
                log.error("Region parent chain too deep or cyclic, regionId:{}, newParentCode:{}", dto.getId(), newParentCode);
                throw SystemErrorCode.SYS_SYSTEM_ERROR.exception("地区层级异常，请检查现有上级配置");
            }
            if (cursor.equals(self.getRegionCode())) {
                if (depth == 0) {
                    throw SystemErrorCode.SYS_REGION_PARENT_CANNOT_BE_SELF.exception();
                }
                throw SystemErrorCode.SYS_REGION_PARENT_CANNOT_BE_DESCENDANT.exception();
            }
            SysRegionDTO parent = this.regionManager.getByRegionCode(cursor);
            if (parent == null) {
                throw SystemErrorCode.SYS_REGION_PARENT_NOT_FOUND.exception("上级地区不存在：" + cursor);
            }
            cursor = parent.getParentCode();
        }
    }

    private void setRegionType(SysRegionDTO dto) {
        if (RegionConstant.TOP_REGION_PARENT_CODE.equals(dto.getParentCode())) {
            // 表示为顶级 regionType为5 省级
            dto.setRegionType(RegionConstant.TOP_REGION_PARENT_TYPE);
        } else {
            // 查询父级区域
            SysRegionDTO region = this.regionManager.getByRegionCode(dto.getParentCode());
            if (Objects.nonNull(region) && region.getRegionType() != null) {
                dto.setRegionType(String.valueOf(Integer.parseInt(region.getRegionType()) - 1));
            }
        }
    }

    /**
     * 逐级加载区域信息
     *
     * @param regionCode 区域编码 为空代表从最上层查询
     * @return java.util.List<com.carlos.system.region.pojo.vo.SysRegionVO>
     * @author Carlos
     * @date 2022/11/16 17:25
     */
    public List<SysRegionDTO> loadRegionStep(String regionCode) {
        if (StrUtil.isBlank(regionCode)) {
            regionCode = "0";
        }
        List<SysRegionDTO> dtos = this.regionManager.listByParentCode(regionCode);
        if (CollUtil.isEmpty(dtos)) {
            return Collections.emptyList();
        }
        dtos.forEach(i -> {
            i.setSubNum(this.regionManager.countByParentCode(i.getRegionCode()));
        });
        dtos.sort(Comparator.comparing(SysRegionDTO::getOrderNum));
        return dtos;
    }

    /**
     * 获取区域属性列表
     *
     * @param parentId 参数0
     * @param name     参数1
     * @param code     参数2
     * @return java.util.List<com.carlos.system.region.pojo.dto.SysRegionDTO>
     * @author Carlos
     * @date 2022/11/17 10:18
     */
    public List<SysRegionVO> getRegionTree(String parentId, String name, String code, boolean detail, boolean codeTop) {
        List<SysRegionVO> result = new ArrayList<>();
        // 查找库中所有的区域信息
        List<SysRegionDTO> all = this.regionManager.listAll();
        if (CollectionUtil.isEmpty(all)) {
            return Collections.emptyList();
        }
        List<SysRegionVO> sysRegionVOS = SysRegionConvert.INSTANCE.dto2vo(all);
        // 如果全部查询条件为空则展示全部区域树
        if (StrUtil.isBlank(parentId) && StrUtil.isBlank(name) && StrUtil.isBlank(code)) {
            Map<String, List<SysRegionVO>> map = sysRegionVOS.stream().collect(Collectors.groupingBy(SysRegionVO::getParentCode));
            List<SysRegionVO> topTree = map.get("0");
            if (CollUtil.isEmpty(topTree)) {
                return Collections.emptyList();
            }
            for (SysRegionVO sysRegionVO : topTree) {
                List<SysRegionVO> allRegionTree = this.getAllRegionTree(sysRegionVO, map);
                allRegionTree.sort(Comparator.comparing(SysRegionVO::getOrderNum));
                sysRegionVO.setChildren(allRegionTree);
                result.add(sysRegionVO);
            }
        } else if (codeTop && StrUtil.isNotBlank(code)) {
            // 现在的逻辑是 以客户输入的coded 查询出对应的区域树
            SysRegionVO parent = sysRegionVOS.stream().filter(t -> t.getRegionCode().equals(code)).findFirst().get();
            Map<String, List<SysRegionVO>> map = sysRegionVOS.stream().collect(Collectors.groupingBy(SysRegionVO::getParentCode));
            List<SysRegionVO> topTree = map.get(code);
            if (CollUtil.isNotEmpty(topTree)) {
                for (SysRegionVO sysRegionVO : topTree) {
                    List<SysRegionVO> allRegionTree = this.getAllRegionTree(sysRegionVO, map);
                    allRegionTree.sort(Comparator.comparing(SysRegionVO::getOrderNum));
                    sysRegionVO.setChildren(allRegionTree);
                    result.add(sysRegionVO);
                }
            }
            result.sort(Comparator.comparing(SysRegionVO::getOrderNum));
            parent.setChildren(result);
            return Stream.of(parent).collect(Collectors.toList());
        } else {
            // 之前的做法是 从根节点由上到下获取到当前code
            List<SysRegionDTO> list = this.regionManager.getList(parentId, name, code, detail);
            for (SysRegionDTO regionDTO : list) {
                SysRegionVO sysRegionVO = SysRegionConvert.INSTANCE.toVO(regionDTO);
                SysRegionVO regionTreeByChildVo = this.getRegionTreeByChildVo(sysRegionVO, all);
                if (regionTreeByChildVo != null) {
                    result.add(regionTreeByChildVo);
                }
            }
        }
        result.sort(Comparator.comparing(SysRegionVO::getOrderNum));
        return result;
    }

    /**
     * 获取所有区域树形列表
     *
     * @return java.util.List<com.carlos.system.region.pojo.dto.SysRegionDTO>
     * @author Carlos
     * @date 2022/11/17 10:18
     */
    public List<SysRegionDTO> getRegionTree() {
        List<SysRegionDTO> result = new ArrayList<>();
        // 查找库中所有的区域信息
        List<SysRegion> all = this.regionManager.list();
        List<SysRegionDTO> sysRegionVOS = SysRegionConvert.INSTANCE.toDTO(all);
        // 如果全部查询条件为空则展示全部区域树
        Map<String, List<SysRegionDTO>> map = sysRegionVOS.stream().collect(Collectors.groupingBy(SysRegionDTO::getParentCode));
        List<SysRegionDTO> topTree = map.get("0");
        if (topTree == null) {
            return result;
        }
        for (SysRegionDTO sysRegionVO : topTree) {
            List<SysRegionDTO> allRegionTree = this.getAllRegionTree(sysRegionVO, map);
            allRegionTree.sort(Comparator.comparing(SysRegionDTO::getOrderNum));
            sysRegionVO.setChildren(allRegionTree);
            result.add(sysRegionVO);
        }
        result.sort(Comparator.comparing(SysRegionDTO::getOrderNum));
        return result;
    }

    private SysRegionVO getRegionTreeByChildVo(SysRegionVO vo, List<SysRegionDTO> allList) {
        // 判断当前区域是否为顶级区域如果是则返回空
        if (StrUtil.equalsIgnoreCase(vo.getParentCode(), "0")) {
            return vo;
        }
        SysRegionDTO parent = allList.stream().filter(
            region -> StrUtil.equalsIgnoreCase(vo.getParentCode(), region.getRegionCode())).findFirst().get();
        if (parent == null) {
            return vo;
        }
        SysRegionVO parentVO = SysRegionConvert.INSTANCE.toVO(parent);
        ArrayList<SysRegionVO> sysRegionVOS = new ArrayList<SysRegionVO>() {
            private static final long serialVersionUID = 3004729168077681144L;

            {
                this.add(vo);
            }
        };
        sysRegionVOS.sort(Comparator.comparing(SysRegionVO::getOrderNum));
        parentVO.setChildren(sysRegionVOS);

        return this.getRegionTreeByChildVo(parentVO, allList);

    }

    private List<SysRegionVO> getAllRegionTree(SysRegionVO vo, Map<String, List<SysRegionVO>> map) {
        List<SysRegionVO> sysRegions = map.get(vo.getRegionCode());
        if (CollectionUtil.isEmpty(sysRegions)) {
            return new ArrayList<>();
        }
        for (SysRegionVO sysRegion : sysRegions) {
            List<SysRegionVO> allRegionTree = this.getAllRegionTree(sysRegion, map);
            allRegionTree.sort(Comparator.comparing(SysRegionVO::getOrderNum));
            sysRegion.setChildren(allRegionTree);
        }
        return sysRegions;
    }

    private List<SysRegionDTO> getAllRegionTree(SysRegionDTO vo, Map<String, List<SysRegionDTO>> map) {
        List<SysRegionDTO> sysRegions = map.get(vo.getRegionCode());
        if (CollectionUtil.isEmpty(sysRegions)) {
            return new ArrayList<>();
        }
        for (SysRegionDTO sysRegion : sysRegions) {
            List<SysRegionDTO> allRegionTree = this.getAllRegionTree(sysRegion, map);
            allRegionTree.sort(Comparator.comparing(SysRegionDTO::getOrderNum));
            sysRegion.setChildren(allRegionTree);
        }
        return sysRegions;
    }

    /**
     * 预览指定级数区域名称
     *
     * @param regionCode 区域编码
     * @param limit      级数限制 0代表不限制 null仅当前级
     * @return java.lang.String [A,B,C]
     * @author Carlos
     * @date 2022/12/5 13:23
     */
    public List<String> previewRegionName(String regionCode, Integer limit) {
        long parentNum = 0;
        // 当前级数
        if (limit != null && limit == 0) {
            parentNum = Integer.MAX_VALUE;
        }
        if (limit != null && limit > 1) {
            parentNum = limit - 1;
        }
        List<String> parentCodes = null;
        if (parentNum > 0) {
            parentCodes = regionManager.getAncestorIdsFromCache(regionCode, parentNum);
        }
        if (CollUtil.isEmpty(parentCodes)) {
            parentCodes = Lists.newArrayList();
        }
        parentCodes.add(regionCode);
        List<String> fields = Lists.newArrayList("regionName");
        // 从hash缓存中获取名称
        List<SysRegionDTO> regions = regionManager.listRegionFromCache(parentCodes, fields);
        if (CollUtil.isNotEmpty(regions)) {
            return regions.stream().map(SysRegionDTO::getRegionName).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    /**
     * 获取所有子级区域编码
     * @author Carlos
     * @date 2022/12/13 15:52
     */

    public Set<String> getRegionSubCodes(String regionCode) {
        // FIXME: Carlos 2025-12-08 方式优化
        Set<String> codes = Sets.newHashSet();
        List<SysRegionDTO> all = this.regionManager.listAll();
        Map<String, List<SysRegionDTO>> parentMap = all.stream().collect(Collectors.groupingBy(SysRegionDTO::getParentCode));
        codes.add(regionCode);
        this.getRegionCodeRecursion(regionCode, parentMap, codes);
        return codes;
    }

    public void getRegionCodeRecursion(String regionCode, Map<String, List<SysRegionDTO>> parentMap, Set<String> codes) {
        List<SysRegionDTO> subs = parentMap.get(regionCode);
        if (CollectionUtil.isEmpty(subs)) {
            return;
        }
        for (SysRegionDTO sub : subs) {
            codes.add(sub.getRegionCode());
            this.getRegionCodeRecursion(sub.getRegionCode(), parentMap, codes);
        }
    }

    /**
     * 获取区域信息
     *
     * @param regionCode 区域编码
     * @param limit      参数1
     * @return com.carlos.common.core.base.RegionInfo
     * @author Carlos
     * @date 2022/12/30 13:58
     */

    public RegionInfo getRegionInfo(String regionCode, Integer limit) {
        SysRegionDTO region = this.regionManager.getByRegionCode(regionCode);
        if (region == null) {
            return null;
        }
        RegionInfo regionInfo = new RegionInfo();
        regionInfo.setId(region.getId());
        regionInfo.setCode(regionCode);
        regionInfo.setName(region.getRegionName());
        regionInfo.setFullName(this.previewRegionName(regionCode, limit));

        return regionInfo;
    }

    /**
     * @Title: importRegions
     * @Description: 导入数据初始化
     * @Date: 2023/2/21 18:44
     * @Parameters: [regions]
     * @Return void
     */

    @Transactional(rollbackFor = Exception.class)
    public void importRegions(List<SysRegionDTO> regions) {
        //

        // 保存数据
        boolean success = regionManager.addBatch(regions);
        if (!success) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("区域数据初始化导入失败");
        }
    }

    /**
     * @Title: export
     * @Description: 导出区域信息
     * @Date: 2023/2/22 10:52
     * @Parameters: [response, isTemplate]
     * @Return void
     */
    public void export(HttpServletResponse response, boolean isTemplate) {
        // 表格标题，就是模型的属性名
        String name = "行政区域--" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN);

        List<RegionExcel> data = Lists.newArrayList();
        if (!isTemplate) {
            // 获取数据
            List<SysRegionDTO> list = regionManager.listAll();
            data = SysRegionConvert.INSTANCE.dtoToexcel(list);
        }
        try {
            ExcelUtil.download(response, name, RegionExcel.class, data);
        } catch (Exception e) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("区域信息导出失败");
        }
    }

    /**
     * listAll
     *
     * @return java.util.List<com.carlos.system.region.pojo.dto.SysRegionDTO>
     * @author Carlos
     * @date 2024/4/8 10:33
     */
    public List<SysRegionDTO> listAll() {
        return regionManager.listAll();
    }

    /**
     * 重置父级关系
     *
     * @author Carlos
     * @date 2024/4/8 10:33
     */
    public void resetParents() {
        List<SysRegion> batchUpdates = new ArrayList<>();
        int processed = 0;
        // 第一次查询：获取所有根节点（parentCode = "0"）
        List<SysRegionDTO> currentLayer = regionManager.listByParentCode("0");
        while (!currentLayer.isEmpty()) {
            // 收集当前层所有节点的 regionCode，用于查询下一层
            List<String> currentCodes = currentLayer.stream()
                .map(SysRegionDTO::getRegionCode)
                .collect(Collectors.toList());

            // 批量更新当前层节点的 parents 字段
            for (SysRegionDTO region : currentLayer) {
                SysRegion update = new SysRegion();
                update.setId(region.getId());
                update.setParents(region.getParents());
                batchUpdates.add(update);

                // 批量提交更新
                if (batchUpdates.size() >= 1000) {
                    regionManager.updateBatchById(batchUpdates);
                    batchUpdates.clear();
                }

                processed++;
                if (processed % 1000 == 0) {
                    log.info("init parents progress: {}", processed);
                }
            }

            // 查询下一层节点：parentCode 在当前层节点的 regionCode 中
            List<SysRegionDTO> nextLayer = regionManager.listByParentCodes(currentCodes);

            // 为下一层节点设置 parents 字段
            Map<String, String> parentPaths = currentLayer.stream()
                .collect(Collectors.toMap(
                    SysRegionDTO::getRegionCode,
                    r -> StrUtil.isBlank(r.getParents()) ? r.getRegionCode() : r.getParents() + "," + r.getRegionCode()
                ));

            for (SysRegionDTO child : nextLayer) {
                String parentCode = child.getParentCode();
                String parentPath = parentPaths.get(parentCode);
                if (parentPath != null) {
                    child.setParents(parentPath);
                }
            }

            // 移动到下一层
            currentLayer = nextLayer;
        }

        // 处理最后一批更新
        if (!batchUpdates.isEmpty()) {
            regionManager.updateBatchById(batchUpdates);
        }

        log.info("init parents finished, total={}", processed);
    }


    /**
     * 获取父级code链
     *
     * @param regionCode 区域编码
     * @return java.util.List<java.lang.String>
     * @author Carlos
     * @date 2025-12-08 10:52
     */
    public List<String> collectAncestors(String regionCode) {
        LinkedList<String> path = new LinkedList<>();
        String currentParent = regionCode;
        while (true) {
            SysRegionDTO r = regionManager.getByRegionCode(currentParent);
            if (r == null || "0".equals(r.getParentCode())) {
                break;
            }
            path.addFirst(r.getParentCode());   // 头部插入，保证顶级在前
            currentParent = r.getParentCode();
        }
        return path;
    }

    /**
     * @Title: importCustomRegions
     * @Description: 导入自定义数据
     * @Date: 2025/12/08 10:52
     * @Parameters: [param]
     * @Return void
     */
    @SneakyThrows
    public void importCustomRegions(SysRegionConvertParam param) {
        MultipartFile file = param.getFile();
        InputStream inputStream = file.getInputStream();
        ExcelReader reader = cn.hutool.poi.excel.ExcelUtil.getReader(inputStream);
        List<Sheet> sheets = reader.getSheets();

        Map<String, String> idMap = new HashMap<>(4);

        // 2. 遍历每个 Sheet
        for (Sheet sheet : sheets) {
            String sheetName = sheet.getSheetName();
            log.info("开始处理 Sheet: {}", sheetName);
            reader.setSheet(sheet);
            List<Map<String, Object>> rows = reader.readAll();
            List<SysRegionDTO> regions = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                String regionCode = row.get(param.getRegionCode()).toString();
                String regionName = (String) row.get(param.getRegionName());
                String pid = row.get(param.getPid()).toString();
                String id = row.get(param.getId()).toString();
                idMap.put(id, regionCode);
                SysRegionDTO region = new SysRegionDTO();
                region.setRegionCode(regionCode);
                region.setRegionName(regionName);
                String parentCode = idMap.get(pid);
                if (StrUtil.isBlank(parentCode)) {
                    parentCode = "0";
                }
                region.setParentCode(parentCode);
                regions.add(region);
                // 分批次写入
                if (regions.size() >= 1000) {
                    importRegions(regions);
                    regions.clear();
                }

            }
            importRegions(regions);
            regions.clear();
            log.info("处理完成 Sheet: {}", sheetName);
        }

    }

    /**
     * 根据code，获取上级区域,传入C,并按照A、B、C层级返回
     *
     * @param regionCode 区域编码
     * @return list
     */
    public List<SysRegionDTO> getAllParentRegions(String regionCode) {
        List<String> parentCode = regionManager.getAncestorIdsFromCache(regionCode, 0);
        if (CollUtil.isEmpty(parentCode)) {
            parentCode = Lists.newArrayList();
        }
        parentCode.add(regionCode);

        // 根据parentCode列表查询对应的区域信息
        List<String> fields = Lists.newArrayList("id", "regionName", "regionCode", "parentCode");
        List<SysRegionDTO> sysRegionDTOS = regionManager.listRegionFromCache(parentCode, fields);

        // 按照路径顺序构建父子结构
        if (CollUtil.isEmpty(sysRegionDTOS)) {
            return Lists.newArrayList();
        }

        // 构建父子结构：从后往前，将每个节点作为前一个节点的子节点
        if (sysRegionDTOS.size() > 1) {
            for (int i = sysRegionDTOS.size() - 2; i >= 0; i--) {
                SysRegionDTO parent = sysRegionDTOS.get(i);
                SysRegionDTO child = sysRegionDTOS.get(i + 1);

                // 如果父节点的children为null，则初始化
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }

                // 将子节点添加到父节点的children列表中
                parent.getChildren().add(child);
            }
        }

        // 只返回根节点（第一个节点）
        if (!sysRegionDTOS.isEmpty()) {
            return Lists.newArrayList(sysRegionDTOS.getFirst());
        }

        return sysRegionDTOS;
    }
}
