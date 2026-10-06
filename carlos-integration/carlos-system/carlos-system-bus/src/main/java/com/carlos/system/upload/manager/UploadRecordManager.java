package com.carlos.system.upload.manager;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.datasource.base.BaseService;
import com.carlos.system.upload.pojo.dto.UploadRecordDTO;
import com.carlos.system.upload.pojo.entity.UploadRecord;
import com.carlos.system.upload.pojo.param.UploadRecordPageParam;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 文件上传记录 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2022-2-7 15:22:31
 */
public interface UploadRecordManager extends BaseService<UploadRecord> {

    /**
     * 新增文件上传记录
     *
     * @param dto 文件上传记录数据
     * @return boolean
     * @author Carlos
     * @date 2022-2-7 15:22:31
     */
    boolean add(UploadRecordDTO dto);

    /**
     * 删除文件上传记录
     *
     * @param id 文件上传记录id
     * @return boolean
     * @author Carlos
     * @date 2022-2-7 15:22:31
     */
    boolean delete(Serializable id);

    /**
     * 修改文件上传记录信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2022-2-7 15:22:31
     */
    boolean modify(UploadRecordDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.system.upload.pojo.dto.UploadRecordDTO
     * @author Carlos
     * @date 2023/1/3 17:21
     */
    UploadRecordDTO getDtoById(Serializable id);

    /**
     * 获取一组文件
     *
     * @param groupId 文件分组id
     * @return java.util.List<com.carlos.system.upload.pojo.dto.UploadRecordDTO>
     * @author Carlos
     * @date 2023/1/3 17:22
     */
    List<UploadRecordDTO> getDtoByGroupId(String groupId);

    /**
     * @Title: getDtoByIds
     * @Description: 根据文件ids获取一组文件
     * @Date: 2023/2/23 16:28
     * @Parameters: [ids]
     * @Return java.util.List<com.carlos.system.upload.pojo.dto.UploadRecordDTO>
     */
    List<UploadRecordDTO> getDtoByIds(Set<Long> ids);

    /**
     * 分页查询文件列表
     *
     * @param param 分页查询参数
     * @return com.baomidou.mybatisplus.core.metadata.IPage<com.carlos.system.upload.pojo.dto.UploadRecordDTO>
     * @author Carlos
     * @date 2026-05-30
     */
    IPage<UploadRecordDTO> pageDto(UploadRecordPageParam param);

    /**
     * 批量逻辑删除文件上传记录
     *
     * @param ids 文件上传记录id集合
     * @return boolean
     * @author Carlos
     * @date 2026-05-30
     */
    boolean deleteBatch(Set<Long> ids);
}
