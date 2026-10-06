package com.carlos.devkit.service;

import com.carlos.devkit.engine.model.GeneratedResult;
import com.carlos.devkit.pojo.dto.TemplateGroupDTO;
import com.carlos.devkit.pojo.param.ProjectGenRunParam;

import java.util.List;

/**
 * <p>
 * 工程生成 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface ProjectGenService {

    /**
     * 可用的工程模板组
     *
     * @return 模板组 DTO 列表
     */
    List<TemplateGroupDTO> listTemplates();

    /**
     * 工程生成预览
     *
     * @param param 生成参数
     * @return 生成产物
     */
    GeneratedResult preview(ProjectGenRunParam param);

    /**
     * 工程生成并打包下载
     *
     * @param param 生成参数
     * @return zip 字节
     */
    byte[] generate(ProjectGenRunParam param);
}
