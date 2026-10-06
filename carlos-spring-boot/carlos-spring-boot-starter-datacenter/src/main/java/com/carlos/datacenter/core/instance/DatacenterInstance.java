package com.carlos.datacenter.core.instance;

import com.carlos.datacenter.core.entity.DatacenterMetadataCatalog;
import com.carlos.datacenter.core.entity.DatacenterMetadataDetail;

import java.util.List;

/**
 * <p>通用接口，定义数据中台方法</p>
 *
 * @author Carlos
 * 2024-10-09 22:47
 */
public interface DatacenterInstance {

    /**
     * 获取中台实例id
     */
    String getInstanceId();

    /**
     * 获取中台供应商
     */
    String getSupplier();


    /**
     * 元数据列表
     *
     * @param keyword 搜索关键字
     * @return java.util.List<com.carlos.warehouse.dc.DCMetadata>
     * @author Carlos
     * @date 2024/9/13 17:35
     */
    List<DatacenterMetadataCatalog> metadataList(String keyword);


    /**
     * 元数据详情
     *
     * @param key 元数据key
     * @return com.carlos.warehouse.dc.DCMetadataField
     * @author Carlos
     * @date 2024/9/13 17:37
     */
    DatacenterMetadataDetail metadataDetail(String key);


    /**
     * 获取数据的接口   // TODO: Carlos 2024/9/13 待设计
     *
     * @throws
     * @author Carlos
     * @date 2024/9/13 17:40
     */
    void data();


}
