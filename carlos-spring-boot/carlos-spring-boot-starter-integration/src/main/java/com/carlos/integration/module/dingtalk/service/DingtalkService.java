package com.carlos.integration.module.dingtalk.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.carlos.integration.common.exception.DockingException;
import com.carlos.integration.module.dingtalk.config.DingtalkAccessTokenManager;
import com.carlos.integration.module.dingtalk.config.DingtalkProperties;
import com.carlos.integration.module.dingtalk.exception.DockingDingtalkException;
import com.carlos.integration.module.dingtalk.support.SendMessageRequest;
import com.carlos.integration.module.dingtalk.support.msg.*;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.*;
import com.dingtalk.api.response.*;
import com.taobao.api.ApiException;
import com.taobao.api.TaobaoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 钉钉服务 - 旧版 SDK 实现
 * </p>
 *
 * @author Carlos
 * @date 2022/2/23 18:05
 * @deprecated 请使{@link DingtalkRestClientService} 替代
 */
@Slf4j
@RequiredArgsConstructor
@Deprecated(since = "2.0.0", forRemoval = true)
public class DingtalkService {

    private final DingtalkAccessTokenManager tokenManager;

    private final DingtalkProperties properties;

    private DingTalkClient getClient(String path) {
        String host = properties.getHost();
        if (!StrUtil.endWith(host, StrUtil.SLASH)) {
            host += StrUtil.SLASH;
        }
        host += path;
        return new DefaultDingTalkClient(host);
    }


    /**
     * getUse获取用户信息
     *
     * @param userId 用户id
     * @return com.dingtalk.api.response.OapiUserGetResponse
     * @author Carlos
     * @date 2023/6/30 13:45
     */
    public OapiV2UserGetResponse getUserInfo(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new DockingException("userId can't be null");
        }
        OapiV2UserGetRequest req = new OapiV2UserGetRequest();
        req.setUserid(userId);
        req.setLanguage("zh_CN");
        try {
            OapiV2UserGetResponse response = getClient("topapi/v2/user/get").execute(req, getToken());
            checkResult(response);
            return response;
        } catch (ApiException e) {
            log.error("Get dingtalk access_token error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("钉钉服务访问失败");
        }
    }

    /**
     * 获取用户id
     *
     * @param code code
     * @return java.lang.String
     * @author Carlos
     * @date 2023/6/30 13:47
     */
    public OapiV2UserGetuserinfoResponse.UserGetByCodeResponse getUserId(String code) {
        if (StrUtil.isBlank(code)) {
            throw new DockingException("code can't be null");
        }

        OapiV2UserGetuserinfoRequest req = new OapiV2UserGetuserinfoRequest();
        req.setCode(code);
        try {
            OapiV2UserGetuserinfoResponse response = getClient("topapi/v2/user/getuserinfo").execute(req, getToken());
            checkResult(response);
            return response.getResult();
        } catch (ApiException e) {
            log.error("Get dingtalk access_token error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("钉钉服务访问失败");

        }
    }

    /**
     * 获取部门信息
     *
     * @param deptId 部门id
     * @return java.lang.String
     * @author Carlos
     * @date 2023/6/30 13:47
     */
    public OapiV2DepartmentGetResponse.DeptGetResponse getDeptInfo(Long deptId) {
        if (deptId == null) {
            throw new DockingException("deptId can't be null");
        }

        OapiV2DepartmentGetRequest req = new OapiV2DepartmentGetRequest();
        req.setDeptId(deptId);
        req.setLanguage("zh_CN");
        try {
            OapiV2DepartmentGetResponse response = getClient("topapi/v2/department/get").execute(req, getToken());
            checkResult(response);
            return response.getResult();
        } catch (ApiException e) {
            log.error("Get dingtalk opapi/v2/department/get error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("钉钉服务访问失败");

        }
    }

    /**
     * 根据手机号查询用
     *
     * @param mobile 手机
     * @return com.dingtalk.api.response.OapiV2UserGetbymobileResponse.UserGetByMobileResponse
     * @throws
     * @author Carlos
     * @date 2025-03-25 15:52
     */
    public OapiV2UserGetbymobileResponse getByMobile(String mobile) {
        if (ObjUtil.isEmpty(mobile)) {
            throw new DockingException("mobile can't be null");
        }

        OapiV2UserGetbymobileRequest req = new OapiV2UserGetbymobileRequest();
        req.setMobile(mobile);

        try {
            OapiV2UserGetbymobileResponse response = getClient("topapi/v2/user/getbymobile").execute(req, getToken());
            // checkResult(response);
            return response;
        } catch (ApiException e) {
            log.error("Get dingtalk topapi/v2/user/getbymobile error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("钉钉服务访问失败");

        }
    }


    /**
     * 获取子级部门信息
     *
     * @param deptId 部门id
     * @return java.lang.String
     * @author Carlos
     * @date 2023/6/30 13:47
     */
    public List<OapiV2DepartmentListsubResponse.DeptBaseResponse> listDeptSub(Long deptId) {
        if (deptId == null) {
            throw new DockingException("deptId can't be null");
        }

        OapiV2DepartmentListsubRequest req = new OapiV2DepartmentListsubRequest();
        req.setDeptId(deptId);
        req.setLanguage("zh_CN");
        try {
            OapiV2DepartmentListsubResponse response = getClient("topapi/v2/department/listsub").execute(req, getToken());
            checkResult(response);
            return response.getResult();
        } catch (ApiException e) {
            log.error("Get dingtalk topapi/v2/department/listsub error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("部门信息获取失败");
        }
    }

    /**
     * 获取部门用户信息
     *
     * @param deptId 部门id
     * @return com.dingtalk.api.response.OapiV2UserListResponse.PageResult
     * @author Carlos
     * @date 2025-02-13 16:25
     */
    public OapiV2UserListResponse.PageResult listUser(Long deptId, Long cursor, Long size) {
        if (deptId == null) {
            throw new DockingException("deptId can't be null");
        }

        OapiV2UserListRequest req = new OapiV2UserListRequest();
        req.setDeptId(deptId);
        req.setLanguage("zh_CN");
        req.setCursor(cursor);
        req.setSize(size);
        try {
            OapiV2UserListResponse response = getClient("topapi/v2/user/list").execute(req, getToken());
            checkResult(response);
            return response.getResult();
        } catch (ApiException e) {
            log.error("Get dingtalk topapi/v2/user/list error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("部门用户信息获取失败");

        }
    }

    /**
     * 创建用户
     *
     * @param request 参数0
     * @return com.dingtalk.api.response.OapiV2UserCreateResponse.UserCreateResponse
     * @author Carlos
     * @date 2025-03-01 19:50
     */
    public OapiV2UserCreateResponse createUser(OapiV2UserCreateRequest request) {
        try {
            OapiV2UserCreateResponse response = getClient("topapi/v2/user/create").execute(request, getToken());
            checkResult(response);
            return response;
        } catch (ApiException e) {
            log.error("Get dingtalk topapi/v2/user/create error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("创建钉钉用户失败");
        }
    }

    /**
     * 修改用户
     *
     * @param request 参数0
     * @return com.dingtalk.api.response.OapiV2UserUpdateResponse
     * @author Carlos
     * @date 2025-03-01 19:52
     */
    public OapiV2UserUpdateResponse updateUser(OapiV2UserUpdateRequest request) {
        try {
            OapiV2UserUpdateResponse response = getClient("topapi/v2/user/update").execute(request, getToken());
            checkResult(response);
            return response;
        } catch (ApiException e) {
            log.error("Get dingtalk topapi/v2/user/update error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("修改钉钉用户失败");
        }
    }

    /**
     * 删除用户
     *
     * @param request 参数0
     * @return com.dingtalk.api.response.OapiV2UserDeleteResponse
     * @author Carlos
     * @date 2025-05-23 08:29
     */
    public OapiV2UserDeleteResponse deleteUser(OapiV2UserDeleteRequest request) {
        try {
            OapiV2UserDeleteResponse response = getClient("topapi/v2/user/delete").execute(request, getToken());
            checkResult(response);
            return response;
        } catch (ApiException e) {
            log.error("Get dingtalk topapi/v2/user/delete error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("删除钉钉用户失败");
        }
    }


    /**
     * 创建部门
     *
     * @param request 参数0
     * @return com.dingtalk.api.response.OapiV2DepartmentCreateResponse.DeptCreateResponse
     * @author Carlos
     * @date 2025-03-01 19:54
     */
    public OapiV2DepartmentCreateResponse createDepartment(OapiV2DepartmentCreateRequest request) {
        try {
            OapiV2DepartmentCreateResponse response = getClient("topapi/v2/department/create").execute(request, getToken());
            checkResult(response);
            return response;
        } catch (ApiException e) {
            log.error("Get dingtalk topapi/v2/department/create error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("创建钉钉部门失败");
        }
    }


    /**
     * 删除部门
     *
     * @param request 参数0
     * @return com.dingtalk.api.response.OapiV2DepartmentDeleteResponse
     * @author Carlos
     * @date 2025-05-23 08:27
     */
    public OapiV2DepartmentDeleteResponse deleteDepartment(OapiV2DepartmentDeleteRequest request) {
        try {
            OapiV2DepartmentDeleteResponse response = getClient("topapi/v2/department/delete").execute(request, getToken());
            checkResult(response);
            return response;
        } catch (ApiException e) {
            log.error("Get dingtalk topapi/v2/department/delete error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("删除钉钉部门失败");
        }
    }

    /**
     * 修改部门
     *
     * @param request 参数0
     * @return com.dingtalk.api.response.OapiV2DepartmentUpdateResponse
     * @author Carlos
     * @date 2025-03-01 19:55
     */
    public OapiV2DepartmentUpdateResponse updateDepartment(OapiV2DepartmentUpdateRequest request) {
        try {
            OapiV2DepartmentUpdateResponse response = getClient("topapi/v2/department/update").execute(request, getToken());
            checkResult(response);
            return response;
        } catch (ApiException e) {
            log.error("Get dingtalk topapi/v2/department/update error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("修改钉钉部门失败");
        }
    }


    /**
     * 处理响应结果
     *
     * @param result 钉钉响应结果
     * @author Carlos
     * @date 2023/6/30 13:46
     */
    private void checkResult(TaobaoResponse result) {
        // String errcode = result.getErrorCode();
        // if (!errcode.equals("0")) {
        //     log.error("Dingtalk request error: errorCode:{}, errMsg:{} url:{} param:{} response:{}", errcode, result.getMessage(), result.getRequestUrl(), result.getParams(), JSONUtil.toJsonPrettyStr(result));
        //     throw new DockingException("钉钉服务调用出错");
        // }
        log.info("Dingtalk request success: url:{}   param:{} response:{}", result.getRequestUrl(), result.getParams(), JSONUtil.toJsonPrettyStr(result));
    }


    /**
     * 获取accessToken
     *
     * @author Carlos
     * @date 2023/4/7 16:06
     */
    public String getToken() {
        return tokenManager.getAccessToken();
    }


    /**
     * 获取部门信息
     *
     * @param sendMessageRequest 消息内容
     * @return java.lang.String
     * @author Carlos
     * @date 2023/6/30 13:47
     */
    public OapiMessageCorpconversationAsyncsendV2Response send(SendMessageRequest sendMessageRequest) {
        if (CollectionUtils.isEmpty(sendMessageRequest.getPushPhoneList())) {
            throw new DockingException("userIds can't be empty");
        }
        String userId = getUserIdByPhone(sendMessageRequest);
        if (CharSequenceUtil.isBlank(userId)) {
            throw new DockingException("userIds can't be empty");
        }
        OapiMessageCorpconversationAsyncsendV2Request req = new OapiMessageCorpconversationAsyncsendV2Request();
        req.setAgentId(Long.valueOf(properties.getAgentId()));
        req.setUseridList(userId);
        req.setToAllUser(false);
        if (sendMessageRequest.getPushDeptList() != null && !sendMessageRequest.getPushDeptList().isEmpty()) {
            String deptString = String.join(",", sendMessageRequest.getPushDeptList());
            req.setDeptIdList(deptString);
        }
        OapiMessageCorpconversationAsyncsendV2Request.Msg reqMsg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();
        Msg msg = sendMessageRequest.getMsg();
        switch (msg) {
            case TextMsg textMsg -> {
                reqMsg.setMsgtype("text");
                reqMsg.setText(BeanUtil.copyProperties(textMsg, OapiMessageCorpconversationAsyncsendV2Request.Text.class));
            }
            case FileMsg fileMsg -> {
                reqMsg.setMsgtype("file");
                reqMsg.setFile(BeanUtil.copyProperties(fileMsg, OapiMessageCorpconversationAsyncsendV2Request.File.class));
            }
            case LinkMsg linkMsg -> {
                reqMsg.setMsgtype("link");
                reqMsg.setLink(BeanUtil.copyProperties(linkMsg, OapiMessageCorpconversationAsyncsendV2Request.Link.class));
            }
            case ImageMsg imageMsg -> {
                reqMsg.setMsgtype("image");
                reqMsg.setImage(BeanUtil.copyProperties(imageMsg, OapiMessageCorpconversationAsyncsendV2Request.Image.class));
            }
            case VoiceMsg voiceMsg -> {
                reqMsg.setMsgtype("voice");
                reqMsg.setVoice(BeanUtil.copyProperties(voiceMsg, OapiMessageCorpconversationAsyncsendV2Request.Voice.class));
            }
            case MarkdownMsg markdownMsg -> {
                reqMsg.setMsgtype("markdown");
                reqMsg.setMarkdown(BeanUtil.copyProperties(markdownMsg, OapiMessageCorpconversationAsyncsendV2Request.Markdown.class));
            }
            case ActionCardMsg actionCardMsg -> {
                reqMsg.setMsgtype("action_card");
                reqMsg.setActionCard(BeanUtil.copyProperties(actionCardMsg, OapiMessageCorpconversationAsyncsendV2Request.ActionCard.class));
            }
            case OAMsg oaMsg -> {
                reqMsg.setMsgtype("oa");
                reqMsg.setOa(BeanUtil.copyProperties(oaMsg, OapiMessageCorpconversationAsyncsendV2Request.OA.class));
            }
            default -> {
            }
        }

        // FIXME: Carlos 2025-04-15 该代码在后续将进行移
        String context = sendMessageRequest.getContext();
        if (StrUtil.isNotBlank(context)) {
            reqMsg.setMsgtype("text");
            OapiMessageCorpconversationAsyncsendV2Request.Text text = new OapiMessageCorpconversationAsyncsendV2Request.Text();
            text.setContent(context);
            reqMsg.setText(text);

        }
        req.setMsg(reqMsg);
        try {
            OapiMessageCorpconversationAsyncsendV2Response response = getClient("topapi/message/corpconversation/asyncsend_v2").execute(req, getToken());
            checkResult(response);
            return response;
        } catch (ApiException e) {
            log.error("Get dingtalk access_token error, message:{}", e.getMessage(), e);
            throw new DockingDingtalkException("钉钉服务访问失败");

        }
    }

    private String getUserIdByPhone(SendMessageRequest sendMessageRequest) {
        if (CollectionUtils.isEmpty(sendMessageRequest.getPushPhoneList())) {
            return null;
        }
        List<String> pushPhoneList = sendMessageRequest.getPushPhoneList();
        OapiV2UserGetbymobileRequest req = new OapiV2UserGetbymobileRequest();
        List<String> userIdList = new ArrayList<>();
        for (String phone : pushPhoneList) {
            req.setMobile(phone);
            try {
                OapiV2UserGetbymobileResponse response = getClient("topapi/v2/user/getbymobile").execute(req, getToken());
                checkResult(response);
                String userid = response.getResult().getUserid();
                log.info("Get dingtalk userid success: userid:{}", userid);
                userIdList.add(userid);
            } catch (ApiException e) {
                log.error("Get dingtalk userid error, message:{}", e.getMessage(), e);
                throw new DockingDingtalkException("钉钉服务访问失败");
            }
        }
        return String.join(StrPool.COMMA, userIdList);
    }

}
