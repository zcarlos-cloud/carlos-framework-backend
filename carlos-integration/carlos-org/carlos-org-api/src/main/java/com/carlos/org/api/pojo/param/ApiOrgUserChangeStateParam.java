package com.carlos.org.api.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 用户状态变更参数（API层，供 Feign 调用使用）
 * </p>
 *
 * <p>状态码与 {@code OrgUserStateEnum} 一致：0-禁用，1-启用，2-锁定</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
public class ApiOrgUserChangeStateParam implements Serializable {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户状态：0-禁用，1-启用，2-锁定
     */
    private Integer userStatus;

}
