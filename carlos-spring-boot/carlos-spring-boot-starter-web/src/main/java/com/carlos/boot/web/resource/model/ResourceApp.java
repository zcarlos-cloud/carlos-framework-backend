package com.carlos.boot.web.resource.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 资源应用 DTO，包含应用名称及其资源组。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
public class ResourceApp {

    /** 应用名称 */
    private String appName;
    /** 资源组列表 */
    private List<ResourceGroup> groups;
}
