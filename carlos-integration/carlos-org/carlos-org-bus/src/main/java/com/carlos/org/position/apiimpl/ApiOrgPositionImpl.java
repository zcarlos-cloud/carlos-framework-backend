package com.carlos.org.position.apiimpl;


import cn.hutool.core.collection.CollUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.position.api.ApiOrgPosition;
import com.carlos.org.position.api.pojo.ao.OrgPositionAO;
import com.carlos.org.position.convert.OrgPositionConvert;
import com.carlos.org.position.manager.OrgPositionManager;
import com.carlos.org.position.pojo.entity.OrgPosition;
import com.carlos.org.position.pojo.param.OrgPositionPageParam;
import com.carlos.org.position.pojo.vo.OrgPositionVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 岗位表 api接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/position")
@Tag(name = "岗位表Feign接口")
public class ApiOrgPositionImpl implements ApiOrgPosition {

    private final OrgPositionManager positionManager;

    @Override
    public Result<Paging<OrgPositionAO>> pagePositionOptions(@RequestParam("positionName") String positionName,
                                                             @RequestParam("positionCode") String positionCode,
                                                             @RequestParam("pageNum") Integer pageNum,
                                                             @RequestParam("pageSize") Integer pageSize) {
        OrgPositionPageParam param = new OrgPositionPageParam();
        param.setPositionName(positionName);
        param.setPositionCode(positionCode);
        // 0 是「未指定」的哨兵：ParamPage 的 setter 会把非正数归一成默认值（页码 1 / 每页 10）
        param.setCurrent(pageNum == null ? 0 : pageNum);
        param.setSize(pageSize == null ? 0 : pageSize);
        Paging<OrgPositionVO> paging = positionManager.getPage(param);
        Paging<OrgPositionAO> result = new Paging<>();
        result.setTotal(paging.getTotal());
        result.setCurrent(paging.getCurrent());
        result.setSize(paging.getSize());
        result.setPages(paging.getPages());
        result.setRecords(OrgPositionConvert.INSTANCE.toAOListFromVo(paging.getRecords()));
        return Result.success(result);
    }

    @Override
    public Result<List<OrgPositionAO>> listPositionOptions(@RequestParam("ids") Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Result.success(List.of());
        }
        // 一次批量取回，避免调用方按 id 逐个回查（N+1）；顺序不保证，调用方按 id 建映射
        List<OrgPosition> positions = positionManager.listByIds(ids);
        return Result.success(OrgPositionConvert.INSTANCE.toAOList(positions.stream()
            .map(OrgPositionConvert.INSTANCE::toDTO)
            .toList()));
    }
}
