package ${project.packageName}.config;

import com.carlos.core.interfaces.ApplicationExtend;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.carlos.boot.web.request.WebRequestContext;
import com.carlos.core.auth.UserContext;
import java.io.Serializable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * <p>
 * 自定义系统扩展实现
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnMissingBean(ApplicationExtend.class)
public class ApplicationExtendImpl implements ApplicationExtend {

    @Override
    public Serializable getUserId() {
        UserContext userContext = RequestUtil.getRequestInfo().getUserContext();
        return userContext.getUserId();
    }

}
