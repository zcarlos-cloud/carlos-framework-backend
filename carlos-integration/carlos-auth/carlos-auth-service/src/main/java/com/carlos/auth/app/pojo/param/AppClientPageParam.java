package com.carlos.auth.app.pojo.param;


import com.carlos.auth.app.pojo.enums.ClientStatusEnum;
import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 应用信息 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(name = "应用信息列表查询参数", description = "应用信息列表查询参数")
public class AppClientPageParam extends ParamPage {
    @Schema(description = "应用编号")
    private String appKey;
    @Schema(description = "应用名称")
    private String appName;
    @Schema(description = "应用密钥到期时间")
    private LocalDateTime clientSecretExpiresAt;
    @Schema(description = "应用发行时间")
    private LocalDateTime clientIssuedAt;
    /**
     * 应用状态
     *
     * <p>
     * 类型与实体 {@code AppClient.clientStatus} 保持一致，都是 {@link ClientStatusEnum} ——
     * 该枚举用 {@code @EnumValue} 标在 code 上，库里存的是 1/2/3。
     * 曾用 {@code String} 承载，一来 manager 根本无法把它拼进查询条件（见 getPage），
     * 二来就算拼了也是拿枚举名去比数字列，永远查不到。
     * 前端传的是枚举名（ENABLE / DISABLE / LOGGED_OUT），Spring 默认按名绑定，无需额外转换。
     * </p>
     */
    @Schema(description = "应用状态（ENABLE / DISABLE / LOGGED_OUT）")
    private ClientStatusEnum clientStatus;
}
