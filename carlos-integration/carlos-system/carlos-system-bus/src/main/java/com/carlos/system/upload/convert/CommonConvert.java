package com.carlos.system.upload.convert;

import org.apache.commons.io.IOUtils;
import org.mapstruct.Named;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * <p>
 * 通用转换器
 * </p>
 *
 * @author Carlos
 * @date 2022-2-7 15:22:31
 */
@Named("CommonConvert")
public class CommonConvert {

    /**
     * 样例方法
     *
     * @param id source字段绑定的参数
     * @return java.lang.String
     * @author Carlos
     * @date 2022-2-7 15:22:31
     */
    @Named("toName")
    public String toName(Serializable id) {
        if (id == null) {
            return null;
        }
        // SysMenuManager menuManager = SpringUtil.getBean(SysMenuManager.class);
        // return menuManager.getNameById(menuId);
        return "";
    }

    @Named("streamToByte")
    public byte[] streamToByte(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        return IOUtils.toByteArray(in);
    }


}
