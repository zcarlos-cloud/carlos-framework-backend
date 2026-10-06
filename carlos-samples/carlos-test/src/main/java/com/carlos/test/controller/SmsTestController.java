package com.carlos.test.controller;

import com.carlos.sms.SmsUtil;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.comm.utils.SmsUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;


@RestController
@RequestMapping("sms")
@Tag(name = "短信测试")
@Slf4j
public class SmsTestController {

    @GetMapping("send")
    @Operation(summary = "发送短信测")
    public void sendSms() {
        LinkedHashMap<String, String> map = Maps.newLinkedHashMap();
        map.put("code", SmsUtils.getRandomInt(6));
        SmsUtil.sendByTemplateKey("13032820513", "gecode", null, map, false);

        // map.put("eventId", SmsUtils.getRandomInt(6));
        // SmsUtil.sendByTemplateKey("13032820513", "event01", null, map, false);

    }


}
