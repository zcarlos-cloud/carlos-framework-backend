package com.carlos.system.api;


import com.carlos.core.response.Result;
import com.carlos.system.ServiceNameConstant;
import com.carlos.system.fallback.FeignFileFallbackFactory;
import com.carlos.system.pojo.ao.FileInfoAO;
import com.carlos.system.pojo.ao.UploadResultAO;
import com.carlos.system.pojo.param.ApiFileUpdateParam;
import com.carlos.system.pojo.param.ApiFileUploadParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 文件信息 rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@FeignClient(value = ServiceNameConstant.SYSTEM, contextId = "file", path = "/api/sys/file", fallbackFactory = FeignFileFallbackFactory.class)
public interface ApiFile {


    /**
     * 获取文件信息 不包含文件流信息
     *
     * @param id 文件id
     * @author carlos
     * @date 2021/12/29 11:29
     */
    @GetMapping("info")
    Result<FileInfoAO> getFile(@RequestParam("id") Long id);

    /**
     * 获取文件流信息，包含文件基本信息
     *
     * @param id 文件id
     * @author carlos
     * @date 2021/12/29 11:29
     */
    @GetMapping("stream/info")
    Result<FileInfoAO> getFileStreamInfo(@RequestParam("id") Long id);

    /**
     * 获取一组文件信息
     *
     * @param groupId 文件组id
     * @return com.carlos.common.core.response.Result<java.util.List < com.carlos.sys.pojo.FileInfo>>
     * @author Carlos
     * @date 2023/3/23 21:45
     */
    @GetMapping("group")
    Result<List<FileInfoAO>> getGroupFile(@RequestParam("groupId") String groupId);

    /**
     * 批量获取文件信息
     *
     * @param ids 文件id集合
     * @return com.carlos.common.core.response.Result<java.util.List < com.carlos.sys.pojo.FileInfo>>
     * @author Carlos
     * @date 2023/3/23 21:44
     */
    @GetMapping("info/ids")
    Result<List<FileInfoAO>> getFiles(@RequestParam("ids") Set<Long> ids);

    /**
     * 文件上传
     *
     * @param param 文件上传参数
     * @return com.carlos.core.response.Result<com.carlos.system.pojo.ao.UploadResultAO>
     * @author Carlos
     * @date 2023/7/6 13:38
     */
    @PostMapping("upload")
    Result<UploadResultAO> upload(@RequestBody ApiFileUploadParam param);

    /**
     * 删除文件
     *
     * @param id 文件id
     * @return com.carlos.core.response.Result<java.lang.Void>
     * @author Carlos
     * @date 2026-05-30
     */
    @PostMapping("delete")
    Result<Void> delete(@RequestParam("id") Long id);

    /**
     * 批量删除文件
     *
     * @param ids 文件id集合
     * @return com.carlos.core.response.Result<java.lang.Void>
     * @author Carlos
     * @date 2026-05-30
     */
    @PostMapping("deleteBatch")
    Result<Void> deleteBatch(@RequestBody Set<Long> ids);

    /**
     * 更新文件信息
     *
     * @param id  文件id
     * @param dto 更新信息
     * @return com.carlos.core.response.Result<java.lang.Void>
     * @author Carlos
     * @date 2026-05-30
     */
    @PostMapping("update")
    Result<Void> update(@RequestParam("id") Long id, @RequestBody ApiFileUpdateParam param);

}
