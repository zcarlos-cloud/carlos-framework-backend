package com.carlos.test.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlRunnable;
import com.carlos.core.util.ExecutorUtil;
import com.carlos.redis.util.RedisUtil;
import com.carlos.test.pojo.dto.OrgUserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;


@RestController
@RequestMapping("test")
@Tag(name = "测试接口")
@Slf4j
public class TestController {

    //
    // @PostMapping("zh")
    // @Operation(summary = "测试中文")
    // public void add(@RequestBody UserCreateParam param) {
    //     SmsBlend smsBlend = SmsFactory.getSmsBlend(SmsServiceType.POSTAL.getCode());
    //
    //     LinkedHashMap<String, String> map = new LinkedHashMap();
    //     map.put("code", "1234");
    //
    //     SmsResponse smsResponse = smsBlend.sendMessage("13032820513", "8aad3e0d-1a67-444e-a7b4-1d809523913c", map);
    //     log.info("param: {}", JSONUtil.toJsonPrettyStr(smsResponse));
    // }


    @PostMapping("init/user/pwd")
    @Operation(summary = "密码解密")
    public String syncUser(String pwd) throws Exception {
        final String key = "aH6fyL1gcwMu7B89";
        final String iv = "cvSWvikkmaBDs2Qi";
        String encrypt = encrypt("SM4/CBC/PKCS7Padding", key.getBytes(), iv.getBytes(), HexUtil.decodeHex(pwd));

        return encrypt;
    }

    public String encrypt(String algorithm, byte[] key, byte[] iv, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm, "BC"); // 使用BC作为提供
        SecretKeySpec keySpec = new SecretKeySpec(key, "SM4");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        // cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] encryptedData = cipher.doFinal(data);
        return new String(encryptedData); // 返回Base64编码的加密数
    }

    @GetMapping("cors")
    @Operation(summary = "跨域测试")
    public void cors() {
        log.info("请求成功");
    }

    @GetMapping("rzt/sendMessage")
    @Operation(summary = "蓉政通消息发")
    public void sendMessage(String phone) {
        // RztUtil.sendTextMessage(Sets.newHashSet(phone), null, null, "测试消息");
        log.info("请求成功");
    }


    @PostMapping("gecode")
    @Operation(summary = "短信发送测")
    public void sendGeCode(String phone) {

        SmsBlend smsBlend = SmsFactory.getSmsBlend("gecode");

        Class<? extends SmsBlend> aClass = smsBlend.getClass();
        Field[] declaredFields = aClass.getDeclaredFields();
        LinkedHashMap<String, Object> map = new LinkedHashMap();
        map.put("code", RandomUtil.randomInt(4));
        SmsResponse smsResponse = smsBlend.sendMessage(phone, "1234");
        log.info("param: {}", JSONUtil.toJsonPrettyStr(smsResponse));
    }

    @PostMapping("redisCache")
    @Operation(summary = "redis缓存测试")
    public void redisCache() {

        Map<String, OrgUserDTO> map = new HashMap<>(4);

        for (int i = 10000; i < 30000; i++) {
            OrgUserDTO user = new OrgUserDTO();
            user.setId((long) i);
            user.setAccount("account");
            user.setLastLogin(LocalDateTime.now());
            user.setLoginCount(RandomUtil.randomInt(500));
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            map.put("user" + i, user);
        }

        long start = System.currentTimeMillis();
        RedisUtil.setValueList(map);
        log.debug("method setValueList:{}", DateUtil.formatBetween(DateUtil.spendMs(start)));

        RedisUtil.setValue("string", "string");
        String value = RedisUtil.getValue("string");
        System.out.println(value);
    }


    private final static ThreadLocal<String> NORMAL_THREAD_LOCAL = new ThreadLocal<>();
    private final static TransmittableThreadLocal<String> TRANS_THREAD_LOCAL = new TransmittableThreadLocal<>();
    private final static ThreadPoolExecutor POOL = ExecutorUtil.POOL;


    @PostMapping("threadlocal")
    @Operation(summary = "测试ThreadLoacal")
    public void importFile2(String id) throws IOException {
        NORMAL_THREAD_LOCAL.set(id);
        TRANS_THREAD_LOCAL.set(id);
        for (int i = 0; i < 5; i++) {
            POOL.execute(TtlRunnable.get(() -> {
                ThreadUtil.sleep(2000);
                log.info("子线程获取normal值：{}", NORMAL_THREAD_LOCAL.get());
                log.info("子线程获取trans值：{}", TRANS_THREAD_LOCAL.get());
            }));
        }
        NORMAL_THREAD_LOCAL.remove();
        TRANS_THREAD_LOCAL.remove();
        log.info("主线程已结束");
    }

}
