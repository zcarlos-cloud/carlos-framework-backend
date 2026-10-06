package com.carlos.datasource;


/**
 * <p>
 *   常量定义
 * </p>
 *
 * @author Carlos
 * @date 2026-03-17 22:47
 */
public interface MybatisConstant {


    String DEFAULT_PRIMARY_KEY_FILED_NAME = "id";
    String DEFAULT_VERSION_FILED_NAME = "version";
    String DEFAULT_LOGIC_DELETE_FILED_NAME = "deleted";
    String DEFAULT_UPDATE_USER_FILED_NAME = "updateBy";
    String DEFAULT_UPDATE_TIME_FILED_NAME = "updateTime";
    String DEFAULT_CREATE_USER_FILED_NAME = "createBy";
    String DEFAULT_CREATE_TIME_FILED_NAME = "createTime";
    String DEFAULT_TENANT_ID_FILED_NAME = "tenantId";

}
