package com.carlos.system.pojo.ao;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 图片信息显示层对象
 * </p>
 *
 * @author carlos
 * @date 2022-11-28 10:48:53
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageAO {

    /**
     * 文件Id
     */
    private Long id;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件地址
     */
    private String url;

    /**
     * 文件地址
     */
    private float size;
}
