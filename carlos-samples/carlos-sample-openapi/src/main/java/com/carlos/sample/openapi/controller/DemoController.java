package com.carlos.sample.openapi.controller;

import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * OpenAPI 演示 Controller
 * 演示各种 Swagger/Knife4j 注解的使用
 * </p>
 *
 * @author carlos
 * @date 2026/3/15
 */
@Tag(name = "演示接口", description = "用于演示 Swagger/Knife4j 注解的各种使用场景")
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /**
     * 简单的 GET 请求示例
     */
    @Operation(
        summary = "获取用户信息",
        description = "根据用户ID获取用户详细信息",
        parameters = {
            @Parameter(name = "id", description = "用户ID", required = true, in = ParameterIn.PATH)
        }
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = UserVO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/user/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO user = new UserVO();
        user.setId(id);
        user.setName("张三");
        user.setAge(25);
        user.setEmail("zhangsan@example.com");
        return Result.success(user);
    }

    /**
     * POST 请求示例 - 创建资源
     */
    @Operation(
        summary = "创建用户",
        description = "创建一个新用户，返回创建后的用户信息"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "用户信息",
        required = true,
        content = @Content(schema = @Schema(implementation = UserCreateParam.class))
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功", content = @Content(schema = @Schema(implementation = UserVO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/user")
    public Result<UserVO> createUser(@RequestBody UserCreateParam param) {
        UserVO user = new UserVO();
        user.setId(1L);
        user.setName(param.getName());
        user.setAge(param.getAge());
        user.setEmail(param.getEmail());
        return Result.success(user);
    }

    /**
     * PUT 请求示例 - 更新资源
     */
    @Operation(
        summary = "更新用户",
        description = "根据用户ID更新用户信息"
    )
    @Parameters({
        @Parameter(name = "id", description = "用户ID", required = true, in = ParameterIn.PATH),
        @Parameter(name = "param", description = "用户更新信息", required = true)
    })
    @PutMapping("/user/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id, @RequestBody UserUpdateParam param) {
        UserVO user = new UserVO();
        user.setId(id);
        user.setName(param.getName());
        user.setAge(param.getAge());
        user.setEmail(param.getEmail());
        return Result.success(user);
    }

    /**
     * DELETE 请求示例
     */
    @Operation(
        summary = "删除用户",
        description = "根据用户ID删除用户"
    )
    @Parameter(name = "id", description = "用户ID", required = true, in = ParameterIn.PATH)
    @DeleteMapping("/user/{id}")
    public Result<Boolean> deleteUser(@PathVariable Long id) {
        return Result.success(true);
    }

    /**
     * 查询参数示例
     */
    @Operation(
        summary = "搜索用户",
        description = "根据条件搜索用户列表，支持分页"
    )
    @Parameters({
        @Parameter(name = "keyword", description = "搜索关键词", in = ParameterIn.QUERY),
        @Parameter(name = "status", description = "用户状态：0-禁用，1-启用", in = ParameterIn.QUERY, example = "1"),
        @Parameter(name = "pageNum", description = "页码，从1开始", in = ParameterIn.QUERY, example = "1"),
        @Parameter(name = "pageSize", description = "每页条数", in = ParameterIn.QUERY, example = "10")
    })
    @GetMapping("/user/search")
    public Result<List<UserVO>> searchUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "1") Integer status,
        @RequestParam(required = false, defaultValue = "1") Integer pageNum,
        @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(List.of());
    }

    /**
     * 批量操作示例
     */
    @Operation(
        summary = "批量删除用户",
        description = "根据ID列表批量删除用户"
    )
    @Parameter(name = "ids", description = "用户ID列表", required = true, example = "[1,2,3]")
    @PostMapping("/user/batch-delete")
    public Result<Boolean> batchDeleteUsers(@RequestBody List<Long> ids) {
        return Result.success(true);
    }

    /**
     * 响应实体类 - 用户视图对象
     */
    @Data
    @Schema(description = "用户信息")
    public static class UserVO {

        @Schema(description = "用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long id;

        @Schema(description = "用户名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "年龄", example = "25", minimum = "0", maximum = "150")
        private Integer age;

        @Schema(description = "邮箱", example = "zhangsan@example.com")
        private String email;

        @Schema(description = "用户状态：0-禁用，1-启用", example = "1", allowableValues = {"0", "1"})
        private Integer status;

        @Schema(description = "创建时间", example = "2026-03-15 10:30:00")
        private String createTime;
    }

    /**
     * 请求参数类 - 创建用户
     */
    @Data
    @Schema(description = "创建用户参数")
    public static class UserCreateParam {

        @Schema(description = "用户名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "年龄", example = "25", minimum = "0", maximum = "150")
        private Integer age;

        @Schema(description = "邮箱", example = "zhangsan@example.com")
        private String email;
    }

    /**
     * 请求参数类 - 更新用户
     */
    @Data
    @Schema(description = "更新用户参数")
    public static class UserUpdateParam {

        @Schema(description = "用户名", example = "张三")
        private String name;

        @Schema(description = "年龄", example = "25", minimum = "0", maximum = "150")
        private Integer age;

        @Schema(description = "邮箱", example = "zhangsan@example.com")
        private String email;

        @Schema(description = "用户状态：0-禁用，1-启用", example = "1", allowableValues = {"0", "1"})
        private Integer status;
    }
}
