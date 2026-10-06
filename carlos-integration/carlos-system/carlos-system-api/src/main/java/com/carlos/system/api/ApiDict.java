package com.carlos.system.api;


import com.carlos.core.base.Dict;
import com.carlos.core.response.Result;
import com.carlos.system.ServiceNameConstant;
import com.carlos.system.fallback.FeignDictFallbackFactory;
import com.carlos.system.pojo.ao.DictItemAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>
 * 系统字典详情 rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@FeignClient(value = ServiceNameConstant.SYSTEM, contextId = "dict", path = "api/sys/dict", fallbackFactory = FeignDictFallbackFactory.class)
public interface ApiDict {

    /**
     * 根据字典id获取字典选项详情
     *
     * @param id 字典id
     * @return com.carlos.voice.common.dto.sys.DictItemDTO
     * @author carlos
     * @date 2021/12/20 17:29
     */
    @GetMapping("item/id")
    Result<Dict> getById(@RequestParam(value = "id") Long id);

    /**
     * 根据code获取字典选项详情
     *
     * @param code 字典code
     * @return com.carlos.voice.common.dto.sys.DictItemDTO
     * @author carlos
     * @date 2021/12/20 17:29
     */
    @GetMapping("item/code")
    Result<Dict> getByCode(@RequestParam(value = "code") String code);

    /**
     * 获取字典所有选项
     *
     * @param code 参数0
     * @return java.util.List<com.carlos.voice.common.dto.sys.DictItemDTO>
     * @author carlos
     * @date 2022/6/1 10:58
     */
    @GetMapping("item/list")
    Result<List<DictItemAO>> list(@RequestParam(value = "code") String code);

}
