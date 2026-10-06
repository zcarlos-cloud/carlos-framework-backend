package com.carlos.message.channel.dingtalk;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.carlos.message.channel.AbstractChannelAdapter;
import com.carlos.message.core.channel.ChannelType;
import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;
import com.carlos.message.manager.MessageChannelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 钉钉渠道适配器
 * </p>
 *
 * <p>
 * 支持工作通知发送，基于钉钉开放平台 API。
 * </p>
 *
 * @author Carlos
 * @date 2026/3/12
 */
@Slf4j
@Component
public class DingtalkChannelAdapter extends AbstractChannelAdapter {

    private static final String GET_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";
    private static final String SEND_WORK_NOTIFY_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2";

    public DingtalkChannelAdapter(MessageChannelManager messageChannelManager) {
        super(messageChannelManager);
    }

    @Override
    public String getChannelCode() {
        return "DINGTALK";
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.DINGTALK;
    }

    @Override
    public boolean supports(String messageType) {
        return true;
    }

    @Override
    public boolean healthCheck() {
        try {
            if (!super.isAvailable()) {
                return false;
            }
            String accessToken = getAccessToken();
            return StrUtil.isNotBlank(accessToken);
        } catch (Exception e) {
            log.warn("钉钉渠道健康检查失败", e);
            return false;
        }
    }

    @Override
    protected SendResult doSend(MessageContext context) {
        String userId = context.getReceiverId();
        if (StrUtil.isBlank(userId)) {
            return SendResult.fail("接收者ID不能为空");
        }

        String content = context.getMessageContent();
        if (StrUtil.isBlank(content)) {
            return SendResult.fail("消息内容不能为空");
        }

        try {
            String accessToken = getAccessToken();
            if (StrUtil.isBlank(accessToken)) {
                return SendResult.fail("DINGTALK_TOKEN_FAIL", "获取钉钉访问令牌失败");
            }

            JSONObject msg = new JSONObject();
            msg.set("msgtype", "markdown");
            JSONObject markdown = new JSONObject();
            markdown.set("title", context.getMessageTitle() != null ? context.getMessageTitle() : "消息通知");
            markdown.set("text", content);
            msg.set("markdown", markdown);

            JSONObject body = new JSONObject();
            body.set("userid_list", userId);
            body.set("msg", msg);
            body.set("agent_id", getAgentId());

            String url = SEND_WORK_NOTIFY_URL + "?access_token=" + accessToken;
            try (HttpResponse response = HttpRequest.post(url)
                .body(body.toString(), "application/json")
                .timeout(10000)
                .execute()) {

                String resultStr = response.body();
                JSONObject result = JSONUtil.parseObj(resultStr);
                int errcode = result.getInt("errcode", -1);
                if (errcode == 0) {
                    String taskId = result.getStr("task_id");
                    return SendResult.success(taskId != null ? taskId : "DT" + System.currentTimeMillis());
                } else {
                    String errmsg = result.getStr("errmsg", "钉钉发送失败");
                    return SendResult.fail("DINGTALK_SEND_FAIL", errmsg);
                }
            }
        } catch (Exception e) {
            log.error("钉钉消息发送异常: userId={}", userId, e);
            return SendResult.fail("DINGTALK_EXCEPTION", e.getMessage());
        }
    }

    private String getAccessToken() {
        try {
            String appKey = getConfigValue("appKey");
            String appSecret = getConfigValue("appSecret");
            if (StrUtil.isBlank(appKey) || StrUtil.isBlank(appSecret)) {
                log.warn("钉钉配置缺失: appKey 或 appSecret 为空");
                return null;
            }

            try (HttpResponse response = HttpRequest.get(GET_TOKEN_URL)
                .form("appkey", appKey)
                .form("appsecret", appSecret)
                .timeout(10000)
                .execute()) {

                JSONObject result = JSONUtil.parseObj(response.body());
                if (result.getInt("errcode", -1) == 0) {
                    return result.getStr("access_token");
                }
                log.warn("获取钉钉 Token 失败: {}", response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("获取钉钉 Token 异常", e);
            return null;
        }
    }

    private String getAgentId() {
        String agentId = getConfigValue("agentId");
        return agentId != null ? agentId : "";
    }

    @SuppressWarnings("unchecked")
    private String getConfigValue(String key) {
        if (channelConfig == null || channelConfig.getChannelConfig() == null) {
            return null;
        }
        try {
            cn.hutool.json.JSONObject config = JSONUtil.parseObj(channelConfig.getChannelConfig());
            return config.getStr(key);
        } catch (Exception e) {
            log.warn("解析钉钉配置失败", e);
            return null;
        }
    }
}
