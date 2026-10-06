package com.carlos.integration.module.dingtalk.service;

import com.carlos.integration.module.dingtalk.api.DingtalkApiClient;
import com.carlos.integration.module.dingtalk.api.dto.*;
import com.carlos.integration.module.dingtalk.config.DingtalkAccessTokenManager;
import com.carlos.integration.module.dingtalk.config.DingtalkProperties;
import com.carlos.integration.module.dingtalk.exception.DockingDingtalkException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * <p>
 * 钉钉服务 - RestClient 实现
 * 基于 Spring RestClient 的全新实
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DingtalkRestClientService {

    private final DingtalkApiClient apiClient;
    private final DingtalkAccessTokenManager tokenManager;
    private final DingtalkProperties properties;

    // ==================== Token 相关 ====================

    /**
     * 获取 AccessToken
     */
    public String getToken() {
        return tokenManager.getAccessToken();
    }

    // ==================== 用户相关 ====================

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public DingtalkUserResponse.DingtalkUser getUserInfo(String userId) {
        DingtalkUserRequest request = new DingtalkUserRequest();
        request.setUserid(userId);

        DingtalkUserResponse response = apiClient.getUser(getToken(), request);
        checkResponse(response, "获取用户信息失败");
        return response.getResult();
    }

    /**
     * 根据 code 获取用户ID
     *
     * @param code 临时授权
     * @return 用户信息
     */
    public DingtalkUserInfoResponse.UserGetByCodeResponse getUserId(String code) {
        DingtalkCodeRequest request = new DingtalkCodeRequest(code);
        DingtalkUserInfoResponse response = apiClient.getUserId(getToken(), request);
        checkResponse(response, "获取用户ID失败");
        return response.getResult();
    }

    /**
     * 根据手机号获取用
     *
     * @param mobile 手机
     * @return 用户信息
     */
    public DingtalkUserByMobileResponse.UserGetByMobileResponse getUserByMobile(String mobile) {
        DingtalkMobileRequest request = new DingtalkMobileRequest(mobile);
        DingtalkUserByMobileResponse response = apiClient.getUserByMobile(getToken(), request);
        checkResponse(response, "根据手机号获取用户失败");
        return response.getResult();
    }

    /**
     * 获取部门用户列表
     *
     * @param deptId 部门ID
     * @param cursor 分页游标
     * @param size   分页大小
     * @return 用户列表分页结果
     */
    public DingtalkUserListResponse.PageResult listUsers(Long deptId, Long cursor, Long size) {
        DingtalkUserListRequest request = new DingtalkUserListRequest();
        request.setDeptId(deptId);
        request.setCursor(cursor);
        request.setSize(size);

        DingtalkUserListResponse response = apiClient.listUsers(getToken(), request);
        checkResponse(response, "获取部门用户列表失败");
        return response.getResult();
    }

    /**
     * 创建用户
     *
     * @param request 用户信息
     * @return 创建结果
     */
    public DingtalkUserCreateResponse.UserCreateResponse createUser(DingtalkUserCreateRequest request) {
        DingtalkUserCreateResponse response = apiClient.createUser(getToken(), request);
        checkResponse(response, "创建用户失败");
        return response.getResult();
    }

    /**
     * 更新用户
     *
     * @param request 用户信息
     */
    public void updateUser(DingtalkUserUpdateRequest request) {
        DingtalkUserUpdateResponse response = apiClient.updateUser(getToken(), request);
        checkResponse(response, "更新用户失败");
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     */
    public void deleteUser(String userId) {
        DingtalkUserDeleteRequest request = new DingtalkUserDeleteRequest(userId);
        DingtalkUserDeleteResponse response = apiClient.deleteUser(getToken(), request);
        checkResponse(response, "删除用户失败");
    }

    // ==================== 部门相关 ====================

    /**
     * 获取部门详情
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    public DingtalkDeptResponse.DeptGetResponse getDeptInfo(Long deptId) {
        DingtalkDeptRequest request = new DingtalkDeptRequest();
        request.setDeptId(deptId);

        DingtalkDeptResponse response = apiClient.getDepartment(getToken(), request);
        checkResponse(response, "获取部门详情失败");
        return response.getResult();
    }

    /**
     * 获取子部门列
     *
     * @param deptId 父部门ID
     * @return 子部门列
     */
    public List<DingtalkDeptListResponse.DeptBaseResponse> listSubDepartments(Long deptId) {
        DingtalkDeptListRequest request = new DingtalkDeptListRequest();
        request.setDeptId(deptId);

        DingtalkDeptListResponse response = apiClient.listSubDepartments(getToken(), request);
        checkResponse(response, "获取子部门列表失");
        return response.getResult();
    }

    /**
     * 创建部门
     *
     * @param request 部门信息
     * @return 创建结果
     */
    public DingtalkDeptCreateResponse.DeptCreateResponse createDepartment(DingtalkDeptCreateRequest request) {
        DingtalkDeptCreateResponse response = apiClient.createDepartment(getToken(), request);
        checkResponse(response, "创建部门失败");
        return response.getResult();
    }

    /**
     * 更新部门
     *
     * @param request 部门信息
     */
    public void updateDepartment(DingtalkDeptUpdateRequest request) {
        DingtalkDeptUpdateResponse response = apiClient.updateDepartment(getToken(), request);
        checkResponse(response, "更新部门失败");
    }

    /**
     * 删除部门
     *
     * @param deptId 部门ID
     */
    public void deleteDepartment(Long deptId) {
        DingtalkDeptDeleteRequest request = new DingtalkDeptDeleteRequest();
        request.setDeptId(deptId);

        DingtalkDeptDeleteResponse response = apiClient.deleteDepartment(getToken(), request);
        checkResponse(response, "删除部门失败");
    }

    // ==================== 消息相关 ====================

    /**
     * 发送工作通知消息
     *
     * @param request 消息请求
     * @return 发送结
     */
    public DingtalkMessageResponse.AsyncSendMessageResponse sendMessage(DingtalkMessageRequest request) {
        request.setAgentId(Long.valueOf(properties.getAgentId()));

        DingtalkMessageResponse response = apiClient.sendMessage(getToken(), request);
        checkResponse(response, "发送消息失");
        return response.getResult();
    }

    // ==================== 私有方法 ====================

    /**
     * 检查响应结
     */
    private void checkResponse(DingtalkBaseResponse response, String errorMsg) {
        if (response == null) {
            throw new DockingDingtalkException(errorMsg + "：响应为");
        }
        if (!response.isSuccess()) {
            log.error("{}，错误码：{}，错误信息：{}", errorMsg, response.getErrcode(), response.getErrmsg());
            throw new DockingDingtalkException(errorMsg + response.getErrmsg());
        }
    }
}
