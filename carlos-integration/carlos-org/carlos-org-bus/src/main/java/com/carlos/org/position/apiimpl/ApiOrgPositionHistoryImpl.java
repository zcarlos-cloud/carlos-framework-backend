package com.carlos.org.position.apiimpl;


import com.carlos.org.position.api.ApiOrgPositionHistory;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 岗位变更历史表 api接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/position/history")
@Tag(name = "岗位变更历史表Feign接口")
public class ApiOrgPositionHistoryImpl implements ApiOrgPositionHistory {


}
