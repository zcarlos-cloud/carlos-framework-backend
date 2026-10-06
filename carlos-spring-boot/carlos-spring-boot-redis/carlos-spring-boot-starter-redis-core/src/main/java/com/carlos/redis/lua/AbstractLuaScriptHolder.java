package com.carlos.redis.lua;


import cn.hutool.core.io.resource.ResourceUtil;
import com.carlos.redis.exception.CacheScriptReadException;

/**
 * <p>
 *   脚本加载
 * </p>
 *
 * @author Carlos
 * @date 2025-09-15 14:51
 */
public abstract class AbstractLuaScriptHolder implements LuaScriptHolder {


    @Override
    public void load() {

    }


    /**
     * 读取脚本内容
     *
     * @param path 脚本路径
     * @return java.lang.String
     * @author Carlos
     * @date 2025-09-15 15:23
     */
    public String readClasspathScript(String path) {
        String script = null;
        try {
            script = ResourceUtil.readUtf8Str(path);
        } catch (Exception e) {
            throw new CacheScriptReadException(e);
        }
        return script;

    }
}
