package com.carlos.auth.mfa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.auth.mfa.pojo.entity.TrustedDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 可信设备 Mapper 接口
 * </p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Mapper
public interface TrustedDeviceMapper extends BaseMapper<TrustedDevice> {

    /**
     * 删除用户的所有设备
     *
     * @param userId 用户ID
     * @return 删除数量
     */
    int deleteByUserId(@Param("userId") Long userId);
}
