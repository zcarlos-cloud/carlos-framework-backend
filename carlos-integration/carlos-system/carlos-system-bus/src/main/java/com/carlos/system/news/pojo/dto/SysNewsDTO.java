package com.carlos.system.news.pojo.dto;

import com.carlos.system.news.pojo.enums.SysNewsScopeEnum;
import com.carlos.system.news.pojo.enums.SysNewsStatusEnum;
import com.carlos.system.news.pojo.enums.SysNewsTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 公告/站内通知 数据传输对象
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
public class SysNewsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private SysNewsTypeEnum newsType;

    private String content;

    private SysNewsScopeEnum publishScope;

    private String scopeRoleIds;

    private Boolean isTop;

    private SysNewsStatusEnum status;

    private LocalDateTime publishTime;

    private LocalDateTime expireTime;

    private Long tenantId;

}
