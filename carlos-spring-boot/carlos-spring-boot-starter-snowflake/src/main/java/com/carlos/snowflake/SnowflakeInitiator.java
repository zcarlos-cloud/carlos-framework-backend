package com.carlos.snowflake;

import cn.hutool.core.net.NetUtil;
import com.carlos.snowflake.cache.SnowflakeCacheManager;
import com.carlos.snowflake.exception.SnowflakeException;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * <p>
 * snowflake  id生成器初始化 流程：
 * 1.系统启动时生成默认dataCenterId和workerId，并尝试做为key存储到redis
 * 2.若是存储成功，设置redis过时时间为24h，把当前dataCenterId和workerId传入snowflake
 * 3.若是存储失败workerId自加1，并判断workerId不大于31，重复1步骤
 * 4.定义一个定时器，每隔24h刷新redis的过时时间为24h
 * <p>
 * 缺陷：
 * 1.服务启动的时候就尝试链接redis获取机器码，会形成服务启动比平时慢5s左右（具体看电脑配置）
 * 2.在tryInit()的过程当中，最多会重试961次（经过测试，重试1000次会延迟6s左右），也会形成服务启动慢
 * 3.当重试961次（即全部机器码都被占用了），系统会默认返回机器码1-1（这是snowflake硬伤，没办法，只能从自己系统上优化）
 * 4.当服务被强制kill掉时，@PreDestroy注解不会被触发，只能经过自己设置的过时日期（24h）等待过时（这个缺陷目前只想到了经过缩短过时日期优化）
 * 5.此方法依赖于spring的Bean注入方式保证单例，若是经过new SnowflakeInitiator()的方式实例化就会失效（能够自行优化或写成单例默认） 6.不支持多线程调用，想多线程调用的本身优化
 * </p>
 *
 * @author carlos
 * @date 2021/12/13 9:56
 */
@Slf4j
@AllArgsConstructor
public class SnowflakeInitiator {

    private final SnowflakeProperties properties;
    private final SnowflakeCacheManager cacheManager;

    public SnowflakeInfo init() {
        SnowflakeInfo snowflake = getSnowflake();
        if (snowflake == null) {
            throw new SnowflakeException("snowflake init fail, datacenterId and workerId has been used full!");
        }
        return snowflake;
    }


    /**
     * 生成下一组不重复的dataCenterId和workerId
     *
     * @param dto 参数0
     * @return com.carlos.snowflake.SnowflakeInitiator.SnowflakeVo
     * @author carlos
     * @date 2021/12/13 11:22
     */
    private SnowflakeInfo generateSnowflake(SnowflakeInfo dto) {
        if (dto.getWorkerId() < SnowflakeConstant.MAX_WORKER_ID) {
            // 若是workerId < 31
            dto.setWorkerId(dto.getWorkerId() + 1);
        } else {
            // 若是workerId >= 31
            if (dto.getDataCenterId() < SnowflakeConstant.MAX_DATA_CENTER_ID) {
                // 若是workerId >= 31 && dataCenterId < 31
                dto.setDataCenterId(dto.getDataCenterId() + 1);
                dto.setWorkerId(1L);
            } else {
                // 若是workerId >= 31 && dataCenterId >= 31
                dto.setDataCenterId(1L);
                dto.setWorkerId(1L);
            }
        }
        return dto;
    }

    /**
     * 容器销毁时主动删除redis注册记录，此方法不适用于强制终止Spring容器的场景，只做为补充优化
     *
     * @author carlos
     * @date 2021/12/13 11:15
     */
    @PreDestroy
    public void destroy() {
        if (cacheManager != null) {
            cacheManager.delCache();
        }
    }

    /**
     * 获取雪花workerId 和 datacenterId
     *
     * @return com.carlos.snowflake.SnowflakeDTO
     * @author carlos
     * @date 2021/12/13 12:18
     */
    public SnowflakeInfo getSnowflake() {
        SnowflakeInfo snowflakeInfo = new SnowflakeInfo();
        snowflakeInfo.setTag(properties.getTag());
        // FIXME: Carlos 2025-03-17 需要注意获取的ip可能不正确
        String localhostIp = NetUtil.getLocalhostStr();
        snowflakeInfo.setIp(localhostIp);

        if (cacheManager == null) {
            // P1-006 修复：cacheManager 不可用时，基于本机 IP 做一致性哈希生成 workerId/dataCenterId，
            // 避免多实例全部回退到 0-0 导致 ID 冲突
            fillIpHashSlot(snowflakeInfo, localhostIp);
            log.warn("Snowflake cacheManager is null, fallback to IP-hash workerId={}-{}",
                snowflakeInfo.getDataCenterId(), snowflakeInfo.getWorkerId());
            return snowflakeInfo;
        }

        // P3-007 优化：基于 IP 哈希预占候选槽位，失败后再有限探测，减少 SETNX 次数
        int[] preferredSlots = computePreferredSlots(localhostIp);
        for (int slot : preferredSlots) {
            long dc = slot / (SnowflakeConstant.MAX_WORKER_ID + 1);
            long wk = slot % (SnowflakeConstant.MAX_WORKER_ID + 1);
            snowflakeInfo.setDataCenterId(dc);
            snowflakeInfo.setWorkerId(wk);
            if (cacheManager.putCache(snowflakeInfo)) {
                return snowflakeInfo;
            }
        }

        // 预占失败，顺序探测剩余槽位
        for (long dc = 0; dc <= SnowflakeConstant.MAX_DATA_CENTER_ID; dc++) {
            for (long wk = 0; wk <= SnowflakeConstant.MAX_WORKER_ID; wk++) {
                int slot = (int) (dc * (SnowflakeConstant.MAX_WORKER_ID + 1) + wk);
                if (contains(preferredSlots, slot)) {
                    continue;
                }
                snowflakeInfo.setDataCenterId(dc);
                snowflakeInfo.setWorkerId(wk);
                if (cacheManager.putCache(snowflakeInfo)) {
                    return snowflakeInfo;
                }
            }
        }
        return null;
    }

    /**
     * 基于本机 IP 计算一致性哈希槽位，确保相同 IP 在不同启动间获得相同 workerId（除非冲突）
     */
    private void fillIpHashSlot(SnowflakeInfo info, String ip) {
        int totalSlots = (int) ((SnowflakeConstant.MAX_DATA_CENTER_ID + 1) * (SnowflakeConstant.MAX_WORKER_ID + 1));
        int hash = Math.abs(ip.hashCode()) % totalSlots;
        long dc = hash / (SnowflakeConstant.MAX_WORKER_ID + 1);
        long wk = hash % (SnowflakeConstant.MAX_WORKER_ID + 1);
        info.setDataCenterId(dc);
        info.setWorkerId(wk);
    }

    /**
     * 计算本机 IP 的候选槽位数组（主槽位 + 邻近 8 个偏移槽位）
     */
    private int[] computePreferredSlots(String ip) {
        int totalSlots = (int) ((SnowflakeConstant.MAX_DATA_CENTER_ID + 1) * (SnowflakeConstant.MAX_WORKER_ID + 1));
        int base = Math.floorMod(ip.hashCode(), totalSlots);
        int[] slots = new int[9];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = Math.floorMod(base + i, totalSlots);
        }
        return slots;
    }

    private boolean contains(int[] array, int value) {
        for (int item : array) {
            if (item == value) {
                return true;
            }
        }
        return false;
    }
}
