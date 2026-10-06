package com.carlos.datascope;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 自定义权限
 * </p>
 *
 * @author Carlos
 * @date 2023/1/28 12:37
 */
@FunctionalInterface
public interface CustomScope {

    Set<Serializable> accept(Map<String, Object> params);

}
