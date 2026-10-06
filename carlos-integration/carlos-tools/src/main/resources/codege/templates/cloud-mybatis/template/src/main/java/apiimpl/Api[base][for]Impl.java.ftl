package ${project.packageName}.apiimpl;


import lombok.RequiredArgsConstructor;
import ${project.packageName}.api.Api${table.classPrefix};
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * <p>
 * ${table.comment} api接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/${table.apiPath}")
@Tag(name = "${table.comment}Feign接口")
public class Api${table.classPrefix}Impl implements Api${table.classPrefix}{


}
