package com.carlos.integration.module.dingtalk.support;

import com.carlos.integration.module.dingtalk.service.DingtalkService;
import com.dingtalk.api.request.*;
import com.dingtalk.api.response.*;
import com.dingtalk.api.response.OapiV2UserGetuserinfoResponse.UserGetByCodeResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * <p>
 * 閽夐拤宸ュ叿绫?
 * </p>
 *
 * @author Carlos
 * @date 2023/4/6 9:39
 */
@Slf4j
public class DingtalkUtil {

    private static DingtalkService service;


    public DingtalkUtil(DingtalkService service) {
        DingtalkUtil.service = service;
    }

    /**
     * getUserId
     *
     * @param code 鍙傛暟0
     * @return java.lang.String
     * @author Carlos
     * @date 2023/6/30 15:27
     */
    public static UserGetByCodeResponse getUserId(String code) {
        return service.getUserId(code);
    }

    /**
     * 鑾峰彇鐢ㄦ埛淇℃伅
     *
     * @param userId 鍙傛暟0
     * @return com.dingtalk.api.response.OapiUserGetResponse
     * @author Carlos
     * @date 2023/6/30 15:27
     */
    public static OapiV2UserGetResponse getUserInfo(String userId) {
        return service.getUserInfo(userId);
    }


    /**
     * 鑾峰彇閮ㄩ棬璇︽儏
     *
     * @param deptId 閮ㄩ棬id
     * @return com.dingtalk.api.response.OapiV2DepartmentGetResponse.DeptGetResponse
     * @author Carlos
     * @date 2024/7/1 17:09
     */
    public static OapiV2DepartmentGetResponse.DeptGetResponse getDepartmentInfo(Long deptId) {
        return service.getDeptInfo(deptId);
    }


    /**
     * 鏍规嵁鎵嬫満鍙疯幏鍙栫敤鎴?
     *
     * @param mobile 鎵嬫満鍙?
     * @return com.dingtalk.api.response.OapiV2UserGetbymobileResponse.UserGetByMobileResponse
     * @throws
     * @author Carlos
     * @date 2025-03-25 15:53
     */
    public OapiV2UserGetbymobileResponse getByMobile(String mobile) {
        return service.getByMobile(mobile);
    }

    /**
     * 鑾峰彇瀛愮骇閮ㄩ棬
     *
     * @param deptId 閮ㄩ棬id
     * @return List<OapiV2DepartmentListsubResponse.DeptBaseResponse>
     * @author Carlos
     * @date 2025-02-13 16:27
     */
    public static List<OapiV2DepartmentListsubResponse.DeptBaseResponse> listDeptSub(Long deptId) {
        return service.listDeptSub(deptId);
    }

    /**
     * 鑾峰彇閮ㄩ棬鐢ㄦ埛
     *
     * @param deptId 閮ㄩ棬id
     * @return com.dingtalk.api.response.OapiV2UserListResponse.PageResult
     * @author Carlos
     * @date 2025-02-13 16:27
     */
    public static OapiV2UserListResponse.PageResult listDeptUser(Long deptId, Long cursor, Long size) {
        return service.listUser(deptId, cursor, size);
    }

    /**
     * 鍒涘缓鐢ㄦ埛
     *
     * @param request 鍙傛暟0
     * @return com.dingtalk.api.response.OapiV2UserCreateResponse.UserCreateResponse
     * @author Carlos
     * @date 2025-03-01 19:50
     */
    public OapiV2UserCreateResponse createUser(OapiV2UserCreateRequest request) {
        return service.createUser(request);
    }

    /**
     * 淇鏀圭敤鎴
     *
     * @param request 鍙傛暟0
     * @return com.dingtalk.api.response.OapiV2UserUpdateResponse
     * @throws
     * @author Carlos
     * @date 2025-03-01 19:52
     */
    public OapiV2UserUpdateResponse updateUser(OapiV2UserUpdateRequest request) {
        return service.updateUser(request);
    }

    /**
     * 鍒犻櫎鐢ㄦ埛
     *
     * @param request 鍒犻櫎鐢ㄦ埛鍙傛暟
     * @return com.dingtalk.api.response.OapiV2UserDeleteResponse
     * @author Carlos
     * @date 2025-03-01 19:53
     */
    public OapiV2UserDeleteResponse deleteUser(OapiV2UserDeleteRequest request) {
        return service.deleteUser(request);
    }


    /**
     * 鍒涘缓閮ㄩ棬
     *
     * @param request 鍙傛暟0
     * @return com.dingtalk.api.response.OapiV2DepartmentCreateResponse.DeptCreateResponse
     * @throws
     * @author Carlos
     * @date 2025-03-01 19:54
     */
    public OapiV2DepartmentCreateResponse createDepartment(OapiV2DepartmentCreateRequest request) {
        return service.createDepartment(request);
    }

    /**
     * 淇鏀归儴闂
     *
     * @param request 鍙傛暟0
     * @return com.dingtalk.api.response.OapiV2DepartmentUpdateResponse
     * @throws
     * @author Carlos
     * @date 2025-03-01 19:55
     */
    public OapiV2DepartmentUpdateResponse updateDepartment(OapiV2DepartmentUpdateRequest request) {
        return service.updateDepartment(request);
    }

    /**
     * 鍒犻櫎閮ㄩ棬
     *
     * @param request 鍒犻櫎閮ㄩ棬鍙傛暟
     * @return com.dingtalk.api.response.OapiV2DepartmentDeleteResponse
     * @author Carlos
     * @date 2025-03-01 19:56
     */
    public OapiV2DepartmentDeleteResponse deleteDepartment(OapiV2DepartmentDeleteRequest request) {
        return service.deleteDepartment(request);
    }

    /**
     * <p>
     * 鑾峰彇閽夐拤accessToken
     * </p>
     *
     * @author Carlos
     * @date 2024/7/19 9:46
     */
    public static String getToken() {
        return service.getToken();
    }

    /**
     * 娑堟伅鍙戦?
     *
     * @param sendMessageRequest 璇锋眰鍙傛暟
     * @return com.dingtalk.api.response.OapiMessageCorpconversationAsyncsendV2Response
     * @author Carlos
     * @date 2025-04-14 23:29
     */
    public static OapiMessageCorpconversationAsyncsendV2Response send(SendMessageRequest sendMessageRequest) {
        return service.send(sendMessageRequest);
    }

}
