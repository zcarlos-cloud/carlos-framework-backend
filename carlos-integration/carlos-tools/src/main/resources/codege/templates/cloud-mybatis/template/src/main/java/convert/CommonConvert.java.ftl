package ${project.packageName}.convert;

import cn.hutool.extra.spring.SpringUtil;
import org.mapstruct.Named;

import java.io.Serializable;

/**
 * <p>
 * 通用转换器
 * </p>
 *
 * @author ${project.author}
 * @date ${.now}
 */
@Named("CommonConvert")
public class CommonConvert {

    /**
     * 样例方法
     *
     * @param id source字段绑定的参数
     * @return java.lang.String
     * @author ${project.author}
     * @date ${.now}
     */
    @Named("toName")
    public String toName(Serializable id) {
        if (id == null) {
            return null;
        }
        // MenuManager menuManager = SpringUtil.getBean(MenuManager.class);
        // return menuManager.getNameById(menuId);
        return "";
    }

}
