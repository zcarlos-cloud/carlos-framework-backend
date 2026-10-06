package com.carlos.auth.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.auth.app.pojo.entity.AppClient;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 应用信息 查询接口
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@Mapper
public interface AppClientMapper extends BaseMapper<AppClient> {


}
