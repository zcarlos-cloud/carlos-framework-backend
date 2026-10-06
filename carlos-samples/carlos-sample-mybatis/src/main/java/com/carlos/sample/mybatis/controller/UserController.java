package com.carlos.sample.mybatis.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.carlos.core.response.Result;
import com.carlos.sample.mybatis.entity.User;
import com.carlos.sample.mybatis.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户管理 Controller
 * 演示 MyBatis-Plus 的 CRUD 操作
 * </p>
 *
 * @author carlos
 * @date 2026/3/15
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 结果
     */
    @PostMapping
    public Result<Boolean> create(@RequestBody User user) {
        int result = userMapper.insert(user);
        return Result.success(result > 0);
    }

    /**
     * 根据ID删除用户（逻辑删除）
     *
     * @param id 用户ID
     * @return 结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        int result = userMapper.deleteById(id);
        return Result.success(result > 0);
    }

    /**
     * 根据ID更新用户
     *
     * @param user 用户信息
     * @return 结果
     */
    @PutMapping
    public Result<Boolean> update(@RequestBody User user) {
        int result = userMapper.updateById(user);
        return Result.success(result > 0);
    }

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        return Result.success(user);
    }

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    @GetMapping("/list")
    public Result<List<User>> list() {
        List<User> list = userMapper.selectList(null);
        return Result.success(list);
    }

    /**
     * 根据状态查询用户（注解方式 SQL）
     *
     * @param status 状态
     * @return 用户列表
     */
    @GetMapping("/status/{status}")
    public Result<List<User>> listByStatus(@PathVariable Integer status) {
        List<User> list = userMapper.selectByStatus(status);
        return Result.success(list);
    }

    /**
     * 根据用户名模糊查询（XML 方式 SQL）
     *
     * @param username 用户名
     * @return 用户列表
     */
    @GetMapping("/search")
    public Result<List<User>> searchByUsername(@RequestParam String username) {
        List<User> list = userMapper.selectByUsernameLike(username);
        return Result.success(list);
    }

    /**
     * 使用 LambdaQueryWrapper 条件查询
     *
     * @param gender   性别
     * @param status   状态
     * @return 用户列表
     */
    @GetMapping("/search/lambda")
    public Result<List<User>> searchByLambda(
        @RequestParam(required = false) Integer gender,
        @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(gender != null, User::getGender, gender)
            .eq(status != null, User::getStatus, status)
            .orderByDesc(User::getCreateTime);
        List<User> list = userMapper.selectList(wrapper);
        return Result.success(list);
    }
}
