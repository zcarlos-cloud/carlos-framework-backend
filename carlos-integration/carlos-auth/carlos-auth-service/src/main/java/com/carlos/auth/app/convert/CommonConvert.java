package com.carlos.auth.app.convert;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.carlos.auth.app.pojo.dto.Oauth2ClientSettings;
import com.carlos.auth.app.pojo.dto.Oauth2TokenSettings;
import com.google.common.collect.Sets;
import org.mapstruct.Named;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 通用转换器
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@Named("CommonConvert")
public class CommonConvert {

    private static final String SPLIT = ",";

    @Named("str2set")
    public Set<String> str2set(String str) {
        if (StrUtil.isNotBlank(str)) {
            return Sets.newHashSet(StrUtil.split(str, SPLIT));
        }
        return null;
    }

    @Named("set2str")
    public String set2str(Set<String> set) {
        if (CollUtil.isNotEmpty(set)) {
            return CollUtil.join(set, SPLIT);
        }
        return null;
    }

    @Named("str2map")
    public static Map<String, Object> str2map(String str) {
        if (StrUtil.isNotBlank(str)) {
            return JSONUtil.toBean(str, Map.class);
        }
        return null;
    }

    @Named("str2clientsetting")
    public static Oauth2ClientSettings str2clientsetting(String str) {
        if (StrUtil.isNotBlank(str)) {
            return JSONUtil.toBean(str, Oauth2ClientSettings.class);
        }
        return null;
    }

    @Named("str2tokensetting")
    public static Oauth2TokenSettings str2tokensetting(String str) {
        if (StrUtil.isNotBlank(str)) {
            return JSONUtil.toBean(str, Oauth2TokenSettings.class);
        }
        return null;
    }


    @Named("obj2str")
    public String obj2str(Object obj) {
        if (obj != null) {
            return JSONUtil.toJsonStr(obj);
        }
        return null;
    }
}
