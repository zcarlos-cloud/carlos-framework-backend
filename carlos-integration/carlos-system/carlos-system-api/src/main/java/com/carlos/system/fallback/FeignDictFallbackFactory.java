package com.carlos.system.fallback;


import com.carlos.core.base.Dict;
import com.carlos.core.response.Result;
import com.carlos.system.api.ApiDict;
import com.carlos.system.pojo.ao.DictItemAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;

/**
 * <p>
 * 用户降级服务
 * </p>
 *
 * @author Carlos
 * @date 2022/11/16 10:57
 */

@Slf4j
public class FeignDictFallbackFactory implements FallbackFactory<ApiDict> {

    @Override
    public ApiDict create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("字典服务调用失败: message:{}", message);
        return new ApiDict() {


            @Override
            public Result<Dict> getById(Long id) {
                return Result.error();
            }

            @Override
            public Result<Dict> getByCode(String code) {
                return Result.error();
            }

            @Override
            public Result<List<DictItemAO>> list(String code) {
                return Result.error();
            }
        };
    }
}
