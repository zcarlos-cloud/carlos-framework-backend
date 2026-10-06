package com.carlos.org.position.api;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.ServiceNameConstant;
import com.carlos.org.position.api.fallback.ApiOrgPositionFallbackFactory;
import com.carlos.org.position.api.pojo.ao.OrgPositionAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 岗位表 feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "position", path = "/api/org/position", fallbackFactory = ApiOrgPositionFallbackFactory.class)
public interface ApiOrgPosition {

    /**
     * 分页查询岗位选择项
     *
     * <p>供不依赖 carlos-org 的模块（如工作流的办理人选择器，办理人写成 {@code position:7}）按关键字挑选岗位，
     * 条件语义与 {@code OrgPositionManager#getPage} 一致：名称、编码都按<b>模糊</b>匹配，
     * 条件之间是 AND，两参数都可为空（为空即不过滤）。
     * 数据权限范围由 carlos-org 侧按当前登录上下文施加。</p>
     *
     * @param positionName 岗位名称（模糊，可空）
     * @param positionCode 岗位编码（模糊，可空）
     * @param pageNum      页码，从 1 开始（小于 1 时按 1 处理）
     * @param pageSize     每页条数（小于 1 时按 10 处理）
     * @return 分页后的岗位列表
     */
    @GetMapping("/pagePositionOptions")
    Result<Paging<OrgPositionAO>> pagePositionOptions(@RequestParam(value = "positionName", required = false) String positionName,
                                                      @RequestParam(value = "positionCode", required = false) String positionCode,
                                                      @RequestParam("pageNum") Integer pageNum,
                                                      @RequestParam("pageSize") Integer pageSize);

    /**
     * 按主键批量查询岗位
     *
     * <p>供「已选中的岗位回显名称」这类场景批量取数，避免逐个调用（N+1）。
     * 空集合、不存在的 id 均不报错，返回结果只包含实际存在的岗位；
     * <b>不保证入参顺序</b>，调用方按 id 自行建映射。</p>
     *
     * @param ids 岗位ID集合（建议单次不超过 500 个，避免 GET 查询串过长）
     * @return 岗位列表
     */
    @GetMapping("/listPositionOptions")
    Result<List<OrgPositionAO>> listPositionOptions(@RequestParam("ids") Collection<Long> ids);

}
