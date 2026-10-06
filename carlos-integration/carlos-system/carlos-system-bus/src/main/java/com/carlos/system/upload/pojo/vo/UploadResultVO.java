package com.carlos.system.upload.pojo.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 数据接入记录 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2021-12-15 17:40:44
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class UploadResultVO implements Serializable {

    /**
     * 文件分组
     */
    private String groupId;
    /**
     * 文件信息
     */
    private List<FileInfoVO> files;
}
