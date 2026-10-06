package com.carlos.org.service;


import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import cn.idev.excel.FastExcel;
import com.carlos.org.config.OrgProperties;
import com.carlos.org.encoder.PasswordEncoderProvider;
import com.carlos.org.exception.*;
import com.carlos.org.listener.OrgUserExcelListener;
import com.carlos.org.manager.*;
import com.carlos.org.pojo.dto.*;
import com.carlos.org.pojo.enums.OrgUserStateEnum;
import com.carlos.org.pojo.excel.OrgUserExcel;
import com.carlos.org.pojo.param.*;
import com.carlos.org.position.manager.OrgPositionHistoryManager;
import com.carlos.org.position.manager.OrgPositionLevelManager;
import com.carlos.org.position.manager.OrgPositionManager;
import com.carlos.org.position.manager.OrgUserPositionManager;
import com.carlos.org.position.pojo.dto.OrgPositionDTO;
import com.carlos.org.position.pojo.dto.OrgPositionHistoryDTO;
import com.carlos.org.position.pojo.dto.OrgPositionLevelDTO;
import com.carlos.org.position.pojo.dto.OrgUserPositionDTO;
import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import com.carlos.util.easyexcel.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * <p>
 * 系统用户 业务
 * </p>
 * <p>实现所有用户管理需求</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgUserService {

    /**
     * 系统操作人兜底 ID（无登录上下文时写入审计历史，与雪花 ID 不会碰撞）
     */
    private static final Long SYSTEM_OPERATOR_ID = 0L;

    /**
     * 系统操作人兜底名称
     */
    private static final String SYSTEM_OPERATOR_NAME = "system";

    private final OrgUserManager userManager;

    private final OrgUserDepartmentManager userDepartmentManager;

    private final OrgUserRoleManager userRoleManager;

    private final OrgUserPositionManager userPositionManager;

    private final OrgDepartmentManager departmentManager;

    private final OrgRoleManager roleManager;

    private final OrgPositionManager positionManager;

    private final OrgPositionLevelManager positionLevelManager;

    private final OrgPositionHistoryManager positionHistoryManager;

    /**
     * 密码加密提供者，允许通过实现 {@link PasswordEncoderProvider} 接口替换默认编码策略。
     * 当 classpath 中存在 {@code PasswordEncoder} Bean 时自动注入，否则为 null。
     */
    private final PasswordEncoderProvider passwordEncoderProvider;

    private final OrgProperties orgProperties;


    /**
     * 新增用户
     * 业务规则：
     * 1. 账号在同一租户内唯一
     * 2. 手机号在同一租户内唯一
     * 3. 密码通过 PasswordEncoderProvider 编码后存储
     *
     * @param dto 系统用户数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void addOrgUser(OrgUserDTO dto) {
        // 校验账号唯一性
        if (StrUtil.isNotBlank(dto.getAccount())) {
            OrgUserDTO existUser = userManager.getUserByAccount(dto.getAccount());
            if (existUser != null) {
                throw new AccountExistsException(dto.getAccount());
            }
        }

        // 校验手机号唯一性
        if (StrUtil.isNotBlank(dto.getPhone())) {
            OrgUserDTO existUser = userManager.getUserByPhone(dto.getPhone());
            if (existUser != null) {
                throw new PhoneExistsException(dto.getPhone());
            }
        }

        // 设置默认状态为启用
        if (dto.getUserStatus() == null) {
            dto.setUserStatus(OrgUserStateEnum.ENABLE);
        }

        // 显式回填租户ID：未指定时取当前登录上下文租户，无上下文（如系统初始化）回落平台租户。
        // 防止超管放行（carlos.org.tenant.super-admin-ignore=true）场景下拦截器默认值缺失导致 tenant_id 写入 NULL。
        if (dto.getTenantId() == null) {
            Long contextTenantId = null;
            UserContext userContext = SecurityUtils.getCurrentUser();
            if (userContext != null) {
                contextTenantId = userContext.getTenantId();
            }
            if (contextTenantId == null) {
                contextTenantId = orgProperties.getTenant().getPlatformTenantId();
            }
            dto.setTenantId(contextTenantId);
        }

        // 密码编码：通过 PasswordEncoderProvider 对明文密码进行编码后存储
        if (StrUtil.isNotBlank(dto.getPwd())) {
            if (passwordEncoderProvider == null) {
                throw new PasswordException("密码编码提供者未配置，请确认 carlos-auth 已正确引入或提供自定义 PasswordEncoderProvider");
            }
            dto.setPwd(passwordEncoderProvider.encode(dto.getPwd()));
        }

        boolean success = userManager.add(dto);
        if (!success) {
            throw new UserOperationException("新增用户失败");
        }
        log.info("新增用户成功：id={}", dto.getId());
    }


    /**
     * 删除用户（逻辑删除）
     *
     * @param ids 系统用户id集合
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrgUser(Set<Long> ids) {
        for (Serializable id : ids) {
            Long userId = (Long) id;

            // 逻辑删除，框架会自动处理is_deleted字段
            boolean success = userManager.removeById(id);
            if (!success) {
                log.warn("删除用户失败：id={}", id);
                continue;
            }

            // 级联删除部门关联
            userDepartmentManager.removeByUserId(userId);

            // 级联删除角色关联
            userRoleManager.removeByUserId(userId);

            // 级联删除岗位关联
            userPositionManager.removeByUserId(userId);
        }
        log.info("删除用户成功：ids={}", ids);
    }


    /**
     * 编辑用户
     *
     * @param dto 对象信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrgUser(OrgUserDTO dto) {
        // 校验用户是否存在
        OrgUserDTO existUser = userManager.getDtoById(dto.getId());
        if (existUser == null) {
            throw new UserNotFoundException(String.valueOf(dto.getId()));
        }

        // 校验账号唯一性（排除自己）
        if (StrUtil.isNotBlank(dto.getAccount()) && !dto.getAccount().equals(existUser.getAccount())) {
            OrgUserDTO accountUser = userManager.getUserByAccount(dto.getAccount());
            if (accountUser != null) {
                throw new AccountExistsException(dto.getAccount());
            }
        }

        // 校验手机号唯一性（排除自己）
        if (StrUtil.isNotBlank(dto.getPhone()) && !dto.getPhone().equals(existUser.getPhone())) {
            OrgUserDTO phoneUser = userManager.getUserByPhone(dto.getPhone());
            if (phoneUser != null) {
                throw new PhoneExistsException(dto.getPhone());
            }
        }

        boolean success = userManager.modify(dto);
        if (!success) {
            throw new UserOperationException("修改用户失败");
        }
        log.info("修改用户成功：id={}", dto.getId());
    }


    /**
     * 启用/禁用用户
     * 切换用户状态
     *
     * @param id    用户id
     * @param state 状态：0禁用，1启用
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeState(Serializable id, OrgUserStateEnum state) {
        if (state != OrgUserStateEnum.DISABLE && state != OrgUserStateEnum.ENABLE) { // Changed from state != 0 && state != 1
            throw new InvalidUserStateException("状态值不正确，只能是0（禁用）或1（启用）");
        }

        OrgUserDTO user = userManager.getDtoById(id);
        if (user == null) {
            throw new UserNotFoundException(String.valueOf(id));
        }

        user.setUserStatus(state);
        boolean success = userManager.modify(user);
        if (!success) {
            throw new UserOperationException("修改用户状态失败");
        }
        log.info("修改用户状态成功：id={}, state={}", id, state);
    }


    /**
     * 解锁用户
     * 将用户状态从锁定(2)改为启用(1)
     *
     * @param id 用户id
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlockOrgUser(Serializable id) {
        OrgUserDTO user = userManager.getDtoById(id);
        if (user == null) {
            throw new UserNotFoundException(String.valueOf(id));
        }
        if (user.getUserStatus() != OrgUserStateEnum.LOCK) {
            throw new InvalidUserStateException("用户状态不是锁定状态，无需解锁");
        }
        user.setUserStatus(OrgUserStateEnum.ENABLE); // 改为启用状态
        boolean success = userManager.modify(user);
        if (!success) {
            throw new UserOperationException("解锁用户失败");
        }
        log.info("解锁用户成功：id={}", id);
    }


    /**
     * 重置密码
     * 管理员重置用户密码
     *
     * @param id     用户ID
     * @param newPwd 新密码（SM2加密后）
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Serializable id, String newPwd) {
        OrgUserDTO user = userManager.getDtoById(id);
        if (user == null) {
            throw new UserNotFoundException(String.valueOf(id));
        }

        // 校验密码复杂度（至少8位，包含大小写字母、数字、特殊字符中的至少3种）
        if (!checkPasswordComplexity(newPwd)) {
            throw new PasswordException("密码复杂度不符合要求：至少8位，包含大小写字母、数字、特殊字符中的至少3种");
        }

        // 密码编码：通过 PasswordEncoderProvider 对明文密码进行编码后存储
        if (passwordEncoderProvider == null) {
            throw new PasswordException("密码编码提供者未配置，请确认 carlos-auth 已正确引入或提供自定义 PasswordEncoderProvider");
        }
        String encodedPwd = passwordEncoderProvider.encode(newPwd);

        user.setPwd(encodedPwd);
        user.setPwdLastModify(LocalDateTime.now());
        boolean success = userManager.modify(user);
        if (!success) {
            throw new UserOperationException("重置密码失败");
        }
        log.info("重置密码成功：id={}", id);
    }


    /**
     * 修改密码（用户自己修改）
     *
     * @param param 修改密码参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(OrgUserChangePwdParam param) {
        OrgUserDTO user = userManager.getDtoById(param.getId());
        if (user == null) {
            throw new UserNotFoundException(param.getId());
        }

        // 校验密码编码提供者已配置
        if (passwordEncoderProvider == null) {
            throw new PasswordException("密码编码提供者未配置，请确认 carlos-auth 已正确引入或提供自定义 PasswordEncoderProvider");
        }

        // 校验旧密码：使用 PasswordEncoderProvider 进行密码匹配校验
        if (!passwordEncoderProvider.matches(param.getOldPwd(), user.getPwd())) {
            throw new PasswordException("旧密码不正确");
        }

        // 校验新密码和确认密码是否一致
        if (!param.getNewPwd().equals(param.getConfirmPwd())) {
            throw new PasswordException("新密码和确认密码不一致");
        }

        // 校验密码复杂度
        if (!checkPasswordComplexity(param.getNewPwd())) {
            throw new PasswordException("密码复杂度不符合要求：至少8位，包含大小写字母、数字、特殊字符中的至少3种");
        }

        // 密码编码：通过 PasswordEncoderProvider 对明文密码进行编码后存储
        String encodedPwd = passwordEncoderProvider.encode(param.getNewPwd());

        user.setPwd(encodedPwd);
        user.setPwdLastModify(LocalDateTime.now());
        boolean success = userManager.modify(user);
        if (!success) {
            throw new UserOperationException("修改密码失败");
        }
        log.info("修改密码成功：id={}", param.getId());
    }


    /**
     * 用户导入
     * Excel批量导入用户
     *
     * @param file Excel文件
     * @return 导入成功的用户ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Serializable> importUser(MultipartFile file) {
        List<Serializable> successIds = new ArrayList<>();

        try {
            ExcelUtil.checkExcel(FileNameUtil.getName(file.getOriginalFilename()));
            OrgUserExcelListener listener = new OrgUserExcelListener();
            FastExcel.read(file.getInputStream(), OrgUserExcel.class, listener).sheet().doRead();

            List<OrgUserExcel> dataList = listener.getDataList();
            if (dataList.isEmpty()) {
                return successIds;
            }

            List<OrgUserDTO> userDtoList = new ArrayList<>();
            for (OrgUserExcel excel : dataList) {
                // 基础校验
                if (StrUtil.isBlank(excel.getAccount())) {
                    continue;
                }
                // 账号唯一性校验
                OrgUserDTO existUser = userManager.getUserByAccount(excel.getAccount());
                if (existUser != null) {
                    continue;
                }
                // 手机号唯一性校验
                if (StrUtil.isNotBlank(excel.getPhone())) {
                    OrgUserDTO phoneUser = userManager.getUserByPhone(excel.getPhone());
                    if (phoneUser != null) {
                        continue;
                    }
                }

                OrgUserDTO dto = new OrgUserDTO();
                dto.setAccount(excel.getAccount());
                dto.setNickname(StrUtil.isNotBlank(excel.getNickname()) ? excel.getNickname() : excel.getAccount());
                dto.setPhone(excel.getPhone());
                dto.setEmail(excel.getEmail());
                dto.setUserStatus(OrgUserStateEnum.ENABLE);
                userDtoList.add(dto);
            }

            // 批量保存
            for (OrgUserDTO dto : userDtoList) {
                boolean success = userManager.add(dto);
                if (success) {
                    successIds.add(dto.getId());
                }
            }

            log.info("用户导入完成，成功 {} 条，失败 {} 条", successIds.size(), dataList.size() - successIds.size());
        } catch (IOException e) {
            throw new UserOperationException("导入异常：" + e.getMessage());
        }

        return successIds;
    }


    /**
     * 用户导出
     * 导出用户列表到Excel
     *
     * @param param    查询参数
     * @param response HTTP响应
     */
    public void exportUser(OrgUserPageParam param, HttpServletResponse response) {
        List<OrgUserDTO> userList = userManager.listByParam(param);

        List<OrgUserExcel> excelList = new ArrayList<>();
        for (OrgUserDTO user : userList) {
            OrgUserExcel excel = new OrgUserExcel();
            excel.setAccount(user.getAccount());
            excel.setNickname(user.getNickname());
            excel.setPhone(user.getPhone());
            excel.setEmail(user.getEmail());
            excelList.add(excel);
        }

        String fileName = "用户导出-" + System.currentTimeMillis();
        ExcelUtil.download(response, fileName, OrgUserExcel.class, excelList);
        log.info("用户导出完成，共 {} 条", excelList.size());
    }


    /**
     * 下载用户导入模板
     * 生成只有表头、没有数据的 Excel 模板文件
     *
     * @param response HTTP响应
     */
    public void downloadImportTemplate(HttpServletResponse response) {
        String fileName = "用户导入模板";
        ExcelUtil.download(response, fileName, OrgUserExcel.class, new ArrayList<>());
        log.info("用户导入模板下载完成");
    }


    /**
     * 用户导入预览
     * 解析 Excel 并逐行校验，返回带 error 信息的预览结果
     *
     * @param file Excel文件
     * @return 预览数据列表（含每行校验结果）
     */
    public List<OrgUserExcel> previewImportUser(MultipartFile file) {
        List<OrgUserExcel> resultList = new ArrayList<>();

        try {
            ExcelUtil.checkExcel(FileNameUtil.getName(file.getOriginalFilename()));
            OrgUserExcelListener listener = new OrgUserExcelListener();
            FastExcel.read(file.getInputStream(), OrgUserExcel.class, listener).sheet().doRead();

            List<OrgUserExcel> dataList = listener.getDataList();
            if (dataList.isEmpty()) {
                return resultList;
            }

            for (OrgUserExcel excel : dataList) {
                List<String> errors = new ArrayList<>();

                // 账号非空校验
                if (StrUtil.isBlank(excel.getAccount())) {
                    errors.add("账号不能为空");
                } else {
                    // 账号唯一性校验
                    OrgUserDTO existUser = userManager.getUserByAccount(excel.getAccount());
                    if (existUser != null) {
                        errors.add("账号已存在");
                    }
                }

                // 手机号唯一性校验
                if (StrUtil.isNotBlank(excel.getPhone())) {
                    OrgUserDTO phoneUser = userManager.getUserByPhone(excel.getPhone());
                    if (phoneUser != null) {
                        errors.add("手机号已存在");
                    }
                }

                if (!errors.isEmpty()) {
                    excel.setError(String.join("；", errors));
                }
                resultList.add(excel);
            }

            log.info("用户导入预览完成，共 {} 条，通过 {} 条，失败 {} 条",
                resultList.size(),
                resultList.stream().filter(e -> StrUtil.isBlank(e.getError())).count(),
                resultList.stream().filter(e -> StrUtil.isNotBlank(e.getError())).count());
        } catch (IOException e) {
            throw new UserOperationException("导入预览异常：" + e.getMessage());
        }

        return resultList;
    }


    /**
     * 用户导入确认写入
     * 接收预览后的数据，将无错误记录批量写入数据库
     *
     * @param previewList 预览数据列表
     * @return 成功导入的用户ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Serializable> confirmImportUser(List<OrgUserExcel> previewList) {
        List<Serializable> successIds = new ArrayList<>();

        if (previewList == null || previewList.isEmpty()) {
            return successIds;
        }

        List<OrgUserDTO> userDtoList = new ArrayList<>();
        for (OrgUserExcel excel : previewList) {
            // 跳过有错误的记录
            if (StrUtil.isNotBlank(excel.getError())) {
                continue;
            }
            // 跳过必填项缺失的记录（兜底校验）
            if (StrUtil.isBlank(excel.getAccount())) {
                continue;
            }

            OrgUserDTO dto = new OrgUserDTO();
            dto.setAccount(excel.getAccount());
            dto.setNickname(StrUtil.isNotBlank(excel.getNickname()) ? excel.getNickname() : excel.getAccount());
            dto.setPhone(excel.getPhone());
            dto.setEmail(excel.getEmail());
            dto.setUserStatus(OrgUserStateEnum.ENABLE);
            userDtoList.add(dto);
        }

        // 批量保存
        for (OrgUserDTO dto : userDtoList) {
            boolean success = userManager.add(dto);
            if (success) {
                successIds.add(dto.getId());
            }
        }

        log.info("用户导入确认写入完成，成功 {} 条，失败 {} 条", successIds.size(), userDtoList.size() - successIds.size());
        return successIds;
    }


    /**
     * 用户分配部门
     * 设置用户主部门及兼职部门
     *
     * @param param 分配部门参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignDepartments(OrgUserAssignDeptParam param) {
        // 校验用户是否存在
        OrgUserDTO user = userManager.getDtoById(param.getUserId());
        if (user == null) {
            throw new UserNotFoundException(param.getUserId());
        }

        Long userId = param.getUserId();

        // 1. 删除原有部门关联
        userDepartmentManager.removeByUserId(userId);

        // 2. 插入新的部门关联
        if (param.getDepartmentIds() != null && !param.getDepartmentIds().isEmpty()) {
            Long mainDeptId = param.getMainDeptId();

            // 校验主部门是否在所选部门列表中
            if (mainDeptId != null && !param.getDepartmentIds().contains(mainDeptId)) {
                throw new UserOperationException("主部门必须在所选部门列表中");
            }

            for (Long deptId : param.getDepartmentIds()) {
                OrgUserDepartmentDTO relation = new OrgUserDepartmentDTO();
                relation.setUserId(userId);
                relation.setDeptId(deptId);
                relation.setMainDept(mainDeptId != null && mainDeptId.equals(deptId));
                userDepartmentManager.add(relation);
            }
        }

        // 3. 更新主部门ID
        if (param.getMainDeptId() != null) {
            user.setMainDeptId(param.getMainDeptId());
            userManager.modify(user);
        } else {
            // 清空主部门
            user.setMainDeptId(null);
            userManager.modify(user);
        }

        log.info("用户分配部门成功：userId={}, departments={}", param.getUserId(), param.getDepartmentIds());
    }


    /**
     * 用户分配角色
     * 为用户分配角色
     *
     * @param param 分配角色参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(OrgUserAssignRoleParam param) {
        // 校验用户是否存在
        OrgUserDTO user = userManager.getDtoById(param.getUserId());
        if (user == null) {
            throw new UserNotFoundException(param.getUserId());
        }

        Long userId = param.getUserId();

        // 1. 删除原有角色关联
        userRoleManager.removeByUserId(userId);

        // 2. 插入新的角色关联（expireTime 非空即临时授权，对本次角色列表统一生效）
        if (param.getRoleIds() != null && !param.getRoleIds().isEmpty()) {
            for (Long roleId : param.getRoleIds()) {
                OrgUserRoleDTO relation = new OrgUserRoleDTO();
                relation.setUserId(userId);
                relation.setRoleId(roleId);
                relation.setExpireTime(param.getExpireTime());
                userRoleManager.add(relation);
            }
        }

        log.info("用户分配角色成功：userId={}, roles={}, expireTime={}",
            param.getUserId(), param.getRoleIds(), param.getExpireTime());
    }


    /**
     * 用户分配岗位
     * 设置用户主岗位及兼岗
     *
     * @param param 分配岗位参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPositions(OrgUserAssignPositionParam param) {
        // 校验用户是否存在
        OrgUserDTO user = userManager.getDtoById(param.getUserId());
        if (user == null) {
            throw new UserNotFoundException(param.getUserId());
        }

        Long userId = param.getUserId();
        Long positionId = param.getPositionId();

        // 1. 如果设置为主岗位，需要将该用户其他岗位设为非主岗位
        if (Boolean.TRUE.equals(param.getIsMain())) {
            List<OrgUserPositionDTO> mainPositions = userPositionManager.listMainByUserId(userId);
            if (!mainPositions.isEmpty()) {
                for (OrgUserPositionDTO mp : mainPositions) {
                    mp.setMain(false);
                    userPositionManager.modify(mp);
                }
            }
        }

        // 2. 查询是否已存在该用户的该岗位关联
        OrgUserPositionDTO exist = userPositionManager.getByUserIdAndPositionId(userId, positionId);

        // 编制校验：新增关联时检查是否超编
        if (exist == null) {
            OrgPositionDTO position = positionManager.getDtoById(positionId);
            if (position != null && position.getHeadcount() != null && position.getHeadcount() > 0) {
                int occupied = userPositionManager.countOccupiedByPositionId(positionId);
                if (occupied >= position.getHeadcount()) {
                    throw new UserOperationException("该岗位编制已满，无法分配");
                }
            }
        }

        if (exist != null) {
            // 更新已有关联
            exist.setLevelId(param.getLevelId());
            exist.setDepartmentId(param.getDepartmentId());
            exist.setMain(param.getIsMain());
            if (param.getAppointDate() != null) {
                exist.setAppointDate(param.getAppointDate());
            }
            userPositionManager.modify(exist);
        } else {
            // 插入新关联
            OrgUserPositionDTO relation = new OrgUserPositionDTO();
            relation.setUserId(userId);
            relation.setPositionId(positionId);
            relation.setLevelId(param.getLevelId());
            relation.setDepartmentId(param.getDepartmentId());
            relation.setMain(param.getIsMain());
            relation.setPositionStatus(1); // 在职
            relation.setAppointDate(param.getAppointDate() != null ? param.getAppointDate() : LocalDate.now());
            userPositionManager.add(relation);
        }

        // 刷新岗位统计
        refreshPositionStats(positionId);

        // 记录岗位变更历史(change_reason/operator_id/operator_name 为 NOT NULL 列, 必须赋值)
        OrgPositionHistoryDTO history = new OrgPositionHistoryDTO();
        history.setUserId(userId);
        history.setChangeType(exist != null ? 4 : 1); // 4转岗, 1入职
        history.setOldPositionId(exist != null ? exist.getPositionId() : null);
        history.setNewPositionId(positionId);
        history.setOldLevelId(exist != null ? exist.getLevelId() : null);
        history.setNewLevelId(param.getLevelId());
        history.setOldDeptId(exist != null ? exist.getDepartmentId() : null);
        history.setNewDeptId(param.getDepartmentId());
        history.setChangeDate(LocalDate.now());
        history.setChangeReason(exist != null ? "转岗调整" : "入职任命");
        UserContext operator = SecurityUtils.getCurrentUserOpt().orElse(null);
        history.setOperatorId(operator != null && operator.getUserId() != null
            ? operator.getUserId() : SYSTEM_OPERATOR_ID);
        history.setOperatorName(operator != null && StrUtil.isNotBlank(operator.getUsername())
            ? operator.getUsername() : SYSTEM_OPERATOR_NAME);
        positionHistoryManager.add(history);

        log.info("用户分配岗位成功：userId={}, positionId={}", param.getUserId(), param.getPositionId());
    }


    /**
     * 获取用户部门列表
     *
     * @param userId 用户ID
     * @return 部门DTO列表
     */
    public List<OrgUserDeptDTO> getUserDepartments(Serializable userId) {
        List<OrgUserDepartmentDTO> relations = userDepartmentManager.listByUserId((Long) userId);

        if (relations.isEmpty()) {
            return new ArrayList<>();
        }

        return relations.stream().map(relation -> {
            OrgUserDeptDTO dto = new OrgUserDeptDTO();
            dto.setDepartmentId(relation.getDeptId());
            dto.setIsMain(relation.getMainDept());
            // 补全部门名称
            OrgDepartmentDTO dept = departmentManager.getDtoById(relation.getDeptId());
            if (dept != null) {
                dto.setDepartmentName(dept.getDeptName());
            }
            return dto;
        }).collect(Collectors.toList());
    }


    /**
     * 获取用户角色列表
     *
     * @param userId 用户ID
     * @return 角色DTO列表
     */
    public List<OrgUserRoleDTO> getUserRoles(Serializable userId) {
        // 管理面语义: 展示全部授权(含已过期), 便于审计与续期; 生效判定走 listByUserId
        List<OrgUserRoleDTO> relations = userRoleManager.listAllByUserId((Long) userId);

        if (relations.isEmpty()) {
            return new ArrayList<>();
        }

        return relations.stream().map(relation -> {
            OrgUserRoleDTO dto = new OrgUserRoleDTO();
            dto.setId(relation.getId());
            dto.setUserId(relation.getUserId());
            dto.setRoleId(relation.getRoleId());
            dto.setDeptId(relation.getDeptId());
            dto.setExpireTime(relation.getExpireTime());
            dto.setCreateBy(relation.getCreateBy());
            dto.setCreateTime(relation.getCreateTime());
            dto.setTenantId(relation.getTenantId());

            // 补全角色名称与编码
            // 关联表只存 role_id，而 OrgUserRoleVO 声明了 roleName / roleCode，
            // 不补的话这两个字段恒为 null（与 getUserPositions 的做法保持一致）
            if (relation.getRoleId() != null) {
                OrgRoleDTO role = roleManager.getDtoById(relation.getRoleId());
                if (role != null) {
                    dto.setRoleName(role.getRoleName());
                    dto.setRoleCode(role.getRoleCode());
                }
            }

            return dto;
        }).collect(Collectors.toList());
    }


    /**
     * 获取用户岗位列表
     *
     * @param userId 用户ID
     * @return 岗位DTO列表
     */
    public List<OrgUserPositionDTO> getUserPositions(Serializable userId) {
        List<OrgUserPositionDTO> relations = userPositionManager.listByUserId((Long) userId);

        if (relations.isEmpty()) {
            return new ArrayList<>();
        }

        return relations.stream().map(relation -> {
            OrgUserPositionDTO dto = new OrgUserPositionDTO();
            dto.setPositionId(relation.getPositionId());
            dto.setLevelId(relation.getLevelId());
            dto.setDepartmentId(relation.getDepartmentId());
            dto.setMain(relation.getMain());
            dto.setPositionStatus(relation.getPositionStatus());
            dto.setAppointDate(relation.getAppointDate());

            // 补全岗位名称
            if (relation.getPositionId() != null) {
                OrgPositionDTO position = positionManager.getDtoById(relation.getPositionId());
                if (position != null) {
                    dto.setPositionName(position.getPositionName());
                    dto.setPositionCode(position.getPositionCode());
                }
            }

            // 补全职级名称
            if (relation.getLevelId() != null) {
                OrgPositionLevelDTO level = positionLevelManager.getDtoById(relation.getLevelId());
                if (level != null) {
                    dto.setLevelName(level.getLevelName());
                }
            }

            // 补全部门名称
            if (relation.getDepartmentId() != null) {
                OrgDepartmentDTO dept = departmentManager.getDtoById(relation.getDepartmentId());
                if (dept != null) {
                    dto.setDepartmentName(dept.getDeptName());
                }
            }

            return dto;
        }).collect(Collectors.toList());
    }


    /**
     * 刷新岗位统计（当前在岗人数、空缺人数）
     *
     * @param positionId 岗位ID
     */
    private void refreshPositionStats(Serializable positionId) {
        int occupied = userPositionManager.countOccupiedByPositionId(positionId);
        OrgPositionDTO position = positionManager.getDtoById(positionId);
        if (position != null) {
            position.setCurrentCount(occupied);
            int headcount = position.getHeadcount() != null ? position.getHeadcount() : 0;
            position.setVacancyCount(headcount > 0 ? Math.max(0, headcount - occupied) : 0);
            positionManager.modify(position);
        }
    }


    /**
     * 校验密码复杂度
     * 规则：至少8位，包含大小写字母、数字、特殊字符中的至少3种
     *
     * @param password 密码（已SM2解密后的明文或加密后的密文，这里简单校验长度）
     * @return 是否符合复杂度
     */
    public boolean checkPasswordComplexity(String password) {
        if (StrUtil.isBlank(password) || password.length() < 8) {
            return false;
        }
        int typeCount = 0;
        if (password.matches(".*[a-z].*")) {
            typeCount++;
        }
        if (password.matches(".*[A-Z].*")) {
            typeCount++;
        }
        if (password.matches(".*\\d.*")) {
            typeCount++;
        }
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            typeCount++;
        }
        return typeCount >= 3;
    }

}
