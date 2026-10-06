package com.carlos.system.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.carlos.core.base.Dict;
import com.carlos.core.base.RegionInfo;
import com.carlos.core.response.Result;
import com.carlos.system.api.ApiDict;
import com.carlos.system.api.ApiRegion;
import com.carlos.system.api.ApiSequence;
import com.carlos.system.api.ApiSysMenu;
import com.carlos.system.api.ApiSysNews;
import com.carlos.system.api.ApiSystemConfig;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.pojo.ao.DictItemAO;
import com.carlos.system.pojo.ao.SequenceBatchAO;
import com.carlos.system.pojo.ao.SysConfigAO;
import com.carlos.system.pojo.ao.SysMenuAO;
import com.carlos.system.pojo.ao.SysNewsAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.List;

/**
 * <p>
 * 系统服务静态门面（carlos-system 对外能力的免注入入口）
 * </p>
 *
 * <p>把 {@code carlos-system-api} 里高频的只读查询与序列号生成收敛成静态方法，省掉每个调用点
 * 注入 Feign 接口 + 拆 {@link Result} 的样板代码：</p>
 *
 * <pre>{@code
 * // 以前
 * private final ApiDict apiDict;
 * ...
 * Result<Dict> r = apiDict.getByCode(code);
 * if (!r.isSuccess()) { ... }
 * Dict dict = r.getData();
 *
 * // 现在
 * Dict dict = SystemUtil.getDictItemByCode(code);
 * }</pre>
 *
 * <p><b>只收录高频的幂等只读查询与序列号生成。</b>写操作（文件上传/删除、区域新增等）
 * <b>刻意不收录</b> —— {@code SystemUtil.deleteFile(id)} 会把「一次远程写、可能删掉别人数据」
 * 藏得像个本地工具方法，调用点看不出有网络副作用。要写请直接注入对应的 {@code Api*} 接口。</p>
 *
 * <p><b>⚠️ 禁止在 carlos-system 模块内部使用本类。</b>这些方法走 Feign，在 carlos-system 里调用
 * 会把一次本地方法调用变成「HTTP 打自己」，既多一跳又引入本不存在的失败面。
 * carlos-system 内部请直接注入 {@code SysSequenceRuleService} / {@code SysDictService} 等本地服务。
 * 注意 cloud 模式下这个违规<b>不会报错</b>（{@code carlos-system-cloud} 自己也注册了
 * {@code Api*} 客户端），只会静默多一跳 —— 所以别指望它响亮地失败。</p>
 *
 * <p><b>失败语义（改动前先读 —— 这里有两条独立通道，容易只想到其中一条）：</b></p>
 *
 * <p><b>通道 A（生产上的实际路径）：远端返回非 2xx ⇒ 抛 {@code OpenFeignException}。</b>
 * 服务端抛 {@code BusinessException}（如规则不存在的 20981）时 HTTP 状态码非 2xx，
 * {@code FeignClientErrorDecoder} 在解码前就把响应体转成 {@code OpenFeignException} 抛出来，
 * <b>本类拿不到 {@code Result}，也不做任何转换</b>，异常原样上抛。</p>
 *
 * <p>注意该异常的特征：错误码被保留（如 20981），文案也由
 * {@code FeignClientErrorDecoder} 从 {@code Result} 的 {@code msg} 正确取出
 * （此前只读 {@code message} 字段、导致文案一律退化成「openfeign调用异常」，
 * 已于 2026-09-27 修复）。</p>
 *
 * <p><b>通道 B（防御性，当前部署下不可达）：{@code Result.success=false} ⇒ 抛
 * {@code BusinessException}。</b>这条只在熔断降级生效时才会走到 —— 前提是容器里装配了
 * {@code FeignCircuitBreakerTargeter}（需开启 Sentinel / circuitbreaker）。当前
 * {@code feignTargeter} 是 {@code DefaultTargeter}，它<b>不读</b> {@code fallbackFactory}，
 * 所以各 {@code Api*FallbackFactory} 实际上从未生效。保留这条分支是为了将来打开熔断时
 * 不会静默降级成 null。</p>
 *
 * <p>无论走哪条通道，失败都<b>以异常上抛</b>，不会静默返回 null 让调用方写脏数据。</p>
 *
 * <p><b>成功路径的拆包规则（这两条已逐端点核对成立）：</b></p>
 * <table border="1">
 *   <caption>success=true 时 data 的处理</caption>
 *   <tr><th>场景</th><th>本类行为</th><th>理由</th></tr>
 *   <tr>
 *     <td>读接口 data 为 null</td>
 *     <td>返回 null</td>
 *     <td>服务端语义就是「查不到」—— 见 {@code ApiDictImpl#getByCode} 的
 *         {@code Result.success(dict)}，查不到不报错</td>
 *   </tr>
 *   <tr>
 *     <td>序列号 data 为空白</td>
 *     <td><b>抛 {@code SYS_SEQUENCE_GENERATE_FAILED}</b></td>
 *     <td>生成器的成功路径不可能产出空编码；返回 null 等于让调用方把空单号写进库</td>
 *   </tr>
 * </table>
 *
 * <p><b>Bean 取不到时</b>抛 {@code SYS_API_BEAN_MISSING} 并带上排查指引 —— 最常见原因是当前服务
 * 没有把 {@code com.carlos.system.api} 纳入 {@code @EnableFeignClients} 的扫描范围。</p>
 *
 * <p>参数合法性<b>不在本类重复校验</b>：空白 ruleCode 由服务端按既有错误码
 * （{@code SYS_PARAM_SEQUENCE_RULE_CODE_EMPTY}）拒绝，经通道 A 以异常上抛（错误码保留、
 * 文案丢失，见上）。在这里再校验一遍等于把同一业务规则写在两处，迟早漂移。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
public final class SystemUtil {

    private SystemUtil() {
    }

    /**
     * 生成下一个业务编码
     *
     * <p>每调用一次真实消耗一个号段，不可撤销（跳号是序列号系统的既定语义）。
     * 规则须先在「系统管理 → 编码规则」维护好，否则服务端抛「编码规则不存在」。</p>
     *
     * <p>⚠️ 禁止在 carlos-system 内部调用（会变 HTTP 打自己），请直调 {@code SysSequenceRuleService}。</p>
     *
     * @param ruleCode 规则编码（对应 {@code sys_sequence_rule.rule_code}，如 order_no）
     * @return 格式化后的业务编码，**保证非空**
     */
    public static String nextCode(String ruleCode) {
        String code = unwrap(api(ApiSequence.class).next(ruleCode), "生成业务编码 ruleCode=" + ruleCode);
        if (StrUtil.isBlank(code)) {
            throw SystemErrorCode.SYS_SEQUENCE_GENERATE_FAILED.exception(
                "生成器对规则 %s 返回了空编码：拒绝把空单号交给调用方", ruleCode);
        }
        return code;
    }

    /**
     * 批量预取号段
     *
     * <p>一次自增 count × step 并返回段首/段尾编码。段内编码需在当日期内用完，
     * 跨日期段预取会浪费号段。</p>
     *
     * <p>⚠️ 禁止在 carlos-system 内部调用（会变 HTTP 打自己），请直调 {@code SysSequenceRuleService}。</p>
     *
     * @param ruleCode 规则编码
     * @param count    预取数量（1~1000）
     * @return 号段信息，保证非空
     */
    public static SequenceBatchAO nextCodeBatch(String ruleCode, int count) {
        SequenceBatchAO batch = unwrap(api(ApiSequence.class).nextBatch(ruleCode, count),
            "批量预取业务编码 ruleCode=" + ruleCode);
        // 与 nextCode 同一条不变量：段首/段尾为空同样等于把空单号交给调用方，故一并拦下
        if (batch == null || StrUtil.isBlank(batch.getStartCode()) || StrUtil.isBlank(batch.getEndCode())) {
            throw SystemErrorCode.SYS_SEQUENCE_GENERATE_FAILED.exception(
                "生成器对规则 %s 返回了空号段（或段首/段尾为空）", ruleCode);
        }
        return batch;
    }

    /**
     * 按字典选项 id 取字典选项
     *
     * <p>⚠️ 禁止在 carlos-system 内部调用（会变 HTTP 打自己），请直调 {@code SysDictService}。</p>
     *
     * @param itemId 字典选项 id
     * @return 字典选项，查不到时返回 null
     */
    public static Dict getDictItemById(Long itemId) {
        return unwrap(api(ApiDict.class).getById(itemId), "查询字典选项 itemId=" + itemId);
    }

    /**
     * 按字典选项编码取字典选项
     *
     * <p>⚠️ 禁止在 carlos-system 内部调用（会变 HTTP 打自己），请直调 {@code SysDictService}。</p>
     *
     * @param itemCode 字典选项编码
     * @return 字典选项，查不到时返回 null
     */
    public static Dict getDictItemByCode(String itemCode) {
        return unwrap(api(ApiDict.class).getByCode(itemCode), "查询字典选项 itemCode=" + itemCode);
    }

    /**
     * 取某个字典下的全部选项
     *
     * <p>⚠️ 禁止在 carlos-system 内部调用（会变 HTTP 打自己），请直调 {@code SysDictItemService}。</p>
     *
     * @param dictCode 字典编码（字典本身的 code，非选项 code）
     * @return 选项列表，空字典返回空列表
     */
    public static List<DictItemAO> listDictItems(String dictCode) {
        return unwrap(api(ApiDict.class).list(dictCode), "查询字典选项列表 dictCode=" + dictCode);
    }

    /**
     * 按配置编码取系统配置
     *
     * <p>⚠️ 禁止在 carlos-system 内部调用（会变 HTTP 打自己），请直调本地配置 Service。</p>
     *
     * @param configCode 配置编码
     * @return 配置项，查不到时返回 null
     */
    public static SysConfigAO getConfigByCode(String configCode) {
        return unwrap(api(ApiSystemConfig.class).getByCode(configCode), "查询系统配置 configCode=" + configCode);
    }

    /**
     * 按 id 取菜单
     *
     * <p>⚠️ 禁止在 carlos-system 内部调用（会变 HTTP 打自己），请直调 {@code SysMenuManager}。</p>
     *
     * @param menuId 菜单 id
     * @return 菜单，查不到时返回 null
     */
    public static SysMenuAO getMenuById(Long menuId) {
        return unwrap(api(ApiSysMenu.class).getMenuById(menuId), "查询菜单 menuId=" + menuId);
    }

    /**
     * 按 id 取公告
     *
     * <p>⚠️ 禁止在 carlos-system 内部调用（会变 HTTP 打自己），请直调 {@code SysNewsService}。</p>
     *
     * @param newsId 公告 id
     * @return 公告，查不到时返回 null
     */
    public static SysNewsAO getNewsById(Long newsId) {
        return unwrap(api(ApiSysNews.class).getById(newsId), "查询公告 newsId=" + newsId);
    }

    /**
     * 取区域信息（含上级链）
     *
     * <p>⚠️ 禁止在 carlos-system 内部调用（会变 HTTP 打自己），请直调本地区域 Service。</p>
     *
     * @param regionCode 区域编码
     * @param limit      级数限制，0 代表不限制，null 仅当前级
     * @return 区域信息，查不到时返回 null
     */
    public static RegionInfo getRegionInfo(String regionCode, Integer limit) {
        return unwrap(api(ApiRegion.class).getRegionInfo(regionCode, limit), "查询区域 regionCode=" + regionCode);
    }

    /**
     * 取 Feign 客户端实例
     *
     * <p>每次调用都从 Spring 容器取，不做静态缓存 —— 既与仓内既有写法一致
     * （{@code SmsCodeIdentityProvider} 里的 {@code SpringUtil.getBean(ApiOrgUser.class)}），
     * 也避免容器刷新后缓存指向失效实例。</p>
     *
     * @param apiType Feign 接口类型
     * @param <T>     接口类型
     * @return 客户端实例
     */
    private static <T> T api(Class<T> apiType) {
        if (SpringUtil.getApplicationContext() == null) {
            throw SystemErrorCode.SYS_API_BEAN_MISSING.exception(
                "Spring 上下文尚未就绪，无法取得 %s：请确认当前服务已引入 carlos-spring-boot-starter-web"
                    + "（静态取 Bean 通道由它的 @EnableSpringUtil 装配），且不是在容器启动完成前就调用了 SystemUtil",
                apiType.getSimpleName());
        }
        try {
            return SpringUtil.getBean(apiType);
        } catch (NoSuchBeanDefinitionException e) {
            // 现场必须落到日志：ErrorCode.exception 只能带文案，带不了 cause，
            // 不 log 的话「没装配」与「装配了但没扫到」在日志里无从区分
            log.warn("SystemUtil 取不到 Feign 客户端 {}：{}", apiType.getSimpleName(), e.getMessage());
            throw SystemErrorCode.SYS_API_BEAN_MISSING.exception(
                "取不到 %s：请确认当前服务已启用该 Feign 客户端，"
                    + "@EnableFeignClients 的扫描范围需覆盖 com.carlos.system.api",
                apiType.getSimpleName());
        } catch (BeansException e) {
            // 不唯一的 Bean、创建失败的 Bean 等 —— 与「压根没有」是两回事，提示不能混为一谈
            log.error("SystemUtil 获取 Feign 客户端 {} 失败", apiType.getSimpleName(), e);
            throw SystemErrorCode.SYS_API_BEAN_MISSING.exception(
                "获取 %s 失败（%s）：应用上下文异常，请查看启动日志",
                apiType.getSimpleName(), e.getClass().getSimpleName());
        }
    }

    /**
     * 拆包：失败抛异常，成功返回 data（可能为 null）
     *
     * @param result 远端返回
     * @param what   调用描述，用于拼错误消息
     * @param <T>    data 类型
     * @return data，可能为 null
     */
    private static <T> T unwrap(Result<T> result, String what) {
        if (result == null) {
            throw SystemErrorCode.SYS_API_INVOKE_FAILED.exception("%s：远端返回空响应", what);
        }
        if (!result.isSuccess()) {
            // 远端错误码（如 20981 规则不存在）原样带进消息：只抛 50911 会让调用方无法区分
            // 「规则不存在」与「下游挂了」。需要程序化区分错误码请直接注入 Api* 拿 Result。
            throw SystemErrorCode.SYS_API_INVOKE_FAILED.exception("%s：%s（远端错误码 %s）",
                what, result.getMsg(), result.getCode());
        }
        return result.getData();
    }
}
