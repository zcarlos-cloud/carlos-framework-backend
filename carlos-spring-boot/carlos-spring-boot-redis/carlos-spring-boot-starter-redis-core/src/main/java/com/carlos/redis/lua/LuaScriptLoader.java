package com.carlos.redis.lua;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * <p>
 *   lua脚本加载器
 * </p>
 *
 * @author Carlos
 * @date 2025-09-15 16:52 
 */
@Slf4j
@AllArgsConstructor
public class LuaScriptLoader {

    private final List<LuaScriptHolder> holders;


    @PostConstruct
    public void init() {
        if (CollUtil.isEmpty(holders)) {
            log.warn("LuaScriptLoader is empty");
            return;
        }
        for (LuaScriptHolder holder : holders) {
            holder.load();
            log.info("LuaScriptLoader load:{}", holder.getClass().getSimpleName());
        }
    }


}
