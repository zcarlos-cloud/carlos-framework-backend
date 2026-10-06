package ${project.packageName}.interfaces.web;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import ${project.packageName}.application.${table.classPrefix}AppService;
import ${project.packageName}.application.dto.${table.classPrefix}DTO;
import ${project.packageName}.application.param.${table.classPrefix}CreateParam;
import ${project.packageName}.application.param.${table.classPrefix}UpdateParam;
import ${project.packageName}.application.param.${table.classPrefix}PageParam;
import ${project.packageName}.application.vo.${table.classPrefix}VO;
import ${project.packageName}.infrastructure.convert.${table.classPrefix}Convert;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

/**
 * <p>
 * ${table.comment} rest服务接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("${table.apiPath}")
@Tag(name = "${table.comment}")
public class ${table.classPrefix}Controller {

    public static final String BASE_NAME = "${table.comment}";

    private final ${table.classPrefix}AppService ${table.classMainPrefix}AppService;

    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated ${table.classPrefix}CreateParam param) {
        ${table.classPrefix}DTO dto = ${table.classPrefix}Convert.INSTANCE.toDTO(param);
        ${table.classMainPrefix}AppService.add${table.classPrefix}(dto);
    }

    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody ParamIdSet<Serializable> param) {
        ${table.classMainPrefix}AppService.delete${table.classPrefix}(param.getIds());
    }

    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated ${table.classPrefix}UpdateParam param) {
        ${table.classPrefix}DTO dto = ${table.classPrefix}Convert.INSTANCE.toDTO(param);
        ${table.classMainPrefix}AppService.update${table.classPrefix}(dto);
    }

    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public ${table.classPrefix}VO detail(Long id) {
        return ${table.classPrefix}Convert.INSTANCE.toVO(${table.classMainPrefix}AppService.getDtoById(id));
    }

    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<${table.classPrefix}VO> page(${table.classPrefix}PageParam param) {
        return ${table.classMainPrefix}AppService.getPage(param);
    }
}
