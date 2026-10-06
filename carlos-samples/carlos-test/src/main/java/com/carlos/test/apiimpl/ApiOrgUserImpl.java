package com.carlos.test.apiimpl;


import com.carlos.test.api.ApiOrgUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 系统用户 api接口
 * </p>
 *
 * @author Carlos
 * @date 2023-8-12 11:16:18
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/user")
@Tag(name = "系统用户Feign接口")
public class ApiOrgUserImpl implements ApiOrgUser {


}
