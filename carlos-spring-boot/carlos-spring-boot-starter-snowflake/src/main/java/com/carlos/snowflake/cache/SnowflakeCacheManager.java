package com.carlos.snowflake.cache;

import com.carlos.snowflake.SnowflakeInfo;


/**
 * <p>
 *缓存管理
 * </p>
 *
 * @author Carlos
 * @date 2025-03-17 13:54 
 */
public interface SnowflakeCacheManager {


    /**
     * 新增缓存
     *
     * @param bean 缓存信息
     * @return boolean
     * @author Carlos
     * @date 2025-03-17 14:11
     */
    boolean putCache(SnowflakeInfo bean);

    /**
     * 删除缓存
     *
     * @author Carlos
     * @date 2025-03-17 14:11
     */
    void delCache();


    /**
     * 重置缓存过期时间
     *
     * @return boolean
     * @author Carlos
     * @date 2025-03-17 14:11
     */
    void resetExpire();
}
