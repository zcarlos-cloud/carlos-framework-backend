package com.carlos.auth.log;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.log.pojo.param.LoginLogPageParam;
import com.carlos.auth.log.pojo.vo.LoginLogDetailVO;
import com.carlos.auth.log.pojo.vo.LoginLogVO;
import com.carlos.audit.api.ApiAuditLogQuery;
import com.carlos.audit.api.pojo.ao.AuditLogMainAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogQueryParam;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 登录日志 业务
 * </p>
 *
 * <p>不新建表：登录事件已由登录链路异步写入 carlos-audit（ClickHouse，logType=USER_LOGIN，
 * 成功/失败靠 logStatus 区分），本服务只做查询聚合（Feign 透传 + 字段裁剪）。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    /** 登录事件在审计中的 logType（UserLoginService 及各登录成功/失败处理器写入口径） */
    private static final String LOG_TYPE_USER_LOGIN = "USER_LOGIN";

    private final ApiAuditLogQuery apiAuditLogQuery;

    /**
     * 登录日志分页
     *
     * @param param 查询参数
     * @return 分页结果
     */
    public Paging<LoginLogVO> page(LoginLogPageParam param) {
        ApiAuditLogQueryParam queryParam = new ApiAuditLogQueryParam()
            .setPageNum(param.getCurrent())
            .setPageSize(param.getSize())
            .setLogType(LOG_TYPE_USER_LOGIN)
            .setLogStatus(param.getLogStatus())
            .setPrincipalName(param.getUsername())
            .setClientIp(param.getIp())
            .setAuthType(param.getAuthType())
            .setStartTime(param.getStartTime())
            .setEndTime(param.getEndTime())
            .setTenantId(currentTenantId());

        Result<Paging<AuditLogMainAO>> result = apiAuditLogQuery.pageQuery(queryParam);
        if (!result.isSuccess() || result.getData() == null) {
            log.warn("登录日志查询失败: code={}, msg={}", result.getCode(), result.getMsg());
            throw AuthErrorCode.AUTH_LOGIN_LOG_QUERY_FAILED.exception();
        }

        Paging<AuditLogMainAO> source = result.getData();
        List<LoginLogVO> records = CollUtil.emptyIfNull(source.getRecords()).stream()
            .map(this::toVO)
            .collect(Collectors.toList());
        return new Paging<LoginLogVO>()
            .setTotal(source.getTotal())
            .setCurrent(source.getCurrent())
            .setSize(source.getSize())
            .setRecords(records);
    }

    private LoginLogVO toVO(AuditLogMainAO ao) {
        return new LoginLogVO()
            .setId(ao.getId())
            .setUsername(ao.getPrincipalName())
            .setLogStatus(ao.getLogStatus())
            .setLoginTime(ao.getEventTime())
            .setIp(ao.getClientIp())
            .setLocation(joinLocation(ao))
            .setUserAgent(ao.getUserAgent())
            .setAuthType(ao.getAuthType())
            .setResultMessage(ao.getResultMessage())
            .setDurationMs(ao.getDurationMs());
    }

    /**
     * 登录日志详情
     *
     * @param id 日志ID
     * @return 详情（含客户端/认证/链路上下文）
     */
    public LoginLogDetailVO getById(Long id) {
        Result<AuditLogMainAO> result = apiAuditLogQuery.getById(id);
        if (!result.isSuccess()) {
            log.warn("登录日志详情查询失败: id={}, code={}, msg={}", id, result.getCode(), result.getMsg());
            throw AuthErrorCode.AUTH_LOGIN_LOG_QUERY_FAILED.exception();
        }
        AuditLogMainAO ao = result.getData();
        if (ao == null) {
            throw AuthErrorCode.AUTH_LOGIN_LOG_NOT_FOUND.exception();
        }
        return toDetailVO(ao);
    }

    private LoginLogDetailVO toDetailVO(AuditLogMainAO ao) {
        LoginLogDetailVO vo = new LoginLogDetailVO();
        BeanUtil.copyProperties(toVO(ao), vo);
        vo.setPrincipalId(ao.getPrincipalId())
            .setPrincipalType(ao.getPrincipalType())
            .setAuthProvider(ao.getAuthProvider())
            .setBizChannel(ao.getBizChannel())
            .setClientPort(ao.getClientPort())
            .setServerIp(ao.getServerIp())
            .setDeviceFingerprint(ao.getDeviceFingerprint())
            .setRiskLevel(ao.getRiskLevel())
            .setResultCode(ao.getResultCode())
            .setClientTime(ao.getClientTime())
            .setServerTime(ao.getServerTime())
            .setDeptName(ao.getDeptName())
            .setTenantId(ao.getTenantId())
            .setTraceId(ao.getTraceId())
            .setAppName(ao.getAppName());
        return vo;
    }

    private String joinLocation(AuditLogMainAO ao) {
        String location = Stream.of(ao.getLocationCountry(), ao.getLocationProvince(), ao.getLocationCity())
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining(" "));
        return location.isEmpty() ? null : location;
    }

    private String currentTenantId() {
        UserContext user = SecurityUtils.getCurrentUser();
        if (user != null && user.getTenantId() != null) {
            return String.valueOf(user.getTenantId());
        }
        // 审计查询的 tenant_id 条件无空值守卫（buildFilterQuery 无条件拼参），
        // 无安全上下文时回落平台租户 0，避免空值透传导致查询异常
        return "0";
    }

}
