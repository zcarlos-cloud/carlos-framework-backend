package com.carlos.system.favorite.controller;

import com.carlos.system.favorite.convert.SysFavoriteConvert;
import com.carlos.system.favorite.pojo.param.FavoriteReplaceParam;
import com.carlos.system.favorite.pojo.param.FavoriteToggleParam;
import com.carlos.system.favorite.pojo.vo.SysFavoriteVO;
import com.carlos.system.favorite.service.SysFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 通用用户收藏 rest服务接口
 * </p>
 *
 * <p>任意业务按 targetType + targetId 收藏内容，数据按 user_id 强隔离，只能操作当前登录人自己的收藏。</p>
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>不加 @PreAuthorize：收藏是用户私有数据，所有查询/变更都强制带上当前登录人 user_id，
 *       天然无法触碰他人数据；资源服务器已要求登录，无需再按权限点收敛。</li>
 *   <li>不加 @Log 审计注解：收藏添加/取消/覆盖是高频用户动作，打审计日志只会制造噪音。</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/favorite")
@Tag(name = "通用收藏")
public class SysFavoriteController {

    private final SysFavoriteService favoriteService;

    @GetMapping("list")
    @Operation(summary = "我的收藏列表（按排序）")
    public List<SysFavoriteVO> list(@RequestParam String targetType) {
        return SysFavoriteConvert.INSTANCE.toVOList(this.favoriteService.listMy(targetType));
    }

    @GetMapping("all")
    @Operation(summary = "我的全部收藏（跨类型，收藏管理用）")
    public List<SysFavoriteVO> all() {
        return SysFavoriteConvert.INSTANCE.toVOList(this.favoriteService.listAll());
    }

    @GetMapping("check")
    @Operation(summary = "批量校验收藏状态")
    public Set<String> check(@RequestParam String targetType, @RequestParam String targetIds) {
        List<String> ids = Arrays.stream(targetIds.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        return this.favoriteService.checkFavorited(targetType, ids);
    }

    @PostMapping("replace")
    @Operation(summary = "覆盖保存某类型的全部收藏（整体排序）")
    public boolean replace(@RequestBody @Validated FavoriteReplaceParam param) {
        return this.favoriteService.replace(param.getTargetType(), param.getTargetIds());
    }

    @PostMapping("add")
    @Operation(summary = "添加收藏（幂等）")
    public boolean add(@RequestBody @Validated FavoriteToggleParam param) {
        return this.favoriteService.add(param.getTargetType(), param.getTargetId());
    }

    @PostMapping("remove")
    @Operation(summary = "取消收藏")
    public boolean remove(@RequestBody @Validated FavoriteToggleParam param) {
        return this.favoriteService.remove(param.getTargetType(), param.getTargetId());
    }

}
