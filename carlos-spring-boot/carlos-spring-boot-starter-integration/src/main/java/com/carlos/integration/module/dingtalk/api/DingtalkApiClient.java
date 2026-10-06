package com.carlos.integration.module.dingtalk.api;

import com.carlos.integration.module.dingtalk.api.dto.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * <p>
 * 钉钉 API 客户端接
 * 基于 Spring RestClient @HttpExchange 声明式定
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@HttpExchange(url = "/", contentType = "application/json")
public interface DingtalkApiClient {

    // ==================== 认证相关 ====================

    /**
     * 获取 AccessToken
     *
     * @param appkey    应用 key
     * @param appsecret 应用密钥
     * @return Token 响应
     */
    @GetExchange("gettoken")
    DingtalkTokenResponse getToken(@RequestParam("appkey") String appkey,
                                   @RequestParam("appsecret") String appsecret);

    // ==================== 用户相关 ====================

    /**
     * 获取用户信息
     *
     * @param accessToken AccessToken
     * @param userid      用户ID
     * @return 用户信息
     */
    @PostExchange("topapi/v2/user/get")
    DingtalkUserResponse getUser(@RequestParam("access_token") String accessToken,
                                 @RequestBody DingtalkUserRequest request);

    /**
     * 根据 code 获取用户ID
     *
     * @param accessToken AccessToken
     * @param code        临时授权
     * @return 用户信息
     */
    @PostExchange("topapi/v2/user/getuserinfo")
    DingtalkUserInfoResponse getUserId(@RequestParam("access_token") String accessToken,
                                       @RequestBody DingtalkCodeRequest request);

    /**
     * 根据手机号获取用
     *
     * @param accessToken AccessToken
     * @param mobile      手机
     * @return 用户信息
     */
    @PostExchange("topapi/v2/user/getbymobile")
    DingtalkUserByMobileResponse getUserByMobile(@RequestParam("access_token") String accessToken,
                                                 @RequestBody DingtalkMobileRequest request);

    /**
     * 获取部门用户列表
     *
     * @param accessToken AccessToken
     * @param request     请求参数
     * @return 用户列表
     */
    @PostExchange("topapi/v2/user/list")
    DingtalkUserListResponse listUsers(@RequestParam("access_token") String accessToken,
                                       @RequestBody DingtalkUserListRequest request);

    /**
     * 创建用户
     *
     * @param accessToken AccessToken
     * @param request     用户信息
     * @return 创建结果
     */
    @PostExchange("topapi/v2/user/create")
    DingtalkUserCreateResponse createUser(@RequestParam("access_token") String accessToken,
                                          @RequestBody DingtalkUserCreateRequest request);

    /**
     * 更新用户
     *
     * @param accessToken AccessToken
     * @param request     用户信息
     * @return 更新结果
     */
    @PostExchange("topapi/v2/user/update")
    DingtalkUserUpdateResponse updateUser(@RequestParam("access_token") String accessToken,
                                          @RequestBody DingtalkUserUpdateRequest request);

    /**
     * 删除用户
     *
     * @param accessToken AccessToken
     * @param request     删除参数
     * @return 删除结果
     */
    @PostExchange("topapi/v2/user/delete")
    DingtalkUserDeleteResponse deleteUser(@RequestParam("access_token") String accessToken,
                                          @RequestBody DingtalkUserDeleteRequest request);

    // ==================== 部门相关 ====================

    /**
     * 获取部门详情
     *
     * @param accessToken AccessToken
     * @param request     请求参数
     * @return 部门信息
     */
    @PostExchange("topapi/v2/department/get")
    DingtalkDeptResponse getDepartment(@RequestParam("access_token") String accessToken,
                                       @RequestBody DingtalkDeptRequest request);

    /**
     * 获取子部门列
     *
     * @param accessToken AccessToken
     * @param request     请求参数
     * @return 部门列表
     */
    @PostExchange("topapi/v2/department/listsub")
    DingtalkDeptListResponse listSubDepartments(@RequestParam("access_token") String accessToken,
                                                @RequestBody DingtalkDeptListRequest request);

    /**
     * 创建部门
     *
     * @param accessToken AccessToken
     * @param request     部门信息
     * @return 创建结果
     */
    @PostExchange("topapi/v2/department/create")
    DingtalkDeptCreateResponse createDepartment(@RequestParam("access_token") String accessToken,
                                                @RequestBody DingtalkDeptCreateRequest request);

    /**
     * 更新部门
     *
     * @param accessToken AccessToken
     * @param request     部门信息
     * @return 更新结果
     */
    @PostExchange("topapi/v2/department/update")
    DingtalkDeptUpdateResponse updateDepartment(@RequestParam("access_token") String accessToken,
                                                @RequestBody DingtalkDeptUpdateRequest request);

    /**
     * 删除部门
     *
     * @param accessToken AccessToken
     * @param request     删除参数
     * @return 删除结果
     */
    @PostExchange("topapi/v2/department/delete")
    DingtalkDeptDeleteResponse deleteDepartment(@RequestParam("access_token") String accessToken,
                                                @RequestBody DingtalkDeptDeleteRequest request);

    // ==================== 消息相关 ====================

    /**
     * 发送工作通知消息
     *
     * @param accessToken AccessToken
     * @param request     消息内容
     * @return 发送结
     */
    @PostExchange("topapi/message/corpconversation/asyncsend_v2")
    DingtalkMessageResponse sendMessage(@RequestParam("access_token") String accessToken,
                                        @RequestBody DingtalkMessageRequest request);
}
