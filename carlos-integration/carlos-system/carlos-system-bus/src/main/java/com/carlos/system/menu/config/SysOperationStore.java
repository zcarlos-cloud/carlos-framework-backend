package com.carlos.system.menu.config;

import com.carlos.boot.web.resource.ResourceStore;
import com.carlos.boot.web.resource.model.ApiResource;
import com.carlos.boot.web.resource.model.ResourceApp;
import com.carlos.boot.web.resource.model.ResourceGroup;
import com.carlos.system.enums.OperationStatus;
import com.carlos.system.enums.OperationType;
import com.carlos.system.menu.manager.SysOperationManager;
import com.carlos.system.menu.pojo.dto.SysOperationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 系统资源存储自定义实现 - 将扫描的API写入 sys_operation
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SysOperationStore implements ResourceStore {

    private final SysOperationManager sysOperationManager;

    @Override
    public void save(ResourceApp resource) {
        List<ResourceGroup> groups = resource.getGroups();
        if (groups == null || groups.isEmpty()) {
            return;
        }

        for (ResourceGroup group : groups) {
            List<ApiResource> resources = group.getResources();
            if (resources == null || resources.isEmpty()) {
                continue;
            }

            for (ApiResource item : resources) {
                String fullPath = item.getPathPrefix() + item.getPath();
                // 优先用端点声明的 @PreAuthorize 权限码（唯一真相源）；
                // 未声明时才按路径约定兜底生成，避免扫描出的权限码与实际鉴权不一致
                String permCode = (item.getPermCode() != null && !item.getPermCode().isBlank())
                    ? item.getPermCode()
                    : generatePermCode(fullPath, item.getMethod());

                // 检查是否已存在（按 path + method）
                // 实际项目中建议增加按 api_path + api_method 的联合查询
                SysOperationDTO existing = sysOperationManager.getByPermCode(permCode);
                if (existing != null) {
                    log.debug("API already exists: {} {}", item.getMethod(), fullPath);
                    continue;
                }

                SysOperationDTO dto = new SysOperationDTO();
                dto.setOpType(OperationType.API);
                dto.setOpName(item.getName());
                dto.setPermCode(permCode);
                dto.setApiPath(fullPath);
                dto.setApiMethod(item.getMethod().name());
                dto.setAutoScan(true);
                dto.setOpStatus(OperationStatus.ENABLED);
                dto.setOrderNum(0);

                sysOperationManager.add(dto);
                log.debug("Auto scanned API: {} {} -> {}", item.getMethod(), fullPath, permCode);
            }
        }
    }

    /**
     * 根据 API 路径和方法生成权限码
     * <p>
     * 规则：将路径中的 / 替换为 :，去掉参数占位符
     * 例如：POST /api/user/{id} -> user:update
     * </p>
     *
     * @param path   API路径
     * @param method HTTP方法
     * @return 权限码
     */
    private String generatePermCode(String path, Object method) {
        // 默认生成规则：去掉前缀后取最后一个路径段 + 方法名
        // 实际项目中建议根据业务规范手动配置 perm_code
        String cleanPath = path.replace("/api/", "")
            .replace("{", "").replace("}", "")
            .replace("//", "/");

        String[] parts = cleanPath.split("/");
        if (parts.length == 0) {
            return "api:" + method.toString().toLowerCase();
        }

        // 取第一个路径段作为资源，方法作为动作
        String resource = parts[0];
        String action = method.toString().toLowerCase();

        // 根据常见方法映射为动作
        action = switch (action.toUpperCase()) {
            case "GET" -> "read";
            case "POST" -> "create";
            case "PUT" -> "update";
            case "DELETE" -> "delete";
            default -> action;
        };

        return resource + ":" + action;
    }

}
