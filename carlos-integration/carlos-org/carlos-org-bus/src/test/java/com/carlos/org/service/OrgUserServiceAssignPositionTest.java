package com.carlos.org.service;

import com.carlos.org.config.OrgProperties;
import com.carlos.org.encoder.PasswordEncoderProvider;
import com.carlos.org.manager.OrgDepartmentManager;
import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.manager.OrgUserDepartmentManager;
import com.carlos.org.manager.OrgUserManager;
import com.carlos.org.manager.OrgUserRoleManager;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.param.OrgUserAssignPositionParam;
import com.carlos.org.position.manager.OrgPositionHistoryManager;
import com.carlos.org.position.manager.OrgPositionLevelManager;
import com.carlos.org.position.manager.OrgPositionManager;
import com.carlos.org.position.manager.OrgUserPositionManager;
import com.carlos.org.position.pojo.dto.OrgPositionDTO;
import com.carlos.org.position.pojo.dto.OrgPositionHistoryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrgUserService.assignPositions 单元测试
 *
 * <p>R-3: org_position_history 的 change_reason/operator_id/operator_name
 * 为 NOT NULL 列, 写历史时必须赋值, 否则插入失败。</p>
 *
 * @author Carlos
 */
class OrgUserServiceAssignPositionTest {

    private OrgUserManager userManager;
    private OrgUserPositionManager userPositionManager;
    private OrgPositionManager positionManager;
    private OrgPositionHistoryManager positionHistoryManager;
    private OrgUserService userService;

    @BeforeEach
    void setUp() {
        userManager = mock(OrgUserManager.class);
        userPositionManager = mock(OrgUserPositionManager.class);
        positionManager = mock(OrgPositionManager.class);
        positionHistoryManager = mock(OrgPositionHistoryManager.class);
        userService = new OrgUserService(
            userManager,
            mock(OrgUserDepartmentManager.class),
            mock(OrgUserRoleManager.class),
            userPositionManager,
            mock(OrgDepartmentManager.class),
            mock(OrgRoleManager.class),
            positionManager,
            mock(OrgPositionLevelManager.class),
            positionHistoryManager,
            mock(PasswordEncoderProvider.class),
            new OrgProperties()
        );
    }

    @Test
    @DisplayName("应该在分配岗位写入历史时填充变更原因与操作人")
    void should_fill_reason_and_operator_when_writing_position_history() {
        Long userId = 1001L;
        Long positionId = 2001L;

        when(userManager.getDtoById(userId)).thenReturn(new OrgUserDTO());
        when(userPositionManager.getByUserIdAndPositionId(userId, positionId)).thenReturn(null);
        OrgPositionDTO position = new OrgPositionDTO();
        position.setId(positionId);
        position.setHeadcount(5);
        when(positionManager.getDtoById(positionId)).thenReturn(position);
        when(userPositionManager.countOccupiedByPositionId(positionId)).thenReturn(0);

        OrgUserAssignPositionParam param = new OrgUserAssignPositionParam()
            .setUserId(userId)
            .setPositionId(positionId)
            .setLevelId(3001L)
            .setDepartmentId(4001L)
            .setIsMain(false);

        userService.assignPositions(param);

        ArgumentCaptor<OrgPositionHistoryDTO> captor = ArgumentCaptor.forClass(OrgPositionHistoryDTO.class);
        verify(positionHistoryManager).add(captor.capture());
        OrgPositionHistoryDTO history = captor.getValue();

        assertNotNull(history.getChangeReason(), "change_reason 为 NOT NULL 列, 必须赋值");
        assertFalse(history.getChangeReason().isBlank(), "change_reason 不能为空串");
        assertNotNull(history.getOperatorId(), "operator_id 为 NOT NULL 列, 必须赋值");
        assertNotNull(history.getOperatorName(), "operator_name 为 NOT NULL 列, 必须赋值");
        assertFalse(history.getOperatorName().isBlank(), "operator_name 不能为空串");
    }
}
