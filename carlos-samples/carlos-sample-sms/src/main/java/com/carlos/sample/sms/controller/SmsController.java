package com.carlos.sample.sms.controller;

import com.carlos.core.response.Result;
import com.carlos.sms.SmsUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * <p>
 * 短信服务演示控制器
 * </p>
 *
 * @author Carlos
 * @date 2026/3/15 22:52
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
@Tag(name = "短信服务演示接口", description = "演示 Carlos 短信服务的各种功能")
public class SmsController {

    /**
     * 发送单条短信（使用模板）
     *
     * @param phone        手机号
     * @param templateCode 模板编码
     * @param code         验证码
     * @return 发送结果
     */
    @Operation(summary = "发送单条短信", description = "使用模板发送单条短信")
    @GetMapping("/send")
    public Result<String> sendSms(
        @Parameter(description = "手机号", required = true, example = "13800138000")
        @RequestParam String phone,
        @Parameter(description = "模板编码", required = true, example = "VERIFY_CODE")
        @RequestParam String templateCode,
        @Parameter(description = "验证码", required = true, example = "123456")
        @RequestParam String code) {
        log.info("发送短信请求 - 手机号: {}, 模板编码: {}, 验证码: {}", phone, templateCode, code);

        LinkedHashMap<String, String> messages = new LinkedHashMap<>();
        messages.put("code", code);

        try {
            SmsUtil.sendByTemplateKey(phone, templateCode, messages);
            log.info("短信发送成功 - 手机号: {}", phone);
            return Result.success("短信发送成功");
        } catch (Exception e) {
            log.error("短信发送失败 - 手机号: {}, 错误: {}", phone, e.getMessage());
            return Result.error("短信发送失败: " + e.getMessage());
        }
    }

    /**
     * 群发短信（使用模板）
     *
     * @param phones       手机号列表
     * @param templateCode 模板编码
     * @param code         验证码
     * @return 发送结果
     */
    @Operation(summary = "群发短信", description = "使用模板发送短信给多个手机号")
    @PostMapping("/send-batch")
    public Result<String> sendBatchSms(
        @Parameter(description = "手机号列表", required = true)
        @RequestBody List<String> phones,
        @Parameter(description = "模板编码", required = true, example = "VERIFY_CODE")
        @RequestParam String templateCode,
        @Parameter(description = "验证码", required = true, example = "123456")
        @RequestParam String code) {
        log.info("批量发送短信请求 - 手机号数量: {}, 模板编码: {}", phones.size(), templateCode);

        LinkedHashMap<String, String> messages = new LinkedHashMap<>();
        messages.put("code", code);

        try {
            SmsUtil.sendByTemplateKey(phones, templateCode, messages);
            log.info("批量短信发送成功 - 手机号数量: {}", phones.size());
            return Result.success("批量短信发送成功");
        } catch (Exception e) {
            log.error("批量短信发送失败 - 错误: {}", e.getMessage());
            return Result.error("批量短信发送失败: " + e.getMessage());
        }
    }

    /**
     * 异步发送短信（使用模板）
     *
     * @param phone        手机号
     * @param templateCode 模板编码
     * @param code         验证码
     * @return 发送结果
     */
    @Operation(summary = "异步发送短信", description = "使用模板异步发送短信")
    @GetMapping("/send-async")
    public Result<String> sendSmsAsync(
        @Parameter(description = "手机号", required = true, example = "13800138000")
        @RequestParam String phone,
        @Parameter(description = "模板编码", required = true, example = "VERIFY_CODE")
        @RequestParam String templateCode,
        @Parameter(description = "验证码", required = true, example = "123456")
        @RequestParam String code) {
        log.info("异步发送短信请求 - 手机号: {}, 模板编码: {}", phone, templateCode);

        LinkedHashMap<String, String> messages = new LinkedHashMap<>();
        messages.put("code", code);

        try {
            SmsUtil.sendByTemplateKey(phone, templateCode, null, messages, true);
            log.info("异步短信发送请求已提交 - 手机号: {}", phone);
            return Result.success("异步短信发送请求已提交");
        } catch (Exception e) {
            log.error("异步短信发送失败 - 手机号: {}, 错误: {}", phone, e.getMessage());
            return Result.error("异步短信发送失败: " + e.getMessage());
        }
    }

    /**
     * 使用指定配置发送短信
     *
     * @param phone        手机号
     * @param templateCode 模板编码
     * @param configId     短信配置ID
     * @param code         验证码
     * @return 发送结果
     */
    @Operation(summary = "指定配置发送短信", description = "使用指定的短信配置发送短信")
    @GetMapping("/send-with-config")
    public Result<String> sendSmsWithConfig(
        @Parameter(description = "手机号", required = true, example = "13800138000")
        @RequestParam String phone,
        @Parameter(description = "模板编码", required = true, example = "VERIFY_CODE")
        @RequestParam String templateCode,
        @Parameter(description = "配置ID", required = true, example = "alibaba")
        @RequestParam String configId,
        @Parameter(description = "验证码", required = true, example = "123456")
        @RequestParam String code) {
        log.info("指定配置发送短信请求 - 手机号: {}, 配置ID: {}, 模板编码: {}", phone, configId, templateCode);

        LinkedHashMap<String, String> messages = new LinkedHashMap<>();
        messages.put("code", code);

        try {
            SmsUtil.sendByTemplateKey(phone, templateCode, configId, messages, false);
            log.info("短信发送成功 - 手机号: {}, 配置ID: {}", phone, configId);
            return Result.success("短信发送成功");
        } catch (Exception e) {
            log.error("短信发送失败 - 手机号: {}, 配置ID: {}, 错误: {}", phone, configId, e.getMessage());
            return Result.error("短信发送失败: " + e.getMessage());
        }
    }
}
