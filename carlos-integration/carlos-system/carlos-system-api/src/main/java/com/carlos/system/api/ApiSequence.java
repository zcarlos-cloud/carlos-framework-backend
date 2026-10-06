package com.carlos.system.api;

import com.carlos.core.response.Result;
import com.carlos.system.ServiceNameConstant;
import com.carlos.system.fallback.ApiSequenceFallbackFactory;
import com.carlos.system.pojo.ao.SequenceBatchAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 业务编码/序列号 feign 提供接口
 * </p>
 *
 * <p>S5 序列号：按规则编码生成业务单号，规则在「系统管理 → 编码规则」维护。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@FeignClient(value = ServiceNameConstant.SYSTEM, contextId = "sequence",
    fallbackFactory = ApiSequenceFallbackFactory.class, path = "/api/sys/sequence")
public interface ApiSequence {

    /**
     * 单次批量预取的数量上限
     */
    int MAX_BATCH_SIZE = 1000;

    /**
     * 生成下一个编码
     *
     * <p>规则不存在时服务端抛 {@code SYS_SEQUENCE_RULE_NOT_FOUND(20981)}、溢出时抛
     * {@code SYS_SEQUENCE_OVERFLOW(20985)}，本客户端<b>不会</b>把它们变成
     * {@code Result.success(null)} —— 失败一律以异常上抛（见 {@code FeignClientErrorDecoder}）。</p>
     *
     * @param ruleCode 规则编码（如 order_no）
     * @return 格式化后的业务编码，成功路径保证非空
     */
    @GetMapping("next")
    Result<String> next(@RequestParam("ruleCode") String ruleCode);

    /**
     * 批量预取号段
     *
     * <p>一次自增 count × step 并返回段首/段尾编码；段内编码需在当日期内用完，
     * 跨日期段预取会浪费号段（重启丢段同理，见模块文档）。</p>
     *
     * @param ruleCode 规则编码
     * @param count    预取数量（1~1000）
     * @return 号段信息，成功路径保证非空
     */
    @GetMapping("batch")
    Result<SequenceBatchAO> nextBatch(@RequestParam("ruleCode") String ruleCode, @RequestParam("count") int count);

}
