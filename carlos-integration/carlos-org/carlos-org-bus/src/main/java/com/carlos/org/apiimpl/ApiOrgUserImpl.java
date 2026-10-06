package com.carlos.org.apiimpl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.api.pojo.ao.OrgUserCandidateAO;
import com.carlos.org.api.pojo.param.ApiOrgUserChangeStateParam;
import com.carlos.org.convert.OrgUserConvert;
import com.carlos.org.manager.OrgUserManager;
import com.carlos.org.pojo.dto.OrgUserCandidateDTO;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.enums.OrgUserStateEnum;
import com.carlos.org.pojo.param.OrgUserCandidatePageParam;
import com.carlos.org.pojo.param.OrgUserPageParam;
import com.carlos.org.pojo.vo.OrgUserVO;
import com.carlos.org.service.OrgUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 系统用户 api接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/user")
@Tag(name = "系统用户Feign接口")
public class ApiOrgUserImpl implements ApiOrgUser {

    private final OrgUserManager orgUserManager;
    private final OrgUserService orgUserService;


    @Override
    public Result<OrgUserAO> getUserByIdentifier(String identifier) {
        if (StrUtil.isBlank(identifier)) {
            return Result.success(null);
        }

        OrgUserDTO user = orgUserManager.getUserByAccount(identifier);
        if (user == null) {
            user = orgUserManager.getUserByPhone(identifier);
        }

        return Result.success(OrgUserConvert.INSTANCE.toAO(user));
    }

    @Override
    public Result<OrgUserAO> getUserByIdentifierAndTenant(String identifier, Long tenantId) {
        if (StrUtil.isBlank(identifier) || tenantId == null) {
            return Result.success(null);
        }

        OrgUserDTO user = orgUserManager.getUserByAccountAndTenant(identifier, tenantId);
        if (user == null) {
            user = orgUserManager.getUserByPhoneAndTenant(identifier, tenantId);
        }

        return Result.success(OrgUserConvert.INSTANCE.toAO(user));
    }

    @Override
    public Result<OrgUserAO> getUserById(Long id) {
        if (id == null) {
            return Result.success(null);
        }
        OrgUserDTO user = orgUserManager.getDtoById(id);
        return Result.success(OrgUserConvert.INSTANCE.toAO(user));
    }

    @Override
    public Result<OrgUserAO> getUserByIdAndTenant(Long id, Long tenantId) {
        if (id == null || tenantId == null) {
            return Result.success(null);
        }
        OrgUserDTO user = orgUserManager.getDtoByIdAndTenant(id, tenantId);
        return Result.success(OrgUserConvert.INSTANCE.toAO(user));
    }

    @Override
    public Result<List<OrgUserAO>> listUsersByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Result.success(List.of());
        }
        List<OrgUserDTO> users = orgUserManager.listDtoByIds(ids);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

    @Override
    public Result<List<OrgUserAO>> listUsersByIdsAndTenant(Collection<Long> ids, Long tenantId) {
        if (CollUtil.isEmpty(ids) || tenantId == null) {
            return Result.success(List.of());
        }
        List<OrgUserDTO> users = orgUserManager.listDtoByIdsAndTenant(ids, tenantId);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

    @Override
    public Result<Paging<OrgUserCandidateAO>> pageCandidates(Long deptId, Collection<Long> includeIds,
                                                             Collection<Long> excludeIds, int current, int size) {
        OrgUserCandidatePageParam param = new OrgUserCandidatePageParam();
        param.setDeptId(deptId);
        param.setIncludeIds(includeIds);
        param.setExcludeIds(excludeIds);
        param.setCurrent(current);
        param.setSize(size);
        Paging<OrgUserCandidateDTO> paging = orgUserManager.getCandidatePage(param);
        Paging<OrgUserCandidateAO> result = new Paging<>();
        result.setTotal(paging.getTotal());
        result.setCurrent(paging.getCurrent());
        result.setSize(paging.getSize());
        result.setPages(paging.getPages());
        result.setRecords(OrgUserConvert.INSTANCE.toCandidateAOList(paging.getRecords()));
        return Result.success(result);
    }

    @Override
    public Result<Paging<OrgUserAO>> pageUserOptions(String account, String nickname,
                                                     Integer pageNum, Integer pageSize) {
        OrgUserPageParam param = new OrgUserPageParam();
        param.setAccount(account);
        param.setNickname(nickname);
        // 0 是「未指定」的哨兵：ParamPage 的 setter 会把非正数归一成默认值（页码 1 / 每页 10）
        param.setCurrent(pageNum == null ? 0 : pageNum);
        param.setSize(pageSize == null ? 0 : pageSize);
        Paging<OrgUserVO> paging = orgUserManager.getPage(param);
        Paging<OrgUserAO> result = new Paging<>();
        result.setTotal(paging.getTotal());
        result.setCurrent(paging.getCurrent());
        result.setSize(paging.getSize());
        result.setPages(paging.getPages());
        // 由 VO 而非 DTO 映射：VO 不含 pwd，密码哈希不会被带到接口响应里
        result.setRecords(OrgUserConvert.INSTANCE.toAOListFromVo(paging.getRecords()));
        return Result.success(result);
    }

    @Override
    public void changeState(ApiOrgUserChangeStateParam param) {
        if (param == null || param.getId() == null || param.getUserStatus() == null) {
            return;
        }
        OrgUserStateEnum state = null;
        for (OrgUserStateEnum e : OrgUserStateEnum.values()) {
            if (e.getCode().equals(param.getUserStatus())) {
                state = e;
                break;
            }
        }
        if (state == null) {
            return;
        }
        orgUserService.changeState(param.getId(), state);
    }

    @Override
    public Result<Void> updatePassword(Long userId, String encodedPassword) {
        if (userId == null || StrUtil.isBlank(encodedPassword)) {
            return Result.error("参数不能为空");
        }
        orgUserManager.updatePassword(userId, encodedPassword);
        return Result.success();
    }

}
