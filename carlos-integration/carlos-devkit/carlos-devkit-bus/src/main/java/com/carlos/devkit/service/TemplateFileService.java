package com.carlos.devkit.service;

import com.carlos.devkit.engine.model.TemplateSourceFile;
import com.carlos.devkit.pojo.dto.TemplateFileDTO;
import com.carlos.devkit.pojo.param.TemplateFileCreateParam;
import com.carlos.devkit.pojo.param.TemplateFileRenameParam;
import com.carlos.devkit.pojo.param.TemplateFileSaveParam;
import com.carlos.devkit.pojo.vo.TemplateFileNodeVO;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 模板文件 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface TemplateFileService {

    /**
     * 模板组文件树
     *
     * @param groupId 模板组ID
     * @return 文件树
     */
    List<TemplateFileNodeVO> listTree(Serializable groupId);

    /**
     * 模板文件详情（含正文）
     *
     * @param id 模板文件ID
     * @return 文件 DTO
     */
    TemplateFileDTO getDetail(Serializable id);

    /**
     * 新增模板文件
     *
     * @param param 新增参数
     * @return 文件ID
     */
    Long createFile(TemplateFileCreateParam param);

    /**
     * 保存模板正文（保存前自动落一版历史）
     *
     * @param param 保存参数
     * @return 是否成功
     */
    boolean saveFile(TemplateFileSaveParam param);

    /**
     * 重命名 / 移动模板文件
     *
     * @param param 重命名参数
     * @return 是否成功
     */
    boolean renameFile(TemplateFileRenameParam param);

    /**
     * 批量删除模板文件
     *
     * @param ids 文件ID集合
     * @return 是否成功
     */
    boolean deleteFiles(Collection<Long> ids);

    /**
     * 导出模板组为 zip
     *
     * @param groupId 模板组ID
     * @return zip 字节
     */
    byte[] exportGroup(Serializable groupId);

    /**
     * 取模板组的模板源文件（生成用，仅启用项）
     *
     * @param groupId 模板组ID
     * @return 引擎可消费的模板源文件
     */
    List<TemplateSourceFile> listSourceFiles(Serializable groupId);

    /**
     * 逻辑删除模板组下全部文件
     *
     * @param groupId 模板组ID
     * @return 是否成功
     */
    boolean deleteByGroupId(Serializable groupId);
}
