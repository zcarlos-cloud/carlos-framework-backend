package com.carlos.audit.apiimpl;

import com.carlos.audit.api.ApiAuditLogMain;
import com.carlos.audit.api.pojo.ao.AuditLogMainAO;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.audit.convert.AuditLogMainConvert;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.service.AuditLogMainService;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 审计日志 Feign 接口实现
 * 提供给其他微服务通过 Feign 调用保存审计日志
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit/log/main")
@Tag(name = "审计日志Feign接口", description = "供其他微服务调用保存审计日志")
public class ApiAuditLogMainImpl implements ApiAuditLogMain {

    private final AuditLogMainService logMainService;

    /**
     * 保存审计日志（异步）
     *
     * @param param 审计日志参数
     * @return 保存结果
     */
    @Override
    @PostMapping("/save")
    @Operation(summary = "保存审计日志（异步）", description = "通过Disruptor异步写入，性能最优")
    public Result<AuditLogMainAO> saveAuditLog(@RequestBody @Valid ApiAuditLogMainParam param) {
        try {
            if (param == null) {
                return Result.error("审计日志参数不能为空");
            }

            // 参数转换为 DTO
            AuditLogMainDTO dto = AuditLogMainConvert.INSTANCE.toDTO(param);

            // 异步保存
            logMainService.addAuditLogMain(dto);

            // 转换为 AO 返回
            AuditLogMainAO ao = AuditLogMainConvert.INSTANCE.toAO(dto);

            log.debug("审计日志异步保存成功，logType: {}", param.getLogType());
            return Result.success(ao, "审计日志保存成功");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("审计日志异步保存失败", e);
            return Result.error("审计日志保存失败");
        }
    }

    /**
     * 保存审计日志（同步）
     *
     * @param param     审计日志参数
     * @param timeoutMs 超时时间（毫秒）
     * @return 保存结果
     */
    @Override
    @PostMapping("/save-sync")
    @Operation(summary = "保存审计日志（同步）", description = "重要日志使用，确保写入成功")
    public Result<AuditLogMainAO> saveAuditLogSync(@RequestBody @Valid ApiAuditLogMainParam param,
                                                   @RequestParam(value = "timeoutMs", defaultValue = "5000") long timeoutMs) {
        try {
            if (param == null) {
                return Result.error("审计日志参数不能为空");
            }

            // 参数转换为 DTO
            AuditLogMainDTO dto = AuditLogMainConvert.INSTANCE.toDTO(param);

            // 同步保存
            boolean success = logMainService.addAuditLogMainSync(dto, timeoutMs);

            if (!success) {
                log.warn("审计日志同步保存超时，logType: {}", param.getLogType());
                return Result.error("审计日志保存超时");
            }

            // 转换为 AO 返回
            AuditLogMainAO ao = AuditLogMainConvert.INSTANCE.toAO(dto);

            log.debug("审计日志同步保存成功，logType: {}", param.getLogType());
            return Result.success(ao, "审计日志保存成功");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("审计日志同步保存失败", e);
            return Result.error("审计日志保存失败");
        }
    }

    /**
     * 批量保存审计日志（异步）
     *
     * @param params 审计日志参数列表
     * @return 保存结果
     */
    @Override
    @PostMapping("/batch-save")
    @Operation(summary = "批量保存审计日志（异步）")
    public Result<Void> batchSaveAuditLog(@RequestBody @Valid List<ApiAuditLogMainParam> params) {
        try {
            if (params == null || params.isEmpty()) {
                return Result.error("审计日志参数列表不能为空");
            }

            // 批量转换为 DTO
            List<AuditLogMainDTO> dtos = params.stream()
                .map(AuditLogMainConvert.INSTANCE::toDTO)
                .collect(Collectors.toList());

            // 批量异步保存
            logMainService.batchAddAuditLogMain(dtos);

            log.debug("批量审计日志异步保存成功，数量: {}", params.size());
            return Result.success();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量审计日志保存失败", e);
            return Result.error("批量审计日志保存失败");
        }
    }

    /**
     * 保存简单审计日志（最小参数）
     *
     * @param logType     日志类型
     * @param category    日志大类
     * @param operation   操作描述
     * @param principalId 操作主体ID
     * @param targetId    目标对象ID
     * @param state       状态
     * @return 保存结果
     */
    @Override
    @PostMapping("/save-simple")
    @Operation(summary = "保存简单审计日志", description = "最小参数快速记录")
    public Result<AuditLogMainAO> saveSimpleAuditLog(@RequestParam("logType") String logType,
                                                     @RequestParam("category") AuditLogCategoryEnum category,
                                                     @RequestParam("operation") String operation,
                                                     @RequestParam("principalId") String principalId,
                                                     @RequestParam(value = "targetId", required = false) String targetId,
                                                     @RequestParam(value = "logStatus", defaultValue = "SUCCESS") AuditLogStatusEnum logStatus) {
        try {
            // 构建最小化的 DTO
            AuditLogMainDTO dto = new AuditLogMainDTO();
            dto.setLogType(logType);
            dto.setCategory(category);
            dto.setOperation(operation);
            dto.setPrincipalId(principalId);
            dto.setTargetId(targetId);
            dto.setLogStatus(logStatus);

            // 设置必要的时间字段
            LocalDateTime now = LocalDateTime.now();
            dto.setServerTime(now);
            dto.setEventTime(now);
            dto.setEventDate(LocalDate.now());
            dto.setClientTime(now);

            // 设置默认值
            dto.setLogSchemaVersion(1);
            dto.setRiskLevel(0);
            dto.setDurationMs(0);
            dto.setRetentionDeadline(LocalDate.now().plusDays(90));
            dto.setHasDataChange(false);
            dto.setPrincipalType(AuditLogPrincipalTypeEnum.USER);
            dto.setLogStatus(logStatus != null ? logStatus : AuditLogStatusEnum.SUCCESS);

            // 异步保存
            logMainService.addAuditLogMain(dto);

            // 转换为 AO 返回
            AuditLogMainAO ao = AuditLogMainConvert.INSTANCE.toAO(dto);

            log.debug("简单审计日志保存成功，logType: {}", logType);
            return Result.success(ao, "审计日志保存成功");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("简单审计日志保存失败", e);
            return Result.error("审计日志保存失败");
        }
    }
}
