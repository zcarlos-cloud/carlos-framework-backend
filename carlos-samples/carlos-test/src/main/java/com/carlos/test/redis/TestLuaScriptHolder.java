package com.carlos.test.redis;


import com.carlos.redis.lua.AbstractLuaScriptHolder;
import com.carlos.redis.util.RedisUtil;
import org.springframework.stereotype.Component;

/**
 * <p>
 *   脚本加载
 * </p>
 *
 * @author Carlos
 * @date 2025-09-15 14:51
 */
@Component
public class TestLuaScriptHolder extends AbstractLuaScriptHolder {


    public static String KEY;


    @Override
    public void load() {
        String content = readClasspathScript("lua/form_indicator.lua");
        String sha = RedisUtil.loadScripts(content);
        KEY = sha;
    }


}
