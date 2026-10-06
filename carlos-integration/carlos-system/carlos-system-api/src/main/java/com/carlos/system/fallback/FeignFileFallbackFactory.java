package com.carlos.system.fallback;

import com.carlos.core.response.Result;
import com.carlos.system.api.ApiFile;
import com.carlos.system.pojo.ao.FileInfoAO;
import com.carlos.system.pojo.ao.UploadResultAO;
import com.carlos.system.pojo.param.ApiFileUploadParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 文件降级服务
 * </p>
 *
 * @date 2022/12/29 11:57
 */

@Slf4j
public class FeignFileFallbackFactory implements FallbackFactory<ApiFile> {


    @Override
    public ApiFile create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("文件服务调用失败: message:{}", message);
        return new ApiFile() {
            @Override
            public Result<FileInfoAO> getFile(Long id) {
                return Result.error("文件信息获取失败");
            }

            @Override
            public Result<FileInfoAO> getFileStreamInfo(Long id) {
                return Result.error("文件获取失败");
            }

            @Override
            public Result<List<FileInfoAO>> getGroupFile(String groupId) {
                return Result.error("文件信息获取失败");
            }

            @Override
            public Result<List<FileInfoAO>> getFiles(Set<Long> ids) {
                return Result.error("文件信息获取失败");
            }

            @Override
            public Result<UploadResultAO> upload(ApiFileUploadParam param) {
                return Result.error("文件上传失败");
            }

            @Override
            public Result<Void> delete(Long id) {
                return Result.error("文件删除失败");
            }

            @Override
            public Result<Void> deleteBatch(Set<Long> ids) {
                return Result.error("批量文件删除失败");
            }

            @Override
            public Result<Void> update(Long id, com.carlos.system.pojo.param.ApiFileUpdateParam param) {
                return Result.error("文件更新失败");
            }
        };
    }
}
