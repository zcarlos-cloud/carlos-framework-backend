package com.carlos.datasource.config;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.carlos.snowflake.SnowflakeUtil;


/**
 * <p>
 * 自定义32位id生成器
 * </p>
 *
 * @author carlos
 * @date 2021/12/13 18:21
 */
public class CustomizeIdGenerator implements IdentifierGenerator {


    @Override
    public Long nextId(Object entity) {
        return SnowflakeUtil.longId();
    }

    @Override
    public String nextUUID(Object entity) {
        return IdUtil.simpleUUID();
    }


}
