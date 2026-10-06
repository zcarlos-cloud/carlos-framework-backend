package com.carlos.monitor.instance.pojo.param;

import com.carlos.monitor.enums.InstanceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 连接测试 参数封装（入参为未保存的连接信息，或与 instanceId 二选一）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@Schema(description = "连接测试参数")
public class TestConnectionParam {

    @Schema(description = "实例ID（传则按已保存实例测试，忽略其余字段）")
    private Long instanceId;

    @Schema(description = "实例类型（instanceId 为空时必填）")
    private InstanceTypeEnum instanceType;

    @Size(max = 1000, message = "连接地址长度不能超过1000")
    @Schema(description = "连接地址，host:port 逗号分隔（instanceId 为空时必填）")
    private String addresses;

    @Size(max = 100, message = "数据库名长度不能超过100")
    @Schema(description = "数据库名（DB类型必填）")
    private String dbName;

    @Schema(description = "Redis 逻辑库编号")
    private Integer dbIndex;

    @Size(max = 100, message = "连接账号长度不能超过100")
    @Schema(description = "连接账号")
    private String username;

    @Size(max = 200, message = "连接密码长度不能超过200")
    @Schema(description = "连接密码（明文，仅用于本次测试，不落库）")
    private String password;

    @Size(max = 2000, message = "扩展配置长度不能超过2000")
    @Schema(description = "扩展配置JSON")
    private String extConfig;

    @Schema(description = "连接超时（毫秒），默认 3000")
    private Integer connectTimeout = 3000;

    @Schema(description = "读写超时（毫秒），默认 5000")
    private Integer socketTimeout = 5000;
}
