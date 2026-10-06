package com.carlos.org.manager.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.org.config.OrgDataScopeHelper;
import com.carlos.org.convert.OrgUserConvert;
import com.carlos.org.manager.OrgDepartmentManager;
import com.carlos.org.manager.OrgUserManager;
import com.carlos.org.mapper.OrgDepartmentMapper;
import com.carlos.org.mapper.OrgUserMapper;
import com.carlos.org.pojo.dto.OrgUserCandidateDTO;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.entity.OrgDepartment;
import com.carlos.org.pojo.entity.OrgUser;
import com.carlos.org.pojo.enums.OrgUserStateEnum;
import com.carlos.org.pojo.param.OrgUserCandidatePageParam;
import com.carlos.org.pojo.param.OrgUserPageParam;
import com.carlos.org.pojo.vo.OrgUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 系统用户 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgUserManagerImpl extends BaseServiceImpl<OrgUserMapper, OrgUser> implements OrgUserManager {

    private final OrgDataScopeHelper dataScopeHelper;

    private final OrgDepartmentMapper departmentMapper;

    private final OrgDepartmentManager departmentManager;

    @Override
    public Paging<OrgUserVO> getPage(OrgUserPageParam param) {
        PageInfo<OrgUser> page = page(pageInfo(param), buildQueryWrapper(param));
        return MybatisPage.convert(page, OrgUserConvert.INSTANCE::toVO);
    }

    @Override
    public Paging<OrgUserCandidateDTO> getCandidatePage(OrgUserCandidatePageParam param) {
        OrgUserCandidatePageParam query = param == null ? new OrgUserCandidatePageParam() : param;
        // includeIds 的「给出但为空」与「未给出」语义不同：前者是「限定为一个空集合」，
        // 也就是无可选人，必须短路成空分页，否则会退化成「不做限定」而泄露全量用户。
        if (query.getIncludeIds() != null && normalizeIds(query.getIncludeIds()).isEmpty()) {
            return MybatisPage.emptyPaging();
        }

        PageInfo<OrgUser> page = page(pageInfo(query), buildCandidateQueryWrapper(query));
        List<OrgUser> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return MybatisPage.convert(page, ignored -> List.of());
        }

        Map<Long, String> deptNames = deptNames(records.stream().map(OrgUser::getMainDeptId).toList());
        return MybatisPage.convert(page, list -> list.stream()
            .map(user -> toCandidateDTO(user, deptNames))
            .toList());
    }

    private LambdaQueryWrapper<OrgUser> buildCandidateQueryWrapper(OrgUserCandidatePageParam param) {
        LambdaQueryWrapper<OrgUser> wrapper = queryWrapper();
        wrapper.select(OrgUser::getId, OrgUser::getAccount, OrgUser::getNickname, OrgUser::getMainDeptId,
            OrgUser::getUserStatus);

        List<Long> includeIds = normalizeIds(param.getIncludeIds());
        wrapper.in(CollUtil.isNotEmpty(includeIds), OrgUser::getId, includeIds);
        wrapper.notIn(CollUtil.isNotEmpty(normalizeIds(param.getExcludeIds())), OrgUser::getId,
            normalizeIds(param.getExcludeIds()));
        if (param.getDeptId() != null) {
            wrapper.in(OrgUser::getMainDeptId, deptTreeIds(param.getDeptId()));
        }
        // 禁用账号不参与选人；锁定是临时状态，仍可选
        wrapper.ne(OrgUser::getUserStatus, OrgUserStateEnum.DISABLE);

        // 分页必须有稳定排序：部门 + 主键（雪花号）构成全序，翻页不会重复或跳记录
        wrapper.orderByAsc(OrgUser::getMainDeptId).orderByAsc(OrgUser::getId);
        return wrapper;
    }

    /**
     * 部门及其全部子孙部门id；部门为空时返回空集合（调用方已保证非空）
     *
     * @param deptId 部门id
     * @return 部门id集合
     */
    private Set<Long> deptTreeIds(Long deptId) {
        Set<Long> deptIds = new LinkedHashSet<>();
        deptIds.add(deptId);
        Set<Long> descIds = departmentManager.getDescIdsFromCache(deptId);
        if (CollUtil.isNotEmpty(descIds)) {
            deptIds.addAll(descIds);
        }
        return deptIds;
    }

    /**
     * 按部门id批量取部门名称：一次查询取回本页涉及的部门，避免逐行回查（N+1）
     *
     * @param deptIds 部门id列表（可含 null）
     * @return 部门id → 部门名称
     */
    private Map<Long, String> deptNames(List<Long> deptIds) {
        Set<Long> distinctIds = new LinkedHashSet<>();
        for (Long deptId : deptIds) {
            if (deptId != null) {
                distinctIds.add(deptId);
            }
        }
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        // 直接查部门表而不是读部门缓存：部门名称是展示性字段，缺一条只让 deptName 为空，
        // 不该因此要求缓存已预热（缓存未预热时读 Redis 会静默返回空，整页都没有部门名）。
        return departmentMapper.selectList(new LambdaQueryWrapper<OrgDepartment>()
                .select(OrgDepartment::getId, OrgDepartment::getDeptName)
                .in(OrgDepartment::getId, distinctIds)).stream()
            .filter(dept -> dept.getId() != null && StrUtil.isNotBlank(dept.getDeptName()))
            .collect(Collectors.toMap(OrgDepartment::getId, OrgDepartment::getDeptName, (a, b) -> a));
    }

    private OrgUserCandidateDTO toCandidateDTO(OrgUser user, Map<Long, String> deptNames) {
        OrgUserCandidateDTO dto = new OrgUserCandidateDTO();
        dto.setUserId(user.getId());
        dto.setCode(user.getAccount());
        dto.setName(user.getNickname());
        dto.setDeptId(user.getMainDeptId());
        dto.setDeptName(deptNames.get(user.getMainDeptId()));
        return dto;
    }

    /**
     * 去掉 null 并去重，保持原顺序
     *
     * @param ids 原始 id 集合
     * @return 清洗后的 id 列表，入参为空时为 null（用于条件构造器的「条件是否生效」判断）
     */
    private List<Long> normalizeIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return null;
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    @Override
    public List<OrgUserDTO> listByParam(OrgUserPageParam param) {
        return OrgUserConvert.INSTANCE.toDTO(list(buildQueryWrapper(param)));
    }

    /**
     * 构造用户列表查询条件
     *
     * <p>
     * ⚠️ 此前 {@code getPage} **完全没有应用任何查询条件** —— 搜索栏的用户名 / 昵称 /
     * 手机号 / 邮箱 / 性别 / 状态六个筛选全部不生效。实测：
     * {@code GET /org/user/page?account=zzz-nomatch} 仍然返回全部可见用户。
     * </p>
     *
     * <p>
     * 而导出走的 {@code listByParam} 另写了一套条件（只有 account / nickname / phone /
     * userStatus / tenantId），于是「列表筛选」与「导出筛选」两者行为还不一致。
     * 现在两者共用本方法，行为天然一致。
     * </p>
     *
     * @param param 查询条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<OrgUser> buildQueryWrapper(OrgUserPageParam param) {
        LambdaQueryWrapper<OrgUser> wrapper = queryWrapper();
        wrapper.select(

            OrgUser::getId,
            OrgUser::getAccount,
            OrgUser::getNickname,
            OrgUser::getPwd,
            OrgUser::getIdentify,
            OrgUser::getPhone,
            OrgUser::getAddress,
            OrgUser::getGender,
            OrgUser::getEmail,
            OrgUser::getAvatar,
            OrgUser::getDescription,
            OrgUser::getUserStatus,
            OrgUser::getMainDeptId,
            OrgUser::getLoginTime,
            OrgUser::getLoginIp,
            OrgUser::getLoginCount,
            OrgUser::getPwdLastModify,
            OrgUser::getCreateBy,
            OrgUser::getCreateTime,
            OrgUser::getUpdateBy,
            OrgUser::getUpdateTime,
            OrgUser::getTenantId
        );

        // 文本条件一律 like（前端可只输入片段，例如手机号后四位）
        wrapper.like(StrUtil.isNotBlank(param.getAccount()), OrgUser::getAccount, param.getAccount())
            .like(StrUtil.isNotBlank(param.getNickname()), OrgUser::getNickname, param.getNickname())
            .like(StrUtil.isNotBlank(param.getPhone()), OrgUser::getPhone, param.getPhone())
            .like(StrUtil.isNotBlank(param.getEmail()), OrgUser::getEmail, param.getEmail())
            // 编码类条件用 eq：0 是合法值，故判 null 而不是判空
            .eq(param.getGender() != null, OrgUser::getGender, param.getGender())
            .eq(param.getUserStatus() != null, OrgUser::getUserStatus, param.getUserStatus())
            .eq(param.getTenantId() != null, OrgUser::getTenantId, param.getTenantId())
            .between(param.getStart() != null && param.getEnd() != null, OrgUser::getCreateTime, param.getStart(), param.getEnd());

        // 分页必须有稳定排序：PostgreSQL 下没有 ORDER BY 的翻页结果不保证稳定，
        // 同一条记录可能重复出现或被跳过。id 是雪花号，做次级键保证全序。
        wrapper.orderByDesc(OrgUser::getCreateTime).orderByDesc(OrgUser::getId);

        // 数据权限范围：必须保留，否则会把范围外的用户一起列出来（导出同理）
        dataScopeHelper.applyUserQueryScope(wrapper);

        return wrapper;
    }

    @Override
    public boolean add(OrgUserDTO dto) {
        OrgUser entity = OrgUserConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgUser' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'OrgUser' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgUser' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgUser' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgUserDTO dto) {
        OrgUser entity = OrgUserConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgUser' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'OrgUser' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgUserDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgUser entity = getBaseMapper().selectById(id);
        return OrgUserConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public OrgUserDTO getUserByAccount(String account) {
        if (account == null) {
            return null;
        }
        LambdaQueryWrapper<OrgUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgUser::getAccount, account)
            .last("LIMIT 1");
        OrgUser entity = getOne(wrapper);
        return OrgUserConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public OrgUserDTO getUserByPhone(String phone) {
        if (phone == null) {
            return null;
        }
        LambdaQueryWrapper<OrgUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgUser::getPhone, phone)
            .last("LIMIT 1");
        OrgUser entity = getOne(wrapper);
        return OrgUserConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public OrgUserDTO getUserByAccountAndTenant(String account, Long tenantId) {
        if (account == null || tenantId == null) {
            return null;
        }
        // 绕过租户拦截器后显式按 tenant_id 过滤，避免被调用方上下文租户（默认 0）污染
        IgnoreStrategy ignoreStrategy = IgnoreStrategy.builder().tenantLine(true).build();
        OrgUser entity = InterceptorIgnoreHelper.execute(ignoreStrategy, () -> getOne(
            new LambdaQueryWrapper<OrgUser>()
                .eq(OrgUser::getAccount, account)
                .eq(OrgUser::getTenantId, tenantId)
                .last("LIMIT 1")));
        return OrgUserConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public OrgUserDTO getUserByPhoneAndTenant(String phone, Long tenantId) {
        if (phone == null || tenantId == null) {
            return null;
        }
        IgnoreStrategy ignoreStrategy = IgnoreStrategy.builder().tenantLine(true).build();
        OrgUser entity = InterceptorIgnoreHelper.execute(ignoreStrategy, () -> getOne(
            new LambdaQueryWrapper<OrgUser>()
                .eq(OrgUser::getPhone, phone)
                .eq(OrgUser::getTenantId, tenantId)
                .last("LIMIT 1")));
        return OrgUserConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Long countByTenantId(Long tenantId) {
        if (tenantId == null) {
            return 0L;
        }
        IgnoreStrategy ignoreStrategy = IgnoreStrategy.builder().tenantLine(true).build();
        Long count = InterceptorIgnoreHelper.execute(ignoreStrategy, () -> getBaseMapper().selectCount(
            new LambdaQueryWrapper<OrgUser>()
                .eq(OrgUser::getTenantId, tenantId)));
        return count == null ? 0L : count;
    }

    @Override
    public List<OrgUserDTO> listDtoByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        List<Long> distinctIds = ids.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        return OrgUserConvert.INSTANCE.toDTO(listByIds(distinctIds));
    }

    @Override
    public OrgUserDTO getDtoByIdAndTenant(Serializable id, Long tenantId) {
        Long idValue = Convert.toLong(id);
        if (idValue == null || tenantId == null) {
            return null;
        }
        // 绕过租户拦截器后显式按 tenant_id 过滤，避免无请求上下文时回落到平台租户
        IgnoreStrategy ignoreStrategy = IgnoreStrategy.builder().tenantLine(true).build();
        OrgUser entity = InterceptorIgnoreHelper.execute(ignoreStrategy, () -> getOne(
            new LambdaQueryWrapper<OrgUser>()
                .eq(OrgUser::getId, idValue)
                .eq(OrgUser::getTenantId, tenantId)
                .last("LIMIT 1")));
        return OrgUserConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public List<OrgUserDTO> listDtoByIdsAndTenant(Collection<Long> ids, Long tenantId) {
        if (CollUtil.isEmpty(ids) || tenantId == null) {
            return List.of();
        }
        List<Long> distinctIds = ids.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        // 绕过租户拦截器后显式按 tenant_id 过滤，避免无请求上下文时回落到平台租户
        IgnoreStrategy ignoreStrategy = IgnoreStrategy.builder().tenantLine(true).build();
        List<OrgUser> entities = InterceptorIgnoreHelper.execute(ignoreStrategy, () -> list(
            new LambdaQueryWrapper<OrgUser>()
                .in(OrgUser::getId, distinctIds)
                .eq(OrgUser::getTenantId, tenantId)));
        return OrgUserConvert.INSTANCE.toDTO(entities);
    }

    @Override
    public void updatePassword(Long userId, String encodedPassword) {
        if (userId == null || StrUtil.isBlank(encodedPassword)) {
            log.warn("updatePassword 参数无效: userId={}, encodedPassword 为空? {}",
                userId, encodedPassword == null);
            return;
        }
        // 不可变更新：仅更新密码字段与密码修改时间，避免覆盖其他字段
        OrgUser update = new OrgUser();
        update.setId(userId);
        update.setPwd(encodedPassword);
        update.setPwdLastModify(java.time.LocalDateTime.now());
        boolean success = updateById(update);
        if (!success) {
            log.warn("更新用户密码失败: userId={}", userId);
            return;
        }
        log.info("用户密码已更新（迁移到 Argon2id）: userId={}", userId);
    }

}
