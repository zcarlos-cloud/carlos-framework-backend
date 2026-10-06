package com.carlos.system.pojo.ao;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 数据接入记录 AO对象
 * </p>
 *
 * @author Carlos
 * @date 2021-12-15 17:40:44
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class UploadResultAO implements Serializable {

    /**
     * 文件分组
     */
    private String groupId;
    /**
     * 文件信息
     */
    private List<com.carlos.system.pojo.ao.FileInfoAO> files;

}
