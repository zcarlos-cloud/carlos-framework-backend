package com.carlos.auth.mfa.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MFA 设置信息 DTO
 *
 * <p>用于 Service 层内部传输 MFA 设置相关信息。</p>
 *
 * <p><strong>与其他对象的区别：</strong></p>
 * <ul>
 *   <li>DTO (此类): 服务层内部传输，包含完整的 MFA 设置信息</li>
 *   <li>VO ({@link com.carlos.auth.mfa.pojo.vo.MfaSetupVO}): 视图对象，返回给前端</li>
 *   <li>Entity ({@link com.carlos.auth.mfa.pojo.entity.MfaRecoveryCode}): 与数据库表对应</li>
 * </ul>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see com.carlos.auth.mfa.pojo.vo.MfaSetupVO
 * @see com.carlos.auth.mfa.convert.MfaConvert
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaSetupDTO {

    /**
     * MFA密钥（Base32编码）
     */
    private String secret;

    /**
     * QR码URL（可生成二维码）
     */
    private String qrCodeUrl;

    /**
     * 格式化显示的密钥
     */
    private String formattedSecret;

    /**
     * 备用恢复码列表
     */
    private List<String> recoveryCodes;
}
