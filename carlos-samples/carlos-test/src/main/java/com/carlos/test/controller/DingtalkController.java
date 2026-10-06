package com.carlos.test.controller;


import com.carlos.integration.module.dingtalk.support.DingtalkUtil;
import com.carlos.integration.module.dingtalk.support.SendMessageRequest;
import com.carlos.integration.module.dingtalk.support.msg.ActionCardMsg;
import com.dingtalk.api.response.OapiMessageCorpconversationAsyncsendV2Response;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
@RequestMapping("dingtalk")
@Tag(name = "钉钉测试", description = "钉钉测试")
@Slf4j
public class DingtalkController {

    @GetMapping("send")
    @Operation(summary = "发送通知测试")
    public void sendDingtalkMessage() {
        SendMessageRequest request = new SendMessageRequest();

        ArrayList<String> userPhoneList = Lists.newArrayList();
        userPhoneList.add("13032820513");
        request.setPushPhoneList(userPhoneList);
        request.setPushDeptList(Lists.newArrayList());
        ActionCardMsg msg = new ActionCardMsg();
        msg.setBtnJsonList(Lists.newArrayList());
        msg.setBtnOrientation("");
        msg.setMarkdown("测试消息内容");
        msg.setSingleTitle("singleTitle");
        msg.setSingleUrl("https:www.baidu.com");
        msg.setTitle("标题");
        request.setMsg(msg);
        OapiMessageCorpconversationAsyncsendV2Response send = DingtalkUtil.send(request);
        log.info("send:{}", send);
    }
}
