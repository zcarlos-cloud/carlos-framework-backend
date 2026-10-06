package com.carlos.sample.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.sample.mybatis.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 用户 Mapper 接口
 * </p>
 *
 * @author carlos
 * @date 2026/3/15
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据状态查询用户列表（注解方式）
     *
     * @param status 状态
     * @return 用户列表
     */
    @Select("SELECT * FROM sys_user WHERE status = #{status} AND is_deleted = 0")
    List<User> selectByStatus(@Param("status") Integer status);

    /**
     * 根据用户名模糊查询（XML方式）
     *
     * @param username 用户名
     * @return 用户列表
     */
    List<User> selectByUsernameLike(@Param("username") String username);
}
