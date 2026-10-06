package com.carlos.boot.web.resource.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 资源组 DTO，包含一组 API 资源。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
public class ResourceGroup {

    /** 资源组名称 */
    private String name;
    /** 该组下的资源列表 */
    private List<ApiResource> resources;
}
