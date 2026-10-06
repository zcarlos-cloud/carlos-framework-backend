package com.carlos.system.menu.service;

import com.carlos.system.enums.OperationType;
import com.carlos.system.menu.convert.SysOperationConvert;
import com.carlos.system.menu.manager.SysOperationManager;
import com.carlos.system.menu.pojo.dto.SysOperationDTO;
import com.carlos.system.menu.pojo.vo.SysOperationTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 操作权限树 业务服务
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationTreeService {

    private final SysOperationManager sysOperationManager;

    /**
     * 按菜单维度组织的操作树
     *
     * @return 操作权限树列表
     */
    public List<SysOperationTreeVO> buildOperationTree() {
        List<SysOperationDTO> allOperations = sysOperationManager.listAll();
        if (allOperations == null || allOperations.isEmpty()) {
            return List.of();
        }

        List<SysOperationTreeVO> result = new ArrayList<>();

        // 按 menu_id 分组
        Map<Long, List<SysOperationDTO>> grouped = allOperations.stream()
            .filter(op -> op.getMenuId() != null)
            .collect(Collectors.groupingBy(SysOperationDTO::getMenuId));

        for (Map.Entry<Long, List<SysOperationDTO>> entry : grouped.entrySet()) {
            SysOperationTreeVO vo = new SysOperationTreeVO();
            vo.setMenuId(entry.getKey());
            vo.setOperations(SysOperationConvert.INSTANCE.toVOS(entry.getValue()));
            result.add(vo);
        }

        // 添加独立 API（menu_id 为 null）
        List<SysOperationDTO> independentApis = allOperations.stream()
            .filter(op -> op.getMenuId() == null && op.getOpType() != null && op.getOpType() == OperationType.API)
            .toList();

        if (!independentApis.isEmpty()) {
            SysOperationTreeVO independentVo = new SysOperationTreeVO();
            independentVo.setMenuId(0L);
            independentVo.setMenuName("独立接口");
            independentVo.setOperations(SysOperationConvert.INSTANCE.toVOS(independentApis));
            result.add(independentVo);
        }

        return result;
    }

}
