package com.carlos.system.menu.service;

import com.carlos.system.menu.convert.SysModuleConvert;
import com.carlos.system.menu.manager.SysModuleManager;
import com.carlos.system.menu.pojo.dto.SysModuleDTO;
import com.carlos.system.menu.pojo.param.SysModuleCreateParam;
import com.carlos.system.menu.pojo.param.SysModuleUpdateParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 功能模块 业务服务
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysModuleService {

    private final SysModuleManager moduleManager;

    /**
     * 新增功能模块
     *
     * @param param 新增参数
     */
    public void add(SysModuleCreateParam param) {
        SysModuleDTO dto = SysModuleConvert.INSTANCE.toDTO(param);
        dto.setModuleStatus(param.getModuleStatus() != null ? param.getModuleStatus() : true);
        moduleManager.add(dto);
    }

    /**
     * 删除功能模块
     *
     * @param ids 主键集合
     */
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            moduleManager.delete(id);
        }
    }

    /**
     * 更新功能模块
     *
     * @param param 更新参数
     */
    public void update(SysModuleUpdateParam param) {
        SysModuleDTO dto = SysModuleConvert.INSTANCE.toDTO(param);
        moduleManager.modify(dto);
    }

}
