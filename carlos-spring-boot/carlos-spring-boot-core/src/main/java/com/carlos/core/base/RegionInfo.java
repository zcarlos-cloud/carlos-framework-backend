package com.carlos.core.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 区域信息
 * </p>
 *
 * @author Carlos
 * @date 2022/12/30 13:52
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class RegionInfo implements Serializable {

    /**
     * 区域id
     */
    private Serializable id;

    /**
     * 区域code
     */
    private String code;

    /**
     * 区域名称
     */
    private String name;

    /**
     * 区域全名
     */
    private List<String> fullName;
}
