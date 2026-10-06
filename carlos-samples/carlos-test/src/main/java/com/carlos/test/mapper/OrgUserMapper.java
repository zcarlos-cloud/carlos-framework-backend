package com.carlos.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.test.pojo.entity.OrgUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 系统用户 查询接口
 * </p>
 *
 * @author Carlos
 * @date 2023-8-12 11:16:18
 */
@Mapper
public interface OrgUserMapper extends BaseMapper<OrgUser> {


}
