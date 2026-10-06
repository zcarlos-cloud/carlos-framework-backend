package com.carlos.system.news.apiimpl;

import com.carlos.core.response.Result;
import com.carlos.system.api.ApiSysNews;
import com.carlos.system.news.convert.SysNewsConvert;
import com.carlos.system.news.manager.SysNewsManager;
import com.carlos.system.news.pojo.dto.SysNewsDTO;
import com.carlos.system.pojo.ao.SysNewsAO;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 公告/站内通知 api接口（Feign 实现）
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/news")
@Tag(name = "公告Feign接口")
@Hidden
public class ApiSysNewsImpl implements ApiSysNews {

    private final SysNewsManager newsManager;

    @Override
    public Result<SysNewsAO> getById(Long id) {
        SysNewsDTO dto = newsManager.getDtoById(id);
        return Result.success(SysNewsConvert.INSTANCE.toAO(dto));
    }

}
