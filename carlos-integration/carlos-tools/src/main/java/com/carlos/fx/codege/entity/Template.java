package com.carlos.fx.codege.entity;

import com.carlos.fx.codege.config.CodegeConstant;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 *
 * </p>
 *
 * @author Carlos
 * @date 2020/9/5 16:53
 */
@Data
public class Template implements Serializable {

    /**
     * 模板名称， 包含后缀
     */
    private String sourceName;
    /**
     * 对应生成的文件名称 含后缀
     */
    private String targetName;
    /**
     * 目标文件扩展名
     */
    private String extName = CodegeConstant.JAVA_FILE_EXT;
}
