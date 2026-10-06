package com.carlos.system.sequence.controller;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.sequence.convert.SysSequenceRuleConvert;
import com.carlos.system.sequence.manager.SysSequenceRuleManager;
import com.carlos.system.sequence.pojo.dto.SysSequenceRuleDTO;
import com.carlos.system.sequence.pojo.param.SysSequenceRuleCreateParam;
import com.carlos.system.sequence.pojo.param.SysSequenceRulePageParam;
import com.carlos.system.sequence.pojo.param.SysSequenceRuleUpdateParam;
import com.carlos.system.sequence.pojo.vo.SysSequenceRuleVO;
import com.carlos.system.sequence.service.SysSequenceRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 业务编码/序列号规则 rest服务接口
 * </p>
 *
 * <p>所有端点均要求登录并持有对应权限点（方法级 {@code @PreAuthorize} 鉴权，权限码由资源
 * 扫描器写入 sys_operation）：查询类接口走 {@code manage:sequence-rule:view}，增删改分别走
 * {@code create}/{@code update}/{@code delete}；{@code POST /sys/sequence/next} 会消耗
 * 号段（Redis 计数 + 数据库水位的持久性变更），单独走 {@code manage:sequence-rule:generate}。</p>
 *
 * <p>对某条无日期段的规则连打 {@code /next} 即可把流水推到 {@code SYS_SEQUENCE_OVERFLOW}，
 * 且 {@code reset_cycle=1} 的规则等日期翻滚也恢复不了，只能改配置——授权时应对该权限点
 * 收紧授权范围。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/sequence")
@Tag(name = "编码规则")
public class SysSequenceRuleController {

    public static final String BASE_NAME = "编码规则";

    private final SysSequenceRuleService sequenceRuleService;

    private final SysSequenceRuleManager sequenceRuleManager;

    @Log(
        title = "编码规则",
        logType = "SYS_SEQUENCE_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增编码规则: ' + #param.ruleName",
        targetType = "SYS_SEQUENCE",
        riskLevel = 30)
    @PreAuthorize("hasPermission('manage:sequence', 'create')")
    @PostMapping
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated SysSequenceRuleCreateParam param) {
        SysSequenceRuleDTO dto = SysSequenceRuleConvert.INSTANCE.toDTO(param);
        this.sequenceRuleService.addRule(dto);
    }

    @Log(
        title = "编码规则",
        logType = "SYS_SEQUENCE_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除编码规则: ' + #param.ids",
        targetType = "SYS_SEQUENCE",
        targetId = "#param.ids",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:sequence', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        this.sequenceRuleService.deleteRules(param.getIds());
    }

    @Log(
        title = "编码规则",
        logType = "SYS_SEQUENCE_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改编码规则: ' + #param.ruleName",
        targetType = "SYS_SEQUENCE",
        targetId = "#param.id",
        riskLevel = 30)
    @PreAuthorize("hasPermission('manage:sequence', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated SysSequenceRuleUpdateParam param) {
        SysSequenceRuleDTO dto = SysSequenceRuleConvert.INSTANCE.toDTO(param);
        this.sequenceRuleService.updateRule(dto);
    }

    @PreAuthorize("hasPermission('manage:sequence', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public SysSequenceRuleVO detail(@RequestParam Long id) {
        SysSequenceRuleDTO dto = this.sequenceRuleManager.getDtoById(id);
        if (dto == null) {
            throw SystemErrorCode.SYS_SEQUENCE_RULE_NOT_FOUND.exception();
        }
        return SysSequenceRuleConvert.INSTANCE.toVO(dto);
    }

    @PreAuthorize("hasPermission('manage:sequence', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<SysSequenceRuleVO> page(SysSequenceRulePageParam param) {
        return this.sequenceRuleManager.getPage(param);
    }

    /**
     * 生成下一个业务编码（供前端表单的「生成」按钮调用）
     *
     * <p>与 Feign 的 {@code ApiSequence#next} 同源，都收敛到
     * {@link SysSequenceRuleService#nextCode(String)}，区别只是调用方在浏览器侧 ——
     * 在此之前该能力只存在于带 {@code @Hidden} 的 Feign 实现里，前端拿不到。</p>
     *
     * <p><b>刻意用 POST 而非 GET</b>：本接口会消耗一个号段、不满足幂等语义，
     * 而 GET 可能被浏览器或中间代理缓存/重放，号段会被无声烧掉。</p>
     *
     * <p><b>审计：标注 {@code @Log}</b>。号段消耗是真实的状态变更（Redis 计数 + 数据库水位），
     * 且「谁在什么时候拿走了哪个单号」本身就是业务上要能追溯的问题；
     * 同时 {@code SysAuditAnnotationGuardTest} 把「本模块所有写接口必须审计」钉死为不变式，
     * 开豁免会让那条断言的含义被稀释。业务类型用 {@code GEN_CODE}。</p>
     *
     * @param ruleCode 规则编码（如 order_no），规则须已在「编码规则」中维护
     * @return 格式化后的业务编码
     */
    @Log(
        title = "编码规则",
        logType = "SYS_SEQUENCE_NEXT",
        businessType = BusinessType.GEN_CODE,
        operation = "'生成业务编码: ' + #ruleCode",
        targetType = "SYS_SEQUENCE",
        targetId = "#ruleCode",
        riskLevel = 0)
    @PreAuthorize("hasPermission('manage:sequence', 'generate')")
    @PostMapping("next")
    @Operation(summary = "生成业务编码", description = "按规则编码生成下一个业务编码，会消耗一个号段")
    public String next(@RequestParam("ruleCode") String ruleCode) {
        if (StrUtil.isBlank(ruleCode)) {
            throw SystemErrorCode.SYS_PARAM_SEQUENCE_RULE_CODE_EMPTY.exception();
        }
        return this.sequenceRuleService.nextCode(ruleCode);
    }

}
