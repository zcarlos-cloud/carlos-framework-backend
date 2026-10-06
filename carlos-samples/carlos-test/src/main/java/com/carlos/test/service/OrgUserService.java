package com.carlos.test.service;

import com.carlos.test.manager.OrgUserManager;
import com.carlos.test.pojo.dto.OrgUserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 系统用户 业务接口实现
 * </p>
 *
 * @author Carlos
 * @date 2023-8-12 11:16:18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgUserService {

    private final OrgUserManager userManager;

    public void addOrgUser(OrgUserDTO dto) {
        boolean success = userManager.add(dto);
        if (!success) {
            // 保存失败的应对措
            return;
        }
        Serializable id = dto.getId();
        // 保存完成的后续业
    }

    public void deleteOrgUser(Set<String> ids) {
        for (Serializable id : ids) {
            boolean success = userManager.delete(id);
            if (!success) {
                // 删除失败的措
                continue;
            }
            // 删除成功的后续业
        }
    }

    public void updateOrgUser(OrgUserDTO dto) {
        boolean success = userManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操
    }


}
