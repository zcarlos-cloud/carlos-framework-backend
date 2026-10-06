-- ============================================================================
-- 开发工具平台（carlos-devkit）内置模板种子脚本（自动生成，请勿手改）
-- ----------------------------------------------------------------------------
-- 由 docs/db/gen-devkit-template-seed.mjs 从 carlos-tools 模板资源扫描生成。
-- 重新生成：node docs/db/gen-devkit-template-seed.mjs
--
-- 幂等：按 group_code / (group_id, file_path) 判重，可重复执行。
-- 模板正文用 dollar-quoting 包裹，避免 FreeMarker ${...} 与 SQL 拼接冲突。
-- id 号段：模板组 211000000000001xxxx，模板文件 211000000000002xxxx。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 模板组
-- ----------------------------------------------------------------------------
INSERT INTO dev_template_group (id, group_code, group_name, description, template_type, tech_stack,
                                db_type, template_root, logic_delete_fields, version_fields, common_fields,
                                built_in, status, sort_num, remark)
SELECT v.*
FROM (VALUES
    (2110000000000010001, 'cloud-mybatis', '微服务 MyBatis-Plus', 'mybatis-plus', 1, 'MYBATIS', 'ANY', 'template', 'is_deleted,is_delete,deleted,delete,delete_flag,del_flag', 'version', 'create_user,createUser,create_by,createBy,create_time,createTime,create_date,createDate,update_user,updateUser,update_by,updateBy,update_time,updateTime,update_date,updateDate,modify_user,modifyUser,modify_by,modifyBy,modify_time,modifyTime,modify_date,modifyDate', TRUE, 1, 1, '内置模板，来源于 carlos-tools'),
    (2110000000000010002, 'cloud-ddd-mybatis', '微服务 DDD 四层', 'mybatis-plus + DDD 领域驱动设计', 1, 'DDD_MYBATIS', 'ANY', 'template', 'is_deleted,is_delete,deleted,delete,delete_flag,del_flag', 'version', 'create_user,createUser,create_by,createBy,create_time,createTime,create_date,createDate,update_user,updateUser,update_by,updateBy,update_time,updateTime,update_date,updateDate,modify_user,modifyUser,modify_by,modifyBy,modify_time,modifyTime,modify_date,modifyDate', TRUE, 1, 2, '内置模板，来源于 carlos-tools'),
    (2110000000000010003, 'cloud-elasticsearch', '微服务 Elasticsearch', 'elasticsearch', 1, 'ELASTICSEARCH', 'ANY', 'template', 'is_deleted,is_delete,deleted,delete,delete_flag,del_flag', 'version', 'create_user,createUser,create_by,createBy,create_time,createTime,create_date,createDate,update_user,updateUser,update_by,updateBy,update_time,updateTime,update_date,updateDate,modify_user,modifyUser,modify_by,modifyBy,modify_time,modifyTime,modify_date,modifyDate', TRUE, 1, 3, '内置模板，来源于 carlos-tools'),
    (2110000000000010004, 'cloud-mongo', '微服务 MongoDB', 'MongoDB', 1, 'MONGO', 'ANY', 'template', 'is_deleted,is_delete,deleted,delete,delete_flag,del_flag', 'version', 'create_user,createUser,create_by,createBy,create_time,createTime,create_date,createDate,update_user,updateUser,update_by,updateBy,update_time,updateTime,update_date,updateDate,modify_user,modifyUser,modify_by,modifyBy,modify_time,modifyTime,modify_date,modifyDate', TRUE, 1, 4, '内置模板，来源于 carlos-tools'),
    (2110000000000010005, 'carlos-springboot', 'Spring Boot 单体工程', '基于Carlos自行封装的 Spring Boot 的单体应用架构模板', 2, 'SPRINGBOOT', NULL, 'template', '', '', '', TRUE, 1, 5, '内置模板，来源于 carlos-tools'),
    (2110000000000010006, 'carlos-springcloud', 'Spring Cloud 微服务工程', '基于carlos自行封装的 Spring Cloud Alibaba 的微服务架构模板，包含api接口模块，bus业务模块，boot单体启动模块，cloud微服务启动模块', 2, 'SPRINGCLOUD', NULL, 'template', '', '', '', TRUE, 1, 6, '内置模板，来源于 carlos-tools')
) AS v(id, group_code, group_name, description, template_type, tech_stack, db_type, template_root,
       logic_delete_fields, version_fields, common_fields, built_in, status, sort_num, remark)
WHERE NOT EXISTS (SELECT 1 FROM dev_template_group g WHERE g.group_code = v.group_code AND g.is_deleted = 0);

-- ----------------------------------------------------------------------------
-- 2. 模板文件
-- ----------------------------------------------------------------------------
INSERT INTO dev_template (id, group_id, file_path, file_name, file_ext, file_type, loop_flag,
                          content, content_encoding, file_size, md5, sort_num, status)
SELECT v.*
FROM (VALUES
    (2110000000000020001, 2110000000000010001, 'src/main/java/api/Api[base][for].java.ftl', 'Api[base][for].java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api;

import ${project.packageName}.ServiceNameConstant;
import ${project.packageName}.api.fallback.Api${table.classPrefix}FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * ${table.comment} feign 提供接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "${table.classMainPrefix}", path = "/api/${table.apiPath}", fallbackFactory = Api${table.classPrefix}FallbackFactory.class)
public interface Api${table.classPrefix} {


}
$devkit$, 'TEXT', 600, 'ed6556f0be9babe39125c6d0f03b29f9', 0, 1),
    (2110000000000020002, 2110000000000010001, 'src/main/java/api/fallback/Api[base][for]FallbackFactory.java.ftl', 'Api[base][for]FallbackFactory.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.fallback;

import ${project.packageName}.api.Api${table.classPrefix};
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
 * <p>
 * ${table.comment} api 降级
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
public class Api${table.classPrefix}FallbackFactory implements FallbackFactory<Api${table.classPrefix}> {

    @Override
    public Api${table.classPrefix} create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("${table.comment}服务调用失败: message:{}", message);
        return new Api${table.classPrefix}() {

        };
    }
}
$devkit$, 'TEXT', 722, 'd5b65dc8533371ebde6ea268c9fb60d2', 0, 1),
    (2110000000000020003, 2110000000000010001, 'src/main/java/api/pojo/ao/[base][for]AO.java.ftl', '[base][for]AO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.pojo.ao;


import lombok.Data;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
 * <p>
 * ${table.comment} API提供的对象(API Object)
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
public class ${table.classPrefix}AO implements Serializable{
<#list table.columns as column>
    <#if !column.logicField>
        /** ${column.columnComment} */
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 562, 'e34022730cdc7b00e67ce9bdecd36132', 0, 1),
    (2110000000000020004, 2110000000000010001, 'src/main/java/api/pojo/param/Api[base][for]Param.java.ftl', 'Api[base][for]Param.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
 * <p>
 * ${table.comment} API提供的对象(API Object)
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
public class Api${table.classPrefix}Param implements Serializable{
<#list table.columns as column>
    <#if !column.logicField>
    /** ${column.columnComment} */
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 626, '26cda6c6f3591ebae73435112e0b3e57', 0, 1),
    (2110000000000020005, 2110000000000010001, 'src/main/java/apiimpl/Api[base][for]Impl.java.ftl', 'Api[base][for]Impl.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.apiimpl;


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
$devkit$, 'TEXT', 648, '632e2aa5fb8049fbe451c70a5c4640c8', 0, 1),
    (2110000000000020006, 2110000000000010001, 'src/main/java/config/ApplicationExtendImpl.java.ftl', 'ApplicationExtendImpl.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName}.config;

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
$devkit$, 'TEXT', 863, '58e8c2f71c37a381ea709939d2941b80', 0, 1),
    (2110000000000020007, 2110000000000010001, 'src/main/java/controller/[base][for]Controller.java.ftl', '[base][for]Controller.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import ${project.packageName}.convert.${table.classPrefix}Convert;
import ${project.packageName}.pojo.param.${table.classPrefix}CreateParam;
import ${project.packageName}.pojo.param.${table.classPrefix}UpdateParam;
import ${project.packageName}.pojo.param.${table.classPrefix}PageParam;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.service.${table.classPrefix}Service;
import ${project.packageName}.manager.${table.classPrefix}Manager;
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

    private final ${table.classPrefix}Service ${table.classMainPrefix}Service;

    private final ${table.classPrefix}Manager ${table.classMainPrefix}Manager;



    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated ${table.classPrefix}CreateParam param) {
        ${table.classPrefix}DTO dto = ${table.classPrefix}Convert.INSTANCE.toDTO(param);
        ${table.classMainPrefix}Service.add${table.classPrefix}(dto);
    }


    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody ParamIdSet<Serializable> param) {
        ${table.classMainPrefix}Service.delete${table.classPrefix}(param.getIds());
    }


    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated ${table.classPrefix}UpdateParam param) {
        ${table.classPrefix}DTO dto = ${table.classPrefix}Convert.INSTANCE.toDTO(param);
        ${table.classMainPrefix}Service.update${table.classPrefix}(dto);
    }


    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public ${table.classPrefix}VO detail(Long id) {
        return ${table.classPrefix}Convert.INSTANCE.toVO(${table.classMainPrefix}Manager.getDtoById(id));
    }


    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<${table.classPrefix}VO> page(${table.classPrefix}PageParam param) {
        return ${table.classMainPrefix}Manager.getPage(param);
    }
}
$devkit$, 'TEXT', 2809, '232ba0b7645fbaa94ebb4e9b3ee75dac', 0, 1),
    (2110000000000020008, 2110000000000010001, 'src/main/java/convert/CommonConvert.java.ftl', 'CommonConvert.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName}.convert;

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
$devkit$, 'TEXT', 779, '3016180f00c2d63fedd3efc3417f07c0', 0, 1),
    (2110000000000020009, 2110000000000010001, 'src/main/java/convert/[base][for]Convert.java.ftl', '[base][for]Convert.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.convert;

import ${project.packageName}.pojo.param.${table.classPrefix}CreateParam;
import ${project.packageName}.pojo.param.${table.classPrefix}UpdateParam;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.convert.CommonConvert;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

/**
 * <p>
 * ${table.comment} 转换器
 * </p>
 *
 * @author ${project.author}
 * @date ${.now}
 */
@Mapper(uses = {CommonConvert.class})
public interface ${table.classPrefix}Convert {

    ${table.classPrefix}Convert INSTANCE = Mappers.getMapper(${table.classPrefix}Convert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}DTO toDTO(${table.classPrefix}CreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}DTO toDTO(${table.classPrefix}UpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author ${project.author}
     * @date ${.now}
     */
    List<${table.classPrefix}DTO> toDTO(List<${table.classPrefix}> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}DTO toDTO(${table.classPrefix} entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix} toDO(${table.classPrefix}DTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}VO toVO(${table.classPrefix}DTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author ${project.author}
     * @date ${.now}
     */
    List<${table.classPrefix}VO> toVO(List<${table.classPrefix}> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}VO toVO(${table.classPrefix} entity);
}
$devkit$, 'TEXT', 3069, '66579e01d9280022c9ac9e0523748a0f', 0, 1),
    (2110000000000020010, 2110000000000010001, 'src/main/java/manager/[base][for]Manager.java.ftl', '[base][for]Manager.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.pojo.param.${table.classPrefix}PageParam;

import java.io.Serializable;
/**
 * <p>
 * ${table.comment} 查询封装接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
public interface ${table.classPrefix}Manager extends BaseService<${table.classPrefix}>{

    /**
     * 新增${table.comment}
     *
     * @param dto ${table.comment}数据
     * @return boolean
     * @author  ${project.author}
     * @date    ${.now}
     */
    boolean add(${table.classPrefix}DTO dto);

    /**
     * 删除${table.comment}
     *
     * @param id ${table.comment}id
     * @return boolean
     * @author  ${project.author}
     * @date    ${.now}
     */
    boolean delete(Serializable id);

    /**
     * 修改${table.comment}信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author  ${project.author}
     * @date    ${.now}
     */
    boolean modify(${table.classPrefix}DTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
* @return ${project.packageName}.pojo.dto.${table.classPrefix}DTO
     * @author ${project.author}
     * @date   ${.now}
     */
    ${table.classPrefix}DTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author ${project.author}
     * @date   ${.now}
     */
    Paging<${table.classPrefix}VO> getPage(${table.classPrefix}PageParam param);
}
$devkit$, 'TEXT', 1817, 'f59ce34ee9291ff75dcfe4a54118c451', 0, 1),
    (2110000000000020011, 2110000000000010001, 'src/main/java/manager/impl/[base][for]ManagerImpl.java.ftl', '[base][for]ManagerImpl.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import ${project.packageName}.convert.${table.classPrefix}Convert;
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import ${project.packageName}.manager.${table.classPrefix}Manager;
import ${project.packageName}.mapper.${table.classPrefix}Mapper;
import ${project.packageName}.pojo.param.${table.classPrefix}PageParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
/**
 * <p>
 * ${table.comment} 查询封装实现类
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ${table.classPrefix}ManagerImpl  extends BaseServiceImpl<${table.classPrefix}Mapper, ${table.classPrefix}> implements ${table.classPrefix}Manager {

    @Override
    public boolean add(${table.classPrefix}DTO dto) {
        ${table.classPrefix} entity = ${table.classPrefix}Convert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert '${table.classPrefix}' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert '${table.classPrefix}' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        boolean success = removeById(id);
        if (!success) {
            log.warn("Remove '${table.classPrefix}' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove '${table.classPrefix}' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(${table.classPrefix}DTO dto) {
        ${table.classPrefix} entity = ${table.classPrefix}Convert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update '${table.classPrefix}' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update '${table.classPrefix}' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public ${table.classPrefix}DTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        ${table.classPrefix} entity = getBaseMapper().selectById(id);
        return ${table.classPrefix}Convert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<${table.classPrefix}VO> getPage(${table.classPrefix}PageParam param){
        LambdaQueryWrapper<${table.classPrefix}> wrapper = queryWrapper();
        wrapper.select(
    <#assign validColumns = []>
        <#list table.columns as column>
            <#if !column.logicField && !column.versionField>
                <#assign validColumns = validColumns + [column]>
            </#if>
        </#list>

    <#list validColumns as column>
        ${table.classPrefix}::get${column.propertyNameUp}<#if column_has_next>,</#if>
    </#list>
                );
        PageInfo<${table.classPrefix}> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, ${table.classPrefix}Convert.INSTANCE::toVO);
    }

}
$devkit$, 'TEXT', 3886, 'd32c80496a539c1bd693e1db3c9b2295', 0, 1),
    (2110000000000020012, 2110000000000010001, 'src/main/java/mapper/[base][for]Mapper.java.ftl', '[base][for]Mapper.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import org.apache.ibatis.annotations.Mapper;


/**
 * <p>
 * ${table.comment} 查询接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Mapper
public interface ${table.classPrefix}Mapper extends MPJBaseMapper<${table.classPrefix}> {


}
$devkit$, 'TEXT', 430, '5f1d703c33aa69103a0efa57e6c5c4a7', 0, 1),
    (2110000000000020013, 2110000000000010001, 'src/main/java/pojo/dto/[base][for]DTO.java.ftl', '[base][for]DTO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

<#list table.imports as import>
import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
public class ${table.classPrefix}DTO {
<#list table.columns as column>
    <#if !column.logicField>
    /** ${column.columnComment} */
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 586, '3d5cda9a70dddc9a4d55fc76f52c0b17', 0, 1),
    (2110000000000020014, 2110000000000010001, 'src/main/java/pojo/entity/[base][for].java.ftl', '[base][for].java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 数据源对象
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("${table.name}")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix} extends Model<${table.classPrefix}> implements Serializable{
private static final long serialVersionUID = 1L;
<#list table.columns as column>
    /**
    * ${column.columnComment}
    */
    <#if column.logicField>
    @TableLogic
    </#if>
    <#if column.versionField>
    @Version
    </#if>
    <#if column.primaryKey>
    @TableId(type = IdType.ASSIGN_ID, value = "${column.columnName}")
    <#else>
    @TableField(value = "${column.columnName}")
    </#if>
    private ${column.javaType} ${column.propertyName};
</#list>

}
$devkit$, 'TEXT', 1258, 'b1c2f071321157e7040632f50d72550f', 0, 1),
    (2110000000000020015, 2110000000000010001, 'src/main/java/pojo/param/[base][for]CreateParam.java.ftl', '[base][for]CreateParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
<#list table.imports as import>
    import ${import};
</#list>


/**
 * <p>
 * ${table.comment} 新增参数封装
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@Schema(description =  "${table.comment}新增参数")
public class ${table.classPrefix}CreateParam {
<#list table.columns as column>
    <#if !column.commonField && !column.primaryKey && !column.logicField  && !column.versionField>
        <#if !column.nullable>
            <#if column.javaType == 'String'>
    @NotBlank(message = "${column.columnComment}不能为空")
            <#else>
    @NotNull(message = "${column.columnComment}不能为空")
            </#if>
        </#if>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 1124, '73956752c140a3148a2e6ec1602bdcdc', 0, 1),
    (2110000000000020016, 2110000000000010001, 'src/main/java/pojo/param/[base][for]PageParam.java.ftl', '[base][for]PageParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.carlos.core.param.ParamPage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

<#list table.imports as import>
    import ${import};
</#list>


/**
 * <p>
 * ${table.comment} 列表查询参数封装
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description =  "${table.comment}列表查询参数")
public class ${table.classPrefix}PageParam extends ParamPage {
<#list table.columns as column>
    <#if !column.commonField && !column.primaryKey && !column.logicField  && !column.versionField>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
@Schema(description = "开始时间")
    private LocalDateTime start;

@Schema(description = "结束时间")
    private LocalDateTime end;
}
$devkit$, 'TEXT', 1183, '5a1bcd93b3a77bed12a77f27851272ac', 0, 1),
    (2110000000000020017, 2110000000000010001, 'src/main/java/pojo/param/[base][for]UpdateParam.java.ftl', '[base][for]UpdateParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} 更新参数封装
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
@Schema(description =  "${table.comment}修改参数")
public class ${table.classPrefix}UpdateParam {
<#list table.columns as column>
    <#if !column.commonField && !column.logicField  && !column.versionField>
        <#if column.primaryKey>
            <#if column.javaType == 'String'>
    @NotBlank(message = "${column.columnComment}不能为空")
            <#else>
    @NotNull(message = "${column.columnComment}不能为空")
            </#if>
        </#if>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 1102, '74e413d8fec5bb492fb5329e6516aeb1', 0, 1),
    (2110000000000020018, 2110000000000010001, 'src/main/java/pojo/vo/[base][for]VO.java.ftl', '[base][for]VO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
<#list table.imports as import>
import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 显示层对象，向页面传输的对象
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix}VO implements Serializable {
private static final long serialVersionUID = 1L;
<#list table.columns as column>
    <#if !column.logicField && !column.versionField>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>

}
$devkit$, 'TEXT', 870, 'cf294082470dbc748c20c48623c70b28', 0, 1),
    (2110000000000020019, 2110000000000010001, 'src/main/java/service/[base][for]Service.java.ftl', '[base][for]Service.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.service;

import ${project.packageName}.manager.${table.classPrefix}Manager;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.service.${table.classPrefix}Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * ${table.comment} 业务
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ${table.classPrefix}Service {

private final ${table.classPrefix}Manager ${table.classMainPrefix}Manager;

    /**
     * 新增${table.comment}
     *
     * @param dto ${table.comment}数据
     * @author  ${project.author}
     * @date    ${.now}
     */
    public void add${table.classPrefix}(${table.classPrefix}DTO dto){
        boolean success = ${table.classMainPrefix}Manager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert '${table.classPrefix}' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除${table.comment}
     *
     * @param ids ${table.comment}id
     * @author  ${project.author}
     * @date    ${.now}
     */
    public void delete${table.classPrefix}(Set<Serializable> ids){
        for (Serializable id : ids) {
        boolean success = ${table.classMainPrefix}Manager.delete(id);
        if (!success) {
            // 删除失败的措施
            continue;
        }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改${table.comment}信息
     *
     * @param dto 对象信息
     * @author  ${project.author}
     * @date    ${.now}
     */
    public void update${table.classPrefix}(${table.classPrefix}DTO dto){
        boolean success = ${table.classMainPrefix}Manager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update '${table.classPrefix}' data: id:{}", dto.getId());
    }

}
$devkit$, 'TEXT', 2278, 'a4f3bfbc50d08324d53fcaf3628ac877', 0, 1),
    (2110000000000020020, 2110000000000010001, 'src/main/resources/mapper/[base][for]Mapper.xml.ftl', '[base][for]Mapper.xml', 'ftl', 1, TRUE, $devkit$<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="${project.packageName}.mapper.${table.classPrefix}Mapper">
    <resultMap type="${project.packageName}.pojo.entity.${table.classPrefix}" id="baseMap">
        <#list table.columns as column>
            <#if column.primaryKey>
                <id column="${column.columnName}" property="${column.propertyName}"/>
            <#else>
                <#if !column.logicField>
                <result column="${column.columnName}" property="${column.propertyName}"/>
                </#if>
            </#if>
        </#list>
    </resultMap>


</mapper>
$devkit$, 'TEXT', 735, '5b3e26e95f2f44fd7747e596f2137f70', 0, 1),
    (2110000000000020021, 2110000000000010002, 'src/main/java/api/Api[base][for].java.ftl', 'Api[base][for].java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api;

import ${project.packageName}.ServiceNameConstant;
import ${project.packageName}.api.fallback.Api${table.classPrefix}FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * ${table.comment} feign 提供接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "${table.classMainPrefix}", path = "/api/${table.apiPath}", fallbackFactory = Api${table.classPrefix}FallbackFactory.class)
public interface Api${table.classPrefix} {


}
$devkit$, 'TEXT', 600, 'ed6556f0be9babe39125c6d0f03b29f9', 0, 1),
    (2110000000000020022, 2110000000000010002, 'src/main/java/api/fallback/Api[base][for]FallbackFactory.java.ftl', 'Api[base][for]FallbackFactory.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.fallback;

import ${project.packageName}.api.Api${table.classPrefix};
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * <p>
 * ${table.comment} api 降级
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
public class Api${table.classPrefix}FallbackFactory implements FallbackFactory<Api${table.classPrefix}> {

    @Override
    public Api${table.classPrefix} create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("${table.comment}服务调用失败: message:{}", message);
        return new Api${table.classPrefix}() {

        };
    }
}
$devkit$, 'TEXT', 720, 'a5a3969b0cb0e8014c4fc0c644391b09', 0, 1),
    (2110000000000020023, 2110000000000010002, 'src/main/java/api/pojo/ao/[base][for]AO.java.ftl', '[base][for]AO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
 * <p>
 * ${table.comment} API提供的对象(API Object)
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
public class ${table.classPrefix}AO implements Serializable{
<#list table.columns as column>
    <#if !column.logicField>
        /** ${column.columnComment} */
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 560, 'aed2007913a2b23ae6d6d792e39233b8', 0, 1),
    (2110000000000020024, 2110000000000010002, 'src/main/java/api/pojo/param/Api[base][for]Param.java.ftl', 'Api[base][for]Param.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.pojo.param;

import lombok.Data;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
 * <p>
 * ${table.comment} API请求参数
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
public class Api${table.classPrefix}Param implements Serializable{
<#list table.columns as column>
    <#if !column.logicField>
        /** ${column.columnComment} */
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 554, 'ded46ff45c7c812b40427bce3b2455ad', 0, 1),
    (2110000000000020025, 2110000000000010002, 'src/main/java/apiimpl/Api[base][for]Impl.java.ftl', 'Api[base][for]Impl.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.apiimpl;

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
public class Api${table.classPrefix}Impl implements Api${table.classPrefix} {


}
$devkit$, 'TEXT', 649, 'c761d574cfe150eb9f0d58c55c35df8d', 0, 1),
    (2110000000000020026, 2110000000000010002, 'src/main/java/application/[base][for]AppService.java.ftl', '[base][for]AppService.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.application;

import com.carlos.core.pagination.Paging;
import ${project.packageName}.application.dto.${table.classPrefix}DTO;
import ${project.packageName}.application.param.${table.classPrefix}PageParam;
import ${project.packageName}.application.vo.${table.classPrefix}VO;
import ${project.packageName}.domain.aggregate.${table.classPrefix};
import ${project.packageName}.domain.repository.${table.classPrefix}Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Set;

/**
 * <p>
 * ${table.comment} 应用服务层
 * </p>
 * <p>负责业务流程编排与事务控制，不包含领域业务规则</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ${table.classPrefix}AppService {

    private final ${table.classPrefix}Repository ${table.classMainPrefix}Repository;

    /**
     * 新增${table.comment}
     *
     * @param dto ${table.comment}数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void add${table.classPrefix}(${table.classPrefix}DTO dto) {
        ${table.classPrefix} entity = new ${table.classPrefix}();
        // TODO: 将 DTO 属性映射到领域实体
        ${table.classMainPrefix}Repository.save(entity);
        log.info("新增${table.comment}成功，id:{}", entity.getId());
    }

    /**
     * 删除${table.comment}
     *
     * @param ids ${table.comment}id集合
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete${table.classPrefix}(Set<Serializable> ids) {
        for (Serializable id : ids) {
            ${table.classPrefix} entity = ${table.classMainPrefix}Repository.findById(id);
            if (entity != null) {
                ${table.classMainPrefix}Repository.remove(entity);
            }
        }
        log.info("删除${table.comment}成功，ids:{}", ids);
    }

    /**
     * 修改${table.comment}信息
     *
     * @param dto 对象信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void update${table.classPrefix}(${table.classPrefix}DTO dto) {
        ${table.classPrefix} entity = ${table.classMainPrefix}Repository.findById(dto.getId());
        if (entity == null) {
            log.warn("${table.comment}不存在，id:{}", dto.getId());
            return;
        }
        // TODO: 将 DTO 属性映射到领域实体并执行业务操作
        ${table.classMainPrefix}Repository.save(entity);
        log.info("修改${table.comment}成功，id:{}", dto.getId());
    }

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return ${table.classPrefix}DTO
     */
    public ${table.classPrefix}DTO getDtoById(Serializable id) {
        ${table.classPrefix} entity = ${table.classMainPrefix}Repository.findById(id);
        if (entity == null) {
            return null;
        }
        ${table.classPrefix}DTO dto = new ${table.classPrefix}DTO();
        // TODO: 将领域实体属性映射到 DTO
        return dto;
    }

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @return 分页数据
     */
    public Paging<${table.classPrefix}VO> getPage(${table.classPrefix}PageParam param) {
        // TODO: 调用 Repository 或领域服务实现分页查询
        return null;
    }
}
$devkit$, 'TEXT', 3596, 'e5888bafa9513d06cb673b25d71ba543', 0, 1),
    (2110000000000020027, 2110000000000010002, 'src/main/java/application/dto/[base][for]DTO.java.ftl', '[base][for]DTO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.application.dto;

import lombok.Data;
import lombok.experimental.Accessors;

<#list table.imports as import>
import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 数据传输对象，用于应用层与领域层之间的数据传递
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
public class ${table.classPrefix}DTO {
<#list table.columns as column>
    <#if !column.logicField>
    /** ${column.columnComment} */
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 604, '7e8a47ed207e66fd91b306193a2203cb', 0, 1),
    (2110000000000020028, 2110000000000010002, 'src/main/java/application/param/[base][for]CreateParam.java.ftl', '[base][for]CreateParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.application.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
<#list table.imports as import>
    import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 新增参数封装
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@Schema(description =  "${table.comment}新增参数")
public class ${table.classPrefix}CreateParam {
<#list table.columns as column>
    <#if !column.commonField && !column.primaryKey && !column.logicField  && !column.versionField>
        <#if !column.nullable>
            <#if column.javaType == 'String'>
    @NotBlank(message = "${column.columnComment}不能为空")
            <#else>
    @NotNull(message = "${column.columnComment}不能为空")
            </#if>
        </#if>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 1127, 'e9e6b1379a699a9611a39add56cd4470', 0, 1),
    (2110000000000020029, 2110000000000010002, 'src/main/java/application/param/[base][for]PageParam.java.ftl', '[base][for]PageParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.application.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.carlos.core.param.ParamPage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

<#list table.imports as import>
    import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 列表查询参数封装
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description =  "${table.comment}列表查询参数")
public class ${table.classPrefix}PageParam extends ParamPage {
<#list table.columns as column>
    <#if !column.commonField && !column.primaryKey && !column.logicField  && !column.versionField>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
@Schema(description = "开始时间")
    private LocalDateTime start;

@Schema(description = "结束时间")
    private LocalDateTime end;
}
$devkit$, 'TEXT', 1186, '39251eddb74e727ea9d06f30af924b0a', 0, 1),
    (2110000000000020030, 2110000000000010002, 'src/main/java/application/param/[base][for]UpdateParam.java.ftl', '[base][for]UpdateParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.application.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} 更新参数封装
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
@Schema(description =  "${table.comment}修改参数")
public class ${table.classPrefix}UpdateParam {
<#list table.columns as column>
    <#if !column.commonField && !column.logicField  && !column.versionField>
        <#if column.primaryKey>
            <#if column.javaType == 'String'>
    @NotBlank(message = "${column.columnComment}不能为空")
            <#else>
    @NotNull(message = "${column.columnComment}不能为空")
            </#if>
        </#if>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 1107, 'e166a3315c0f75c2ff50551620d5697a', 0, 1),
    (2110000000000020031, 2110000000000010002, 'src/main/java/application/vo/[base][for]VO.java.ftl', '[base][for]VO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.application.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
<#list table.imports as import>
import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 视图对象，响应给前端的数据展示对象
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix}VO implements Serializable {
private static final long serialVersionUID = 1L;
<#list table.columns as column>
    <#if !column.logicField && !column.versionField>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>

}
$devkit$, 'TEXT', 886, '21f8a0e5c01507d9b94e34e8cdf9f5f1', 0, 1),
    (2110000000000020032, 2110000000000010002, 'src/main/java/config/ApplicationExtendImpl.java.ftl', 'ApplicationExtendImpl.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName}.config;

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
$devkit$, 'TEXT', 862, '64782ef27e055793499fe4a7b84b9f0c', 0, 1),
    (2110000000000020033, 2110000000000010002, 'src/main/java/domain/aggregate/[base][for].java.ftl', '[base][for].java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.domain.aggregate;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
<#list table.imports as import>
import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 领域实体（聚合根）
 * </p>
 * <p>封装业务规则与领域行为，属于充血模型</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
public class ${table.classPrefix} implements Serializable {

    private static final long serialVersionUID = 1L;

<#list table.columns as column>
    <#if !column.logicField>
    /**
     * ${column.columnComment}
     */
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>

    /**
     * 创建新${table.comment}领域实体
     *
     * @return ${table.classPrefix}
     */
    public static ${table.classPrefix} create() {
        return new ${table.classPrefix}();
    }

    /**
     * 执行业务更新操作
     * TODO: 根据实际业务补充领域行为
     */
    public void update() {
        // 领域业务规则校验与状态变更
    }

    /**
     * 逻辑删除标记
     */
    public void remove() {
        // TODO: 执行领域层面的删除前校验与清理
    }
}
$devkit$, 'TEXT', 1302, 'b1e74caf241c8064db2a0687fef3f669', 0, 1),
    (2110000000000020034, 2110000000000010002, 'src/main/java/domain/event/[base][for]CreatedEvent.java.ftl', '[base][for]CreatedEvent.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.domain.event;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * ${table.comment} 已创建领域事件
 * </p>
 * <p>当${table.comment}被成功创建后发布，供其他限界上下文订阅处理</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Getter
public class ${table.classPrefix}CreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ${table.comment}ID
     */
    private final Serializable id;

    /**
     * 发生时间
     */
    private final LocalDateTime occurredOn;

    public ${table.classPrefix}CreatedEvent(Serializable id) {
        this.id = id;
        this.occurredOn = LocalDateTime.now();
    }
}
$devkit$, 'TEXT', 812, 'dae4c4aefaa2eade57073c6bceb724ea', 0, 1),
    (2110000000000020035, 2110000000000010002, 'src/main/java/domain/repository/[base][for]Repository.java.ftl', '[base][for]Repository.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.domain.repository;

import ${project.packageName}.domain.aggregate.${table.classPrefix};

import java.io.Serializable;

/**
 * <p>
 * ${table.comment} 仓储接口
 * </p>
 * <p>定义领域对象的持久化契约，由基础设施层实现</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
public interface ${table.classPrefix}Repository {

    /**
     * 根据ID查询领域实体
     *
     * @param id 主键
     * @return ${table.classPrefix}
     */
    ${table.classPrefix} findById(Serializable id);

    /**
     * 保存领域实体（新增或更新）
     *
     * @param entity 领域实体
     */
    void save(${table.classPrefix} entity);

    /**
     * 移除领域实体
     *
     * @param entity 领域实体
     */
    void remove(${table.classPrefix} entity);
}
$devkit$, 'TEXT', 874, '6b27607adb2dfcfdd4cb6b9f6a4e0159', 0, 1),
    (2110000000000020036, 2110000000000010002, 'src/main/java/infrastructure/convert/[base][for]Convert.java.ftl', '[base][for]Convert.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.infrastructure.convert;

import ${project.packageName}.application.dto.${table.classPrefix}DTO;
import ${project.packageName}.application.param.${table.classPrefix}CreateParam;
import ${project.packageName}.application.param.${table.classPrefix}UpdateParam;
import ${project.packageName}.application.vo.${table.classPrefix}VO;
import ${project.packageName}.domain.aggregate.${table.classPrefix};
import ${project.packageName}.infrastructure.persistence.${table.classPrefix}PO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * ${table.comment} 对象转换器
 * </p>
 * <p>负责 Param/DTO/Domain/PO/VO 之间的类型转换</p>
 *
 * @author ${project.author}
 * @date ${.now}
 */
@Mapper
public interface ${table.classPrefix}Convert {

    ${table.classPrefix}Convert INSTANCE = Mappers.getMapper(${table.classPrefix}Convert.class);

    // Param -> DTO
    ${table.classPrefix}DTO toDTO(${table.classPrefix}CreateParam param);

    ${table.classPrefix}DTO toDTO(${table.classPrefix}UpdateParam param);

    // PO -> Domain
    ${table.classPrefix} toDomain(${table.classPrefix}PO po);

    // Domain -> PO
    ${table.classPrefix}PO toPO(${table.classPrefix} domain);

    // DTO -> VO
    ${table.classPrefix}VO toVO(${table.classPrefix}DTO dto);

    // Domain -> DTO
    ${table.classPrefix}DTO toDTO(${table.classPrefix} domain);

    // PO -> VO
    ${table.classPrefix}VO toVO(${table.classPrefix}PO po);

    List<${table.classPrefix}VO> toVO(List<${table.classPrefix}PO> pos);

    List<${table.classPrefix}DTO> toDTO(List<${table.classPrefix}PO> pos);
}
$devkit$, 'TEXT', 1705, '7dcbd2527821d7c576ea3d3f61e0a9a1', 0, 1),
    (2110000000000020037, 2110000000000010002, 'src/main/java/infrastructure/mapper/[base][for]Mapper.java.ftl', '[base][for]Mapper.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.infrastructure.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import ${project.packageName}.infrastructure.persistence.${table.classPrefix}PO;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * ${table.comment} 数据访问层接口
 * </p>
 * <p>基于 MyBatis-Plus + MyBatis-Plus-Join 实现，仅由 Repository 调用</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Mapper
public interface ${table.classPrefix}Mapper extends MPJBaseMapper<${table.classPrefix}PO> {

}
$devkit$, 'TEXT', 554, '7e94581da824c3d707f8a407b7881d47', 0, 1),
    (2110000000000020038, 2110000000000010002, 'src/main/java/infrastructure/persistence/[base][for]PO.java.ftl', '[base][for]PO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
<#list table.imports as import>
import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 持久化对象（Persistence Object）
 * </p>
 * <p>与数据库表结构一一对应，仅用于基础设施层数据持久化</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("${table.name}")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix}PO extends Model<${table.classPrefix}PO> implements Serializable {

    private static final long serialVersionUID = 1L;

<#list table.columns as column>
    /**
     * ${column.columnComment}
     */
    <#if column.logicField>
    @TableLogic
    </#if>
    <#if column.versionField>
    @Version
    </#if>
    <#if column.primaryKey>
    @TableId(type = IdType.ASSIGN_ID, value = "${column.columnName}")
    <#else>
    @TableField(value = "${column.columnName}")
    </#if>
    private ${column.javaType} ${column.propertyName};
</#list>

}
$devkit$, 'TEXT', 1393, '7671dfbbd0205b129b4dab42866fc34e', 0, 1),
    (2110000000000020039, 2110000000000010002, 'src/main/java/infrastructure/repository/[base][for]RepositoryImpl.java.ftl', '[base][for]RepositoryImpl.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.infrastructure.repository;

import ${project.packageName}.domain.aggregate.${table.classPrefix};
import ${project.packageName}.domain.repository.${table.classPrefix}Repository;
import ${project.packageName}.infrastructure.convert.${table.classPrefix}Convert;
import ${project.packageName}.infrastructure.mapper.${table.classPrefix}Mapper;
import ${project.packageName}.infrastructure.persistence.${table.classPrefix}PO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

/**
 * <p>
 * ${table.comment} 仓储实现
 * </p>
 * <p>基于 MyBatis-Plus 实现领域对象的持久化，隔离 ORM 细节</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ${table.classPrefix}RepositoryImpl implements ${table.classPrefix}Repository {

    private final ${table.classPrefix}Mapper ${table.classMainPrefix}Mapper;

    @Override
    public ${table.classPrefix} findById(Serializable id) {
        if (id == null) {
            return null;
        }
        ${table.classPrefix}PO po = ${table.classMainPrefix}Mapper.selectById(id);
        return ${table.classPrefix}Convert.INSTANCE.toDomain(po);
    }

    @Override
    public void save(${table.classPrefix} entity) {
        ${table.classPrefix}PO po = ${table.classPrefix}Convert.INSTANCE.toPO(entity);
        if (po.getId() == null) {
            ${table.classMainPrefix}Mapper.insert(po);
            entity.setId(po.getId());
        } else {
            ${table.classMainPrefix}Mapper.updateById(po);
        }
    }

    @Override
    public void remove(${table.classPrefix} entity) {
        if (entity == null || entity.getId() == null) {
            return;
        }
        ${table.classMainPrefix}Mapper.deleteById(entity.getId());
    }
}
$devkit$, 'TEXT', 1960, 'd87418b03e6fba3fa2aa50ba01844270', 0, 1),
    (2110000000000020040, 2110000000000010002, 'src/main/java/interfaces/web/[base][for]Controller.java.ftl', '[base][for]Controller.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.interfaces.web;

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
$devkit$, 'TEXT', 2737, '15ab3a26eafa249663e6f4774ff6ac51', 0, 1),
    (2110000000000020041, 2110000000000010002, 'src/main/resources/mapper/[base][for]Mapper.xml.ftl', '[base][for]Mapper.xml', 'ftl', 1, TRUE, $devkit$<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="${project.packageName}.infrastructure.mapper.${table.classPrefix}Mapper">
    <resultMap type="${project.packageName}.infrastructure.persistence.${table.classPrefix}PO" id="baseMap">
        <#list table.columns as column>
            <#if column.primaryKey>
                <id column="${column.columnName}" property="${column.propertyName}"/>
            <#else>
                <#if !column.logicField>
                <result column="${column.columnName}" property="${column.propertyName}"/>
                </#if>
            </#if>
        </#list>
    </resultMap>


</mapper>
$devkit$, 'TEXT', 767, '8ff391e0a0dc32929114b72fa7e4e589', 0, 1),
    (2110000000000020042, 2110000000000010003, 'src/main/java/api/Api[base][for].java.ftl', 'Api[base][for].java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api;

import ${project.packageName}.ServiceNameConstant;
import ${project.packageName}.api.fallback.Api${table.classPrefix}FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;

/**
* <p>
    * ${table.comment} feign 提供接口
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "${table.classMainPrefix}", path = "/api/${table.apiPath}", fallbackFactory = Api${table.classPrefix}FallbackFactory.class)
public interface Api${table.classPrefix} {


}
$devkit$, 'TEXT', 601, '25329ca8881bb03672db9fae250bc1a1', 0, 1),
    (2110000000000020043, 2110000000000010003, 'src/main/java/api/fallback/Api[base][for]FallbackFactory.java.ftl', 'Api[base][for]FallbackFactory.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.fallback;

import ${project.packageName}.api.Api${table.classPrefix};
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
* <p>
    * ${table.comment} api 降级
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Slf4j
public class Api${table.classPrefix}FallbackFactory implements FallbackFactory
<Api${table.classPrefix}> {

    @Override
    public Api${table.classPrefix} create(Throwable throwable) {
    String message = throwable.getMessage();
    log.error("${table.comment}服务调用失败: message:{}", message);
    return new Api${table.classPrefix}() {

    };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public Api${table.classPrefix}FallbackFactory ${table.classMainPrefix}FallbackFactory() {
    // return new Api${table.classPrefix}FallbackFactory();
    // }
    }$devkit$, 'TEXT', 956, '01f2a85b4453f46cce2ce92e3f4b6ca6', 0, 1),
    (2110000000000020044, 2110000000000010003, 'src/main/java/api/pojo/ao/[base][for]AO.java.ftl', '[base][for]AO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.pojo.ao;


import lombok.Data;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} API提供的对象(API Object)
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
public class ${table.classPrefix}AO implements Serializable{
<#list table.columns as column>
    <#if !column.logicField>
        /** ${column.columnComment} */
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 563, '0088aa8580f8b4c039a0e4e1beeb7d4f', 0, 1),
    (2110000000000020045, 2110000000000010003, 'src/main/java/api/pojo/param/Api[base][for]Param.java.ftl', 'Api[base][for]Param.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} API提供的对象(API Object)
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
public class Api${table.classPrefix}Param implements Serializable{
<#list table.columns as column>
    <#if !column.logicField>
        /** ${column.columnComment} */
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 635, 'a44cf88cdda307e99769a11e099c7c1d', 0, 1),
    (2110000000000020046, 2110000000000010003, 'src/main/java/apiimpl/Api[base][for]Impl.java.ftl', 'Api[base][for]Impl.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.apiimpl;


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
$devkit$, 'TEXT', 648, '632e2aa5fb8049fbe451c70a5c4640c8', 0, 1),
    (2110000000000020047, 2110000000000010003, 'src/main/java/config/ApplicationExtendImpl.java.ftl', 'ApplicationExtendImpl.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName}.config;

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
$devkit$, 'TEXT', 839, '6bf03875db5c3d41ef234fe95601c1aa', 0, 1),
    (2110000000000020048, 2110000000000010003, 'src/main/java/controller/[base][for]Controller.java.ftl', '[base][for]Controller.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import ${project.packageName}.convert.${table.classPrefix}Convert;
import ${project.packageName}.pojo.param.${table.classPrefix}CreateParam;
import ${project.packageName}.pojo.param.${table.classPrefix}UpdateParam;
import ${project.packageName}.pojo.param.${table.classPrefix}PageParam;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.service.${table.classPrefix}Service;
import ${project.packageName}.manager.${table.classPrefix}Manager;
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

    private final ${table.classPrefix}Service ${table.classMainPrefix}Service;

    private final ${table.classPrefix}Manager ${table.classMainPrefix}Manager;


@PostMapping("add")
@Operation(summary = "新增" + BASE_NAME)
public void add(@RequestBody @Validated ${table.classPrefix}CreateParam param) {
        ${table.classPrefix}DTO dto = ${table.classPrefix}Convert.INSTANCE.toDTO(param);
${table.classMainPrefix}Service.add${table.classPrefix}(dto);
    }


@PostMapping("delete")
@Operation(summary = "删除" + BASE_NAME)
public void delete(@RequestBody ParamIdSet
<Serializable> param) {
    ${table.classMainPrefix}Service.delete${table.classPrefix}(param.getIds());
    }


    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated ${table.classPrefix}UpdateParam param) {
        ${table.classPrefix}DTO dto = ${table.classPrefix}Convert.INSTANCE.toDTO(param);
    ${table.classMainPrefix}Service.update${table.classPrefix}(dto);
    }


    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public ${table.classPrefix}VO detail(Long id) {
        return ${table.classPrefix}Convert.INSTANCE.toVO(${table.classMainPrefix}Manager.getDtoById(id));
    }


    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging
    <${table.classPrefix}VO> page(${table.classPrefix}PageParam param) {
        return ${table.classMainPrefix}Manager.getPage(param);
        }
}
$devkit$, 'TEXT', 2778, 'b9507044e3e91fc2e0640579fd15b9c9', 0, 1),
    (2110000000000020049, 2110000000000010003, 'src/main/java/convert/CommonConvert.java.ftl', 'CommonConvert.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName}.convert;

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
$devkit$, 'TEXT', 779, '3016180f00c2d63fedd3efc3417f07c0', 0, 1),
    (2110000000000020050, 2110000000000010003, 'src/main/java/convert/[base][for]Convert.java.ftl', '[base][for]Convert.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.convert;

import ${project.packageName}.pojo.param.${table.classPrefix}CreateParam;
import ${project.packageName}.pojo.param.${table.classPrefix}UpdateParam;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.convert.CommonConvert;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

/**
 * <p>
 * ${table.comment} 转换器
 * </p>
 *
 * @author ${project.author}
 * @date ${.now}
 */
@Mapper(uses = {CommonConvert.class})
public interface ${table.classPrefix}Convert {

    ${table.classPrefix}Convert INSTANCE = Mappers.getMapper(${table.classPrefix}Convert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}DTO toDTO(${table.classPrefix}CreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}DTO toDTO(${table.classPrefix}UpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author ${project.author}
     * @date ${.now}
     */
    List<${table.classPrefix}DTO> toDTO(List<${table.classPrefix}> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}DTO toDTO(${table.classPrefix} entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix} toDO(${table.classPrefix}DTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}VO toVO(${table.classPrefix}DTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author ${project.author}
     * @date ${.now}
     */
    List<${table.classPrefix}VO> toVO(List<${table.classPrefix}> dos);

        /**
        * 数据持久对象转数据显示对象
        *
        * @param entity 数据传输对象
        * @return 数据显示对象
        * @author ${project.author}
        * @date ${.now}
        */
        ${table.classPrefix}VO toVO(${table.classPrefix} entity);
}
$devkit$, 'TEXT', 3098, '010a85688acef19697efc35f26040a9a', 0, 1),
    (2110000000000020051, 2110000000000010003, 'src/main/java/manager/[base][for]Manager.java.ftl', '[base][for]Manager.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.pojo.param.${table.classPrefix}PageParam;

import java.io.Serializable;
/**
 * <p>
 * ${table.comment} 查询封装接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
public interface ${table.classPrefix}Manager extends BaseService<${table.classPrefix}>{

/**
* 新增${table.comment}
*
* @param dto ${table.comment}数据
* @return boolean
* @author  ${project.author}
* @date    ${.now}
*/
boolean add(${table.classPrefix}DTO dto);

/**
* 删除${table.comment}
*
* @param id ${table.comment}id
* @return boolean
* @author  ${project.author}
* @date    ${.now}
*/
boolean delete(Serializable id);

/**
* 修改${table.comment}信息
*
* @param dto 对象信息
* @return boolean
* @author  ${project.author}
* @date    ${.now}
*/
boolean modify(${table.classPrefix}DTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
* @return ${project.packageName}.pojo.dto.${table.classPrefix}DTO
     * @author ${project.author}
     * @date   ${.now}
     */
${table.classPrefix}DTO getDtoById(Serializable id);

/**
* 分页列表
*
* @param  param 分页参数
* @author ${project.author}
* @date   ${.now}
*/
Paging
<${table.classPrefix}VO> getPage(${table.classPrefix}PageParam param);
}
$devkit$, 'TEXT', 1648, '5368117d381211f1270f9a4c3c0eff02', 0, 1),
    (2110000000000020052, 2110000000000010003, 'src/main/java/manager/impl/[base][for]ManagerImpl.java.ftl', '[base][for]ManagerImpl.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import ${project.packageName}.convert.${table.classPrefix}Convert;
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import ${project.packageName}.manager.${table.classPrefix}Manager;
import ${project.packageName}.mapper.${table.classPrefix}Mapper;
import ${project.packageName}.pojo.param.${table.classPrefix}PageParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
/**
 * <p>
 * ${table.comment} 查询封装实现类
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ${table.classPrefix}ManagerImpl  extends BaseServiceImpl
<${table.classPrefix}Mapper, ${table.classPrefix}> implements ${table.classPrefix}Manager {

@Override
public boolean add(${table.classPrefix}DTO dto) {
${table.classPrefix} entity = ${table.classPrefix}Convert.INSTANCE.toDO(dto);
boolean success = save(entity);
if (!success) {
log.warn("Insert '${table.classPrefix}' data fail, entity:{}", entity);
return false;
}
dto.setId(entity.getId());
// 保存完成的后续
log.debug("Insert '${table.classPrefix}' data: id:{}", entity.getId());
return true;
}

@Override
public boolean delete(Serializable id) {
if (id == null) {
log.warn("id can't be null");
return false;
}
boolean success = removeById(id);
if (!success) {
log.warn("Remove '${table.classPrefix}' data fail, id:{}", id);
return false;
}
log.debug("Remove '${table.classPrefix}' data by id:{}", id);
return true;
}

@Override
public boolean modify(${table.classPrefix}DTO dto) {
${table.classPrefix} entity = ${table.classPrefix}Convert.INSTANCE.toDO(dto);
boolean success = updateById(entity);
if (!success) {
log.warn("Update '${table.classPrefix}' data fail, entity:{}", entity);
return false;
}
// 修改成功的后续操作
log.debug("Update '${table.classPrefix}' data by id:{}", dto.getId());
return true;
}

    @Override
public ${table.classPrefix}DTO getDtoById(Serializable id) {
if (id == null) {
log.warn("id is null");
return null;
}
${table.classPrefix} entity = getBaseMapper().selectById(id);
        return ${table.classPrefix}Convert.INSTANCE.toDTO(entity);
    }

@Override
public Paging
<${table.classPrefix}VO> getPage(${table.classPrefix}PageParam param){
    LambdaQueryWrapper<${table.classPrefix}> wrapper = queryWrapper();
    wrapper.select(
    <#assign validColumns = []>
    <#list table.columns as column>
        <#if !column.logicField && !column.versionField>
            <#assign validColumns = validColumns + [column]>
        </#if>
    </#list>

    <#list validColumns as column>
        ${table.classPrefix}::get${column.propertyNameUp}<#if column_has_next>,</#if>
    </#list>
    );
    PageInfo<${table.classPrefix}> page = page(pageInfo(param), wrapper);
    return MybatisPage.convert(page, ${table.classPrefix}Convert.INSTANCE::toVO);
    }

}
$devkit$, 'TEXT', 3474, '55af573b397f56641d6cfd6f57588a5c', 0, 1),
    (2110000000000020053, 2110000000000010003, 'src/main/java/pojo/dto/[base][for]DTO.java.ftl', '[base][for]DTO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

<#list table.imports as import>
import ${import};
</#list>

/**
* <p>
    * ${table.comment} 数据传输对象，service和manager向外传输对象
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
public class ${table.classPrefix}DTO {
<#list table.columns as column>
    <#if !column.logicField>
        /** ${column.columnComment} */
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 595, 'b220193075db096e835e6cef994f0651', 0, 1),
    (2110000000000020054, 2110000000000010003, 'src/main/java/pojo/entity/[base][for].java.ftl', '[base][for].java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 数据源对象
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@Document(indexName = "${table.name}", createIndex = true, shards = 5, replicas = 1)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix} implements Serializable{
private static final long serialVersionUID = 1L;
<#list table.columns as column>
    /**
    * ${column.columnComment}
    */
    <#if column.primaryKey>
    @Id
    <#else>
    @Field(value = "${column.columnName}", type = FieldType.Auto)
    </#if>
    private ${column.javaType} ${column.propertyName};
</#list>

}
$devkit$, 'TEXT', 1193, 'de6f27c114f13c2a20af80572bd12ee7', 0, 1),
    (2110000000000020055, 2110000000000010003, 'src/main/java/pojo/param/[base][for]CreateParam.java.ftl', '[base][for]CreateParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
<#list table.imports as import>
    import ${import};
</#list>


/**
 * <p>
 * ${table.comment} 新增参数封装
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@Schema(description =  "${table.comment}新增参数")
public class ${table.classPrefix}CreateParam {
<#list table.columns as column>
    <#if !column.commonField && !column.primaryKey && !column.logicField  && !column.versionField>
        <#if !column.nullable>
            <#if column.javaType == 'String'>
                @NotBlank(message = "${column.columnComment}不能为空")
            <#else>
                @NotNull(message = "${column.columnComment}不能为空")
            </#if>
        </#if>
        @Schema(description = "${column.columnComment}")
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 1152, 'd5d246cf3c1e4c4f8e53b1e80d1101c2', 0, 1),
    (2110000000000020056, 2110000000000010003, 'src/main/java/pojo/param/[base][for]PageParam.java.ftl', '[base][for]PageParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.carlos.core.param.ParamPage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

<#list table.imports as import>
    import ${import};
</#list>


/**
 * <p>
 * ${table.comment} 列表查询参数封装
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description =  "${table.comment}列表查询参数")
public class ${table.classPrefix}PageParam extends ParamPage {
<#list table.columns as column>
    <#if !column.commonField && !column.primaryKey && !column.logicField  && !column.versionField>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
@Schema(description = "开始时间")
    private LocalDateTime start;

@Schema(description = "结束时间")
    private LocalDateTime end;
}
$devkit$, 'TEXT', 1183, '5a1bcd93b3a77bed12a77f27851272ac', 0, 1),
    (2110000000000020057, 2110000000000010003, 'src/main/java/pojo/param/[base][for]UpdateParam.java.ftl', '[base][for]UpdateParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} 更新参数封装
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
@Schema(description =  "${table.comment}修改参数")
public class ${table.classPrefix}UpdateParam {
<#list table.columns as column>
    <#if !column.commonField && !column.logicField  && !column.versionField>
        <#if column.primaryKey>
            <#if column.javaType == 'String'>
                @NotBlank(message = "${column.columnComment}不能为空")
            <#else>
                @NotNull(message = "${column.columnComment}不能为空")
            </#if>
        </#if>
        @Schema(description = "${column.columnComment}")
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 1130, '8ac8b8f32f8e9018a0d89140ebb8ff1c', 0, 1),
    (2110000000000020058, 2110000000000010003, 'src/main/java/pojo/vo/[base][for]VO.java.ftl', '[base][for]VO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} 显示层对象，向页面传输的对象
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix}VO implements Serializable {
private static final long serialVersionUID = 1L;
<#list table.columns as column>
    <#if !column.logicField && !column.versionField>
        @Schema(description = "${column.columnComment}")
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>

}
$devkit$, 'TEXT', 879, '3256bc0a24668f53d54cb46e0f128008', 0, 1),
    (2110000000000020059, 2110000000000010003, 'src/main/java/repository/[base][for]Repository.java.ftl', '[base][for]Repository.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.repository;


import ${project.packageName}.pojo.entity.${table.classPrefix};
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;


import java.util.Map;

/**
 * <p>
 * ${table.comment} 查询接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Repository
public interface ${table.classPrefix}Repository extends ElasticsearchRepository<${table.classPrefix}, String> {


}
$devkit$, 'TEXT', 526, 'ffa77f4fbe8ce885f505b0fc88aa0677', 0, 1),
    (2110000000000020060, 2110000000000010003, 'src/main/java/service/[base][for]Service.java.ftl', '[base][for]Service.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.service;

import ${project.packageName}.manager.${table.classPrefix}Manager;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.service.${table.classPrefix}Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
    * ${table.comment} 业务
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ${table.classPrefix}Service {

private final ${table.classPrefix}Manager ${table.classMainPrefix}Manager;

    /**
     * 新增${table.comment}
     *
     * @param dto ${table.comment}数据
     * @author  ${project.author}
     * @date    ${.now}
     */
public void add${table.classPrefix}(${table.classPrefix}DTO dto){
boolean success = ${table.classMainPrefix}Manager.add(dto);
if (!success) {
// 保存失败的应对措施
return;
}
Serializable id = dto.getId();
log.info("Insert '${table.classPrefix}' data: id:{}", id);
// 保存完成的后续业务
}

    /**
     * 删除${table.comment}
     *
     * @param ids ${table.comment}id
     * @author  ${project.author}
     * @date    ${.now}
     */
public void delete${table.classPrefix}(Set
<Serializable> ids){
    for (Serializable id : ids) {
    boolean success = ${table.classMainPrefix}Manager.delete(id);
    if (!success) {
    // 删除失败的措施
    continue;
    }
    // 删除成功的后续业务
    }
    }

    /**
     * 修改${table.comment}信息
     *
     * @param dto 对象信息
     * @author  ${project.author}
     * @date    ${.now}
     */
    public void update${table.classPrefix}(${table.classPrefix}DTO dto){
    boolean success = ${table.classMainPrefix}Manager.modify(dto);
    if (!success) {
    // 修改失败操作
    return;
    }
    // 修改成功的后续操作
    log.info("Update '${table.classPrefix}' data: id:{}", dto.getId());
    }

}
$devkit$, 'TEXT', 2119, '268fe5d3c5231ef916166f0bf2706f04', 0, 1),
    (2110000000000020061, 2110000000000010004, 'src/main/java/api/Api[base][for].java.ftl', 'Api[base][for].java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api;

import ${project.packageName}.ServiceNameConstant;
import ${project.packageName}.api.fallback.Api${table.classPrefix}FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;

/**
* <p>
    * ${table.comment} feign 提供接口
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "${table.classMainPrefix}", path = "/api/${table.apiPath}", fallbackFactory = Api${table.classPrefix}FallbackFactory.class)
public interface Api${table.classPrefix} {


}
$devkit$, 'TEXT', 601, '25329ca8881bb03672db9fae250bc1a1', 0, 1),
    (2110000000000020062, 2110000000000010004, 'src/main/java/api/fallback/Api[base][for]FallbackFactory.java.ftl', 'Api[base][for]FallbackFactory.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.fallback;

import ${project.packageName}.api.Api${table.classPrefix};
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
* <p>
    * ${table.comment} api 降级
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Slf4j
public class Api${table.classPrefix}FallbackFactory implements FallbackFactory
<Api${table.classPrefix}> {

    @Override
    public Api${table.classPrefix} create(Throwable throwable) {
    String message = throwable.getMessage();
    log.error("${table.comment}服务调用失败: message:{}", message);
    return new Api${table.classPrefix}() {

    };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public Api${table.classPrefix}FallbackFactory ${table.classMainPrefix}FallbackFactory() {
    // return new Api${table.classPrefix}FallbackFactory();
    // }
    }$devkit$, 'TEXT', 956, '01f2a85b4453f46cce2ce92e3f4b6ca6', 0, 1),
    (2110000000000020063, 2110000000000010004, 'src/main/java/api/pojo/ao/[base][for]AO.java.ftl', '[base][for]AO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.pojo.ao;


import lombok.Data;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} API提供的对象(API Object)
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
public class ${table.classPrefix}AO implements Serializable{
<#list table.columns as column>
    <#if !column.logicField>
        /** ${column.columnComment} */
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 563, '0088aa8580f8b4c039a0e4e1beeb7d4f', 0, 1),
    (2110000000000020064, 2110000000000010004, 'src/main/java/api/pojo/param/Api[base][for]Param.java.ftl', 'Api[base][for]Param.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.api.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} API提供的对象(API Object)
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
public class Api${table.classPrefix}Param implements Serializable{
<#list table.columns as column>
    <#if !column.logicField>
        /** ${column.columnComment} */
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 635, 'a44cf88cdda307e99769a11e099c7c1d', 0, 1),
    (2110000000000020065, 2110000000000010004, 'src/main/java/apiimpl/Api[base][for]Impl.java.ftl', 'Api[base][for]Impl.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.apiimpl;


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
$devkit$, 'TEXT', 648, '632e2aa5fb8049fbe451c70a5c4640c8', 0, 1),
    (2110000000000020066, 2110000000000010004, 'src/main/java/config/ApplicationExtendImpl.java.ftl', 'ApplicationExtendImpl.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName}.config;

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
$devkit$, 'TEXT', 839, '6bf03875db5c3d41ef234fe95601c1aa', 0, 1),
    (2110000000000020067, 2110000000000010004, 'src/main/java/controller/[base][for]Controller.java.ftl', '[base][for]Controller.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import ${project.packageName}.convert.${table.classPrefix}Convert;
import ${project.packageName}.pojo.param.${table.classPrefix}CreateParam;
import ${project.packageName}.pojo.param.${table.classPrefix}UpdateParam;
import ${project.packageName}.pojo.param.${table.classPrefix}PageParam;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.service.${table.classPrefix}Service;
import ${project.packageName}.manager.${table.classPrefix}Manager;
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

    private final ${table.classPrefix}Service ${table.classMainPrefix}Service;

    private final ${table.classPrefix}Manager ${table.classMainPrefix}Manager;


@PostMapping("add")
@Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated ${table.classPrefix}CreateParam param) {
        ${table.classPrefix}DTO dto = ${table.classPrefix}Convert.INSTANCE.toDTO(param);
        ${table.classMainPrefix}Service.add${table.classPrefix}(dto);
    }


@PostMapping("delete")
@Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody ParamIdSet<Serializable> param) {
        ${table.classMainPrefix}Service.delete${table.classPrefix}(param.getIds());
    }


    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated ${table.classPrefix}UpdateParam param) {
        ${table.classPrefix}DTO dto = ${table.classPrefix}Convert.INSTANCE.toDTO(param);
        ${table.classMainPrefix}Service.update${table.classPrefix}(dto);
    }


    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public ${table.classPrefix}VO detail(Long id) {
        return ${table.classPrefix}Convert.INSTANCE.toVO(${table.classMainPrefix}Manager.getDtoById(id));
    }


    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<${table.classPrefix}VO> page(${table.classPrefix}PageParam param) {
        return ${table.classMainPrefix}Manager.getPage(param);
    }
}
$devkit$, 'TEXT', 2792, 'd710e4e18277b53f7eb1df41e30acbbe', 0, 1),
    (2110000000000020068, 2110000000000010004, 'src/main/java/convert/CommonConvert.java.ftl', 'CommonConvert.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName}.convert;

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
$devkit$, 'TEXT', 779, '3016180f00c2d63fedd3efc3417f07c0', 0, 1),
    (2110000000000020069, 2110000000000010004, 'src/main/java/convert/[base][for]Convert.java.ftl', '[base][for]Convert.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.convert;

import ${project.packageName}.pojo.param.${table.classPrefix}CreateParam;
import ${project.packageName}.pojo.param.${table.classPrefix}UpdateParam;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.convert.CommonConvert;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

/**
 * <p>
 * ${table.comment} 转换器
 * </p>
 *
 * @author ${project.author}
 * @date ${.now}
 */
@Mapper(uses = {CommonConvert.class})
public interface ${table.classPrefix}Convert {

    ${table.classPrefix}Convert INSTANCE = Mappers.getMapper(${table.classPrefix}Convert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}DTO toDTO(${table.classPrefix}CreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}DTO toDTO(${table.classPrefix}UpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author ${project.author}
     * @date ${.now}
     */
    List<${table.classPrefix}DTO> toDTO(List<${table.classPrefix}> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}DTO toDTO(${table.classPrefix} entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix} toDO(${table.classPrefix}DTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author ${project.author}
     * @date ${.now}
     */
    ${table.classPrefix}VO toVO(${table.classPrefix}DTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author ${project.author}
     * @date ${.now}
     */
    List<${table.classPrefix}VO> toVO(List<${table.classPrefix}> dos);

        /**
        * 数据持久对象转数据显示对象
        *
        * @param entity 数据传输对象
        * @return 数据显示对象
        * @author ${project.author}
        * @date ${.now}
        */
        ${table.classPrefix}VO toVO(${table.classPrefix} entity);
}
$devkit$, 'TEXT', 3098, '010a85688acef19697efc35f26040a9a', 0, 1),
    (2110000000000020070, 2110000000000010004, 'src/main/java/manager/[base][for]Manager.java.ftl', '[base][for]Manager.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.pojo.param.${table.classPrefix}PageParam;

import java.io.Serializable;
/**
 * <p>
 * ${table.comment} 查询封装接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
public interface ${table.classPrefix}Manager extends BaseService<${table.classPrefix}>{

    /**
     * 新增${table.comment}
     *
     * @param dto ${table.comment}数据
     * @return boolean
     * @author  ${project.author}
     * @date    ${.now}
     */
    boolean add(${table.classPrefix}DTO dto);

    /**
     * 删除${table.comment}
     *
     * @param id ${table.comment}id
     * @return boolean
     * @author  ${project.author}
     * @date    ${.now}
     */
    boolean delete(Serializable id);

    /**
     * 修改${table.comment}信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author  ${project.author}
     * @date    ${.now}
     */
    boolean modify(${table.classPrefix}DTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
* @return ${project.packageName}.pojo.dto.${table.classPrefix}DTO
     * @author ${project.author}
     * @date   ${.now}
     */
${table.classPrefix}DTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author ${project.author}
     * @date   ${.now}
     */
    Paging<${table.classPrefix}VO> getPage(${table.classPrefix}PageParam param);
}
$devkit$, 'TEXT', 1813, 'a3672a7422ed38945a844c954b6e36c3', 0, 1),
    (2110000000000020071, 2110000000000010004, 'src/main/java/manager/impl/[base][for]ManagerImpl.java.ftl', '[base][for]ManagerImpl.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import ${project.packageName}.convert.${table.classPrefix}Convert;
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import ${project.packageName}.manager.${table.classPrefix}Manager;
import ${project.packageName}.mapper.${table.classPrefix}Mapper;
import ${project.packageName}.pojo.param.${table.classPrefix}PageParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
/**
 * <p>
 * ${table.comment} 查询封装实现类
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ${table.classPrefix}ManagerImpl  extends BaseServiceImpl
<${table.classPrefix}Mapper, ${table.classPrefix}> implements ${table.classPrefix}Manager {

    @Override
    public boolean add(${table.classPrefix}DTO dto) {
        ${table.classPrefix} entity = ${table.classPrefix}Convert.INSTANCE.toDO(dto);
boolean success = save(entity);
if (!success) {
log.warn("Insert '${table.classPrefix}' data fail, entity:{}", entity);
return false;
}
        dto.setId(entity.getId());
// 保存完成的后续
        log.debug("Insert '${table.classPrefix}' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
boolean success = removeById(id);
if (!success) {
log.warn("Remove '${table.classPrefix}' data fail, id:{}", id);
return false;
}
        log.debug("Remove '${table.classPrefix}' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(${table.classPrefix}DTO dto) {
        ${table.classPrefix} entity = ${table.classPrefix}Convert.INSTANCE.toDO(dto);
boolean success = updateById(entity);
if (!success) {
log.warn("Update '${table.classPrefix}' data fail, entity:{}", entity);
return false;
}
// 修改成功的后续操作
log.debug("Update '${table.classPrefix}' data by id:{}", dto.getId());
        return true;
    }

    @Override
public ${table.classPrefix}DTO getDtoById(Serializable id) {
if (id == null) {
log.warn("id is null");
return null;
}
${table.classPrefix} entity = getBaseMapper().selectById(id);
        return ${table.classPrefix}Convert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<${table.classPrefix}VO> getPage(${table.classPrefix}PageParam param){
    LambdaQueryWrapper<${table.classPrefix}> wrapper = queryWrapper();
    wrapper.select(
    <#assign validColumns = []>
    <#list table.columns as column>
        <#if !column.logicField && !column.versionField>
            <#assign validColumns = validColumns + [column]>
        </#if>
    </#list>

    <#list validColumns as column>
        ${table.classPrefix}::get${column.propertyNameUp}<#if column_has_next>,</#if>
    </#list>
    );
    PageInfo<${table.classPrefix}> page = page(pageInfo(param), wrapper);
    return MybatisPage.convert(page, ${table.classPrefix}Convert.INSTANCE::toVO);
    }

}
$devkit$, 'TEXT', 3620, '7cb2a3afa6f40d1af3bf4c1142bd2e57', 0, 1),
    (2110000000000020072, 2110000000000010004, 'src/main/java/pojo/dto/[base][for]DTO.java.ftl', '[base][for]DTO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

<#list table.imports as import>
import ${import};
</#list>

/**
* <p>
    * ${table.comment} 数据传输对象，service和manager向外传输对象
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
public class ${table.classPrefix}DTO {
<#list table.columns as column>
    <#if !column.logicField>
        /** ${column.columnComment} */
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 595, 'b220193075db096e835e6cef994f0651', 0, 1),
    (2110000000000020073, 2110000000000010004, 'src/main/java/pojo/entity/[base][for].java.ftl', '[base][for].java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} 数据源对象
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
@Document(collection = "${table.name}")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix} implements Serializable{
private static final long serialVersionUID = 1L;
<#list table.columns as column>
    /**
    * ${column.columnComment}
    */
    <#if column.primaryKey>
        @Id
    <#else>
        @Field(value = "${column.columnName}")
    </#if>
    private ${column.javaType} ${column.propertyName};
</#list>

}
$devkit$, 'TEXT', 1054, '5ec0362cd7aa9cf99176d51c2620fade', 0, 1),
    (2110000000000020074, 2110000000000010004, 'src/main/java/pojo/param/[base][for]CreateParam.java.ftl', '[base][for]CreateParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
<#list table.imports as import>
    import ${import};
</#list>


/**
 * <p>
 * ${table.comment} 新增参数封装
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@Schema(description =  "${table.comment}新增参数")
public class ${table.classPrefix}CreateParam {
<#list table.columns as column>
    <#if !column.commonField && !column.primaryKey && !column.logicField  && !column.versionField>
        <#if !column.nullable>
            <#if column.javaType == 'String'>
                @NotBlank(message = "${column.columnComment}不能为空")
            <#else>
                @NotNull(message = "${column.columnComment}不能为空")
            </#if>
        </#if>
        @Schema(description = "${column.columnComment}")
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 1152, 'd5d246cf3c1e4c4f8e53b1e80d1101c2', 0, 1),
    (2110000000000020075, 2110000000000010004, 'src/main/java/pojo/param/[base][for]PageParam.java.ftl', '[base][for]PageParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.carlos.core.param.ParamPage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

<#list table.imports as import>
    import ${import};
</#list>


/**
 * <p>
 * ${table.comment} 列表查询参数封装
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description =  "${table.comment}列表查询参数")
public class ${table.classPrefix}PageParam extends ParamPage {
<#list table.columns as column>
    <#if !column.commonField && !column.primaryKey && !column.logicField  && !column.versionField>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
@Schema(description = "开始时间")
    private LocalDateTime start;

@Schema(description = "结束时间")
    private LocalDateTime end;
}
$devkit$, 'TEXT', 1183, '5a1bcd93b3a77bed12a77f27851272ac', 0, 1),
    (2110000000000020076, 2110000000000010004, 'src/main/java/pojo/param/[base][for]UpdateParam.java.ftl', '[base][for]UpdateParam.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} 更新参数封装
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
@Schema(description =  "${table.comment}修改参数")
public class ${table.classPrefix}UpdateParam {
<#list table.columns as column>
    <#if !column.commonField && !column.logicField  && !column.versionField>
        <#if column.primaryKey>
            <#if column.javaType == 'String'>
                @NotBlank(message = "${column.columnComment}不能为空")
            <#else>
                @NotNull(message = "${column.columnComment}不能为空")
            </#if>
        </#if>
        @Schema(description = "${column.columnComment}")
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
$devkit$, 'TEXT', 1130, '8ac8b8f32f8e9018a0d89140ebb8ff1c', 0, 1),
    (2110000000000020077, 2110000000000010004, 'src/main/java/pojo/vo/[base][for]VO.java.ftl', '[base][for]VO.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} 显示层对象，向页面传输的对象
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix}VO implements Serializable {
private static final long serialVersionUID = 1L;
<#list table.columns as column>
    <#if !column.logicField && !column.versionField>
        @Schema(description = "${column.columnComment}")
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>

}
$devkit$, 'TEXT', 879, '3256bc0a24668f53d54cb46e0f128008', 0, 1),
    (2110000000000020078, 2110000000000010004, 'src/main/java/repository/[base][for]Repository.java.ftl', '[base][for]Repository.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.repository;


import ${project.packageName}.pojo.entity.${table.classPrefix};
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


import java.util.Map;

/**
 * <p>
 * ${table.comment} 查询接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Repository
public interface ${table.classPrefix}Repository extends MongoRepository<${table.classPrefix}, String> {


}
$devkit$, 'TEXT', 504, '012c81e273f2481cfb291373f1c7c649', 0, 1),
    (2110000000000020079, 2110000000000010004, 'src/main/java/service/[base][for]Service.java.ftl', '[base][for]Service.java', 'ftl', 1, TRUE, $devkit$package ${project.packageName}.service;

import ${project.packageName}.manager.${table.classPrefix}Manager;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.service.${table.classPrefix}Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
    * ${table.comment} 业务
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ${table.classPrefix}Service {

private final ${table.classPrefix}Manager ${table.classMainPrefix}Manager;

    /**
     * 新增${table.comment}
     *
     * @param dto ${table.comment}数据
     * @author  ${project.author}
     * @date    ${.now}
     */
public void add${table.classPrefix}(${table.classPrefix}DTO dto){
boolean success = ${table.classMainPrefix}Manager.add(dto);
if (!success) {
// 保存失败的应对措施
return;
}
Serializable id = dto.getId();
log.info("Insert '${table.classPrefix}' data: id:{}", id);
// 保存完成的后续业务
}

    /**
     * 删除${table.comment}
     *
     * @param ids ${table.comment}id
     * @author  ${project.author}
     * @date    ${.now}
     */
public void delete${table.classPrefix}(Set
<Serializable> ids){
    for (Serializable id : ids) {
    boolean success = ${table.classMainPrefix}Manager.delete(id);
    if (!success) {
    // 删除失败的措施
    continue;
    }
    // 删除成功的后续业务
    }
    }

    /**
     * 修改${table.comment}信息
     *
     * @param dto 对象信息
     * @author  ${project.author}
     * @date    ${.now}
     */
    public void update${table.classPrefix}(${table.classPrefix}DTO dto){
    boolean success = ${table.classMainPrefix}Manager.modify(dto);
    if (!success) {
    // 修改失败操作
    return;
    }
    // 修改成功的后续操作
    log.info("Update '${table.classPrefix}' data: id:{}", dto.getId());
    }

}
$devkit$, 'TEXT', 2119, '268fe5d3c5231ef916166f0bf2706f04', 0, 1),
    (2110000000000020080, 2110000000000010005, '[ArtifactId]/.gitignore.ftl', '.gitignore', 'ftl', 1, FALSE, $devkit$######################################################################
# Build Tools

.gradle
/build/
!gradle/wrapper/gradle-wrapper.jar

target/
!.mvn/wrapper/maven-wrapper.jar

######################################################################
# IDE

### STS ###
.apt_generated
.classpath
.class
.flattened-pom.xml
.project
.settings
.springBeans

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### JRebel ###
rebel.xml
### NetBeans ###
nbproject/private/
build/*
nbbuild/
dist/
nbdist/
.nb-gradle/

######################################################################
# Others
*.log
*.xml.versionsBackup
*.swp

!*/build/*.java
!*/build/*.html
!*/build/*.xml$devkit$, 'TEXT', 712, '9370fdd7376a4f12f5ec29c81178d8ae', 0, 1),
    (2110000000000020081, 2110000000000010005, '[ArtifactId]/Dockerfile.ftl', 'Dockerfile', 'ftl', 1, FALSE, $devkit$FROM openjdk:8-jdk-alpine3.9

ENV LANG en_US.UTF-8

# 设置镜像源
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories

# 解决时差8小时问题
RUN apk add tzdata
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# 安装字体
RUN apk add --update ttf-dejavu fontconfig && rm -rf /var/cache/apk/*
#安装simsun
RUN wget -O /usr/share/fonts/simsun.ttf https://pfh-file-store.oss-cn-hangzhou.aliyuncs.com/simsun.ttf
RUN fc-cache -vf

RUN mkdir -p /jar

WORKDIR /jar

# 默认端口8080
ENV PORT=8080

EXPOSE ${r'${PORT}'}

ENV JAVA_OPTS "-Xmx1024m -Xms256m"

ENV PROJECT_NAME="${r'${project.artifactId}'}"

ADD ./${r'${PROJECT_NAME}'}-cloud/target/${r'${PROJECT_NAME}'}.jar ./${r'${PROJECT_NAME}'}.jar

CMD java -server ${r'${JAVA_OPTS}'} -Dserver.port=${r'${PORT}'} -jar ${r'${PROJECT_NAME}'}.jar
$devkit$, 'TEXT', 926, '33ef092367812a775ae17ff14c7ca287', 0, 1),
    (2110000000000020082, 2110000000000010005, '[ArtifactId]/deploy/deploy.yaml.ftl', 'deploy.yaml', 'ftl', 1, FALSE, $devkit$kind: Deployment
apiVersion: apps/v1
metadata:
  name: #APP_NAME
  namespace: #NAMESPACE
  labels:
    app: #APP_NAME
spec:
  replicas: 1
  selector:
    matchLabels:
      app: #APP_NAME
  template:
    metadata:
      labels:
        app: #APP_NAME
    spec:
      automountServiceAccountToken: false
      containers:
        -
          name: #APP_NAME
          image: #APP_IMAGE_NAME
          ports:
            -
              containerPort: 8080
              protocol: TCP
          resources:
            requests:
              cpu: "500m"
              memory: "500m"
            limits:
              cpu: "500m"
              memory: "1Gi"
          env: #ENV_LIST
            -
              name: TZ
              value: Asia/Shanghai
            -
              name: PROFILE
              value: deploy






$devkit$, 'TEXT', 874, '463f346a995d9f9226ab6ad2abfc84df', 0, 1),
    (2110000000000020083, 2110000000000010005, '[ArtifactId]/pom.xml.ftl', 'pom.xml', 'ftl', 1, FALSE, $devkit$<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.carlos</groupId>
        <artifactId>carlos-parent</artifactId>
        <version>3.0.0-SNAPSHOT</version>
    </parent>
    <packaging>pom</packaging>

    <version>${project.version}</version>
    <groupId>${project.groupId}</groupId>
    <artifactId>${project.artifactId}</artifactId>
    <name>${project.projectName}</name>
    <description>${project.describe}</description>

    <modelVersion>4.0.0</modelVersion>

    <properties>

    </properties>

    <dependencies>
        <dependency>
            <groupId>com.carlos</groupId>
            <artifactId>carlos-spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>


    <build>
        <finalName>${r'${project.parent.artifactId}'}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
$devkit$, 'TEXT', 1664, '6ba146852cd850b70459a9f2824d8a9a', 0, 1),
    (2110000000000020084, 2110000000000010005, '[ArtifactId]/src/main/java/[Package]/[CamelName]Application.java.ftl', '[CamelName]Application.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
* <p>
    * Spring Boot 启动类
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@SpringBootApplication
public class ${project.camelName}Application {

public static void main(String[] args) {
SpringApplication.run(${project.camelName}BootApplication.class, args);
}
}
$devkit$, 'TEXT', 464, '1eb24627a50061231fa377b539992332', 0, 1),
    (2110000000000020085, 2110000000000010005, '[ArtifactId]/src/main/java/[Package]/config/[CamelName]Config.java.ftl', '[CamelName]Config.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName}.config;

import org.springframework.context.annotation.Configuration;

/**
* 配置类
*
* @author carlos
*/
@Configuration
public class ${project.camelName}Config {


}
$devkit$, 'TEXT', 215, '6335580cfee3b53249a951f73d92610d', 0, 1),
    (2110000000000020086, 2110000000000010005, '[ArtifactId]/src/main/java/[Package]/exceptions/[CamelName]Exception.java.ftl', '[CamelName]Exception.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName}.exceptions;

import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
* <p>
    * 服务主异常
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
public class ${project.camelName}Exception extends ComponentException {

public ${project.camelName}Exception() {
super("${project.camelName}  exception!");
}

public ${project.camelName}Exception(String message) {
super(message);
}

public ${project.camelName}Exception(ErrorCode errorCode, String message) {
super(errorCode, message);
}

public ${project.camelName}Exception(StatusCode errorCode) {
super(errorCode);
}

public ${project.camelName}Exception(Throwable cause) {
super(cause);
}

public ${project.camelName}Exception(String message, Throwable cause) {
super(message, cause);
}

}
$devkit$, 'TEXT', 872, 'e237bf2bf7dbf953b7c1a753354544fc', 0, 1),
    (2110000000000020087, 2110000000000010005, '[ArtifactId]/src/main/resources/[UnderLineName].sql', '[UnderLineName].sql', 'sql', 2, FALSE, $devkit$-- ----------------------------
-- Table structure for test
-- ----------------------------
DROP TABLE IF EXISTS test;
CREATE TABLE test
(
    id          varchar(32) NOT NULL COMMENT '主键ID',
    test_1      varchar(32) NOT NULL COMMENT '模板字段1',
    test_2      varchar(32) NOT NULL COMMENT '模板字段2',
    test_3      varchar(32) NOT NULL COMMENT '模板字段3',
    test_4      varchar(32) NOT NULL COMMENT '模板字段4',
    is_deleted  tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除，0：未删除，1：已删除',
    create_by   char(32) DEFAULT NULL COMMENT '创建者编号',
    create_time datetime DEFAULT NULL COMMENT '创建时间',
    update_by   char(32) DEFAULT NULL COMMENT '更新者编号',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '模板表名'
  ROW_FORMAT = DYNAMIC;


$devkit$, 'TEXT', 976, 'd740838734de6822475bbe0a9ca10e51', 0, 1),
    (2110000000000020088, 2110000000000010005, '[ArtifactId]/src/main/resources/application-boot.yml.ftl', 'application-boot.yml', 'ftl', 1, FALSE, $devkit$$devkit$, 'TEXT', 0, 'd41d8cd98f00b204e9800998ecf8427e', 0, 1),
    (2110000000000020089, 2110000000000010005, '[ArtifactId]/src/main/resources/application-env.yml.ftl', 'application-env.yml', 'ftl', 1, FALSE, $devkit$## 数据库配置
MYSQL_URL: '100.127.6.234:13001/carlos_bbt'
MYSQL_USERNAME: 'root'
MYSQL_PWD: 'carlos@123456'


MYSQL_URL_SLAVE: '100.127.6.234:13306/carlos_bbt_dy'
MYSQL_USERNAME_SLAVE: 'root'
MYSQL_PWD_SLAVE: 'carlos@123456'

#minio配置
MINIO_HOST: 'http://100.127.6.234:19000'
MINIO_PUBLIC_HOST: 'http://100.127.6.234:19000'
MINIO_USERNAME: 'carlos'
MINIO_PWD: 'carlos@123456'

#redis配置
REDIS_IP: '100.127.6.234'
REDIS_PORT: '16379'
REDIS_DB: '1'
REDIS_PWD: 'carlos@123456'
$devkit$, 'TEXT', 507, '314a099ebb1fa7928011cd6898a38606', 0, 1),
    (2110000000000020090, 2110000000000010005, '[ArtifactId]/src/main/resources/application.yml.ftl', 'application.yml', 'ftl', 1, FALSE, $devkit$server:
port: 8080
compression:
enabled: true
mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
min-response-size: 128KB
tomcat:
threads:
max: 32
servlet:
context-path: /
spring:
profiles:
active: dm,bootdm
servlet:
multipart:
enabled: true
max-file-size: 200MB # 单文件最大限制
max-request-size: 200MB # 一次上传所有文件的最大限制. 如果接口只支持单文件,则该值与上面相同即可
application:
name: carlos-test
# spring配置
datasource:
druid:
stat-view-servlet:
enabled: true
loginUsername: admin
loginPassword: 123456
dynamic:
primary: master
druid:
break-after-acquire-failure: false
fail-fast: true
connection-error-retry-attempts: 10
initial-size: 5
min-idle: 5
max-active: 600
max-wait: 60000
time-between-eviction-runs-millis: 60000
min-evictable-idle-time-millis: 300000
validation-query: SELECT 1
test-while-idle: true
test-on-borrow: false
test-on-return: false
pool-prepared-statements: true
max-pool-prepared-statement-per-connection-size: 20
#                filters: stat,slf4j
connection-properties:
druid.stat.mergeSql: false
druid.stat.slowSqlMillis: 5000
filter:
stat:
merge-sql: false
wall:
enabled: false

############################## application start ##############################
carlos:
boot:
cors:
enable: true
enums:
scan-package: com.carlos.test
enabled: true
# 自定义项目信息
info:
project-name: carlos-test
groupId: com.carlos
author: Carlos
description:
sourceEncoding: utf-8
version: 1.0.0-SNAPSHOT
domain: www.zcarlos.com

############################ application end ###############################

management:
endpoints:
web:
exposure:
include: "*"


##############################  日志配置 start  ###########################
logging:
config: classpath:config/logback.xml
level:
root: info
com.carlos: debug
file:
path: logs/carlos-test/
##############################  日志配置 end   ###########################


knife4j: # 是否开启Knife4j增强模式 默认 false
enable: true
# 是否是生产环境
production: false
# knife4j配置
cors: false
# 个性化配置 详情配置参照源码
setting: # 是否开启Debug调试
enableDebug: true
language: zh-CN
enableSwaggerModels: true
enableDocumentManage: true
swaggerModelName: 实体类列表
enableVersion: true
enableReloadCacheParameter: false
enableAfterScript: true
enableFilterMultipartApiMethodType: POST
enableFilterMultipartApis: false
enableRequestCache: true
enableHost: false
enableHostText: 192.168.0.193:8000
# 是否开启自定义主页内容
enableHomeCustom: false
# 主页内容Markdown文件路径
homeCustomLocation: classpath:markdown/home.md
# 开启搜索
enableSearch: true
# 显示页底信息
enableFooter: false
enableFooterCustom: true
footerCustomContent: 云津智慧科技有限公司
# 动态参数调试
enableDynamicParameter: true
# 显示OpenAPI规范
enableOpenApi: true
enableGroup: true
# 是否开启BasicHttp验证
basic:
enable: false
username: carlos
password: hadoop543216
#################################### 接口文档 Swagger end ###################################

$devkit$, 'TEXT', 3124, '6a5ea51d35f97ba679ca1bee3a50f2e5', 0, 1),
    (2110000000000020091, 2110000000000010006, '[ArtifactId]/.gitignore.ftl', '.gitignore', 'ftl', 1, FALSE, $devkit$######################################################################
# Build Tools

.gradle
/build/
!gradle/wrapper/gradle-wrapper.jar

target/
!.mvn/wrapper/maven-wrapper.jar

######################################################################
# IDE

### STS ###
.apt_generated
.classpath
.class
.flattened-pom.xml
.project
.settings
.springBeans

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### JRebel ###
rebel.xml
### NetBeans ###
nbproject/private/
build/*
nbbuild/
dist/
nbdist/
.nb-gradle/

######################################################################
# Others
*.log
*.xml.versionsBackup
*.swp

!*/build/*.java
!*/build/*.html
!*/build/*.xml$devkit$, 'TEXT', 712, '9370fdd7376a4f12f5ec29c81178d8ae', 0, 1),
    (2110000000000020092, 2110000000000010006, '[ArtifactId]/Dockerfile.ftl', 'Dockerfile', 'ftl', 1, FALSE, $devkit$FROM openjdk:8-jdk-alpine3.9

ENV LANG en_US.UTF-8

# 设置镜像源
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories

# 解决时差8小时问题
RUN apk add tzdata
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# 安装字体
RUN apk add --update ttf-dejavu fontconfig && rm -rf /var/cache/apk/*
#安装simsun
RUN wget -O /usr/share/fonts/simsun.ttf https://pfh-file-store.oss-cn-hangzhou.aliyuncs.com/simsun.ttf
RUN fc-cache -vf

RUN mkdir -p /jar

WORKDIR /jar

# 默认端口8080
ENV PORT=8080

EXPOSE ${r'${PORT}'}

ENV JAVA_OPTS "-Xmx1024m -Xms256m"

ENV PROJECT_NAME="${r'${project.artifactId}'}"

ADD ./${r'${PROJECT_NAME}'}-cloud/target/${r'${PROJECT_NAME}'}.jar ./${r'${PROJECT_NAME}'}.jar

CMD java -server ${r'${JAVA_OPTS}'} -Dserver.port=${r'${PORT}'} -jar ${r'${PROJECT_NAME}'}.jar
$devkit$, 'TEXT', 926, '33ef092367812a775ae17ff14c7ca287', 0, 1),
    (2110000000000020093, 2110000000000010006, '[ArtifactId]/[ArtifactId]-api/pom.xml.ftl', 'pom.xml', 'ftl', 1, FALSE, $devkit$<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>${project.groupId}</groupId>
        <artifactId>${project.artifactId}</artifactId>
        <version>${r'${revision}'}</version>
    </parent>

    <artifactId>${project.artifactId}-api</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.carlos</groupId>
            <artifactId>carlos-spring-boot-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-openfeign-core</artifactId>
        </dependency>
    </dependencies>
</project>
$devkit$, 'TEXT', 916, '5632b250e222839a6e8b40486cbab062', 0, 1),
    (2110000000000020094, 2110000000000010006, '[ArtifactId]/[ArtifactId]-api/src/main/java/[Package]/ServiceNameConstant.java.ftl', 'ServiceNameConstant.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName};

/**
 * <p>
 * 服务名常量
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
public interface ServiceNameConstant {

    /**
     * 服务名称
     */
     String SERVICE_NAME = "${project.artifactId}";

}
$devkit$, 'TEXT', 277, 'c9f92dec0ad344dbd61ad2ab418d57bf', 0, 1),
    (2110000000000020095, 2110000000000010006, '[ArtifactId]/[ArtifactId]-api/src/main/java/[Package]/api/enums/[CamelName]ErrorCode.java.ftl', '[CamelName]ErrorCode.java', 'ftl', 1, FALSE, $devkit$package com.carlos.audit.api.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ${project.describe}模块错误码
 * <p>
 * ${project.describe}编码：xx（${project.describe}服务）
 * <p>
 * 错误码格式：A-BB-CC
 * <ul>
 *   <li>A - 错误级别：1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：xx服务</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 *
 * @author  ${project.author}
 * @date    ${.now}
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ${project.camelName}ErrorCode implements ErrorCode {

    // ==================== 业务错误 (2-xx-xx) ====================


    // ==================== 第三方错误 (3-xx-xx) ====================

    // ==================== 系统错误 (5-xx-xx) ====================

    ;

    private final String code;
    private final String message;
    private final int httpStatus;

    /**
     * 创建业务异常（使用默认消息）
     *
     * @return BusinessException
     */
    @Override
    public BusinessException exception() {
        return new BusinessException(this);
    }

    /**
     * 创建业务异常（自定义消息）
     *
     * @param customMessage 自定义消息
     * @return BusinessException
     */
    @Override
    public BusinessException exception(String customMessage) {
        return new BusinessException(this, customMessage);
    }

    /**
     * 创建业务异常（格式化消息）
     *
     * @param format 格式字符串
     * @param args   参数
     * @return BusinessException
     */
    @Override
    public BusinessException exception(String format, Object... args) {
        return new BusinessException(this, String.format(format, args));
    }
}
$devkit$, 'TEXT', 1957, '43645abc1653b8d20e9445df930444ab', 0, 1),
    (2110000000000020096, 2110000000000010006, '[ArtifactId]/[ArtifactId]-boot/pom.xml.ftl', 'pom.xml', 'ftl', 1, FALSE, $devkit$<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>${project.groupId}</groupId>
        <artifactId>${project.artifactId}</artifactId>
        <version>${r'${revision}'}</version>
    </parent>

    <artifactId>${project.artifactId}-boot</artifactId>


    <dependencies>
        <dependency>
            <groupId>${project.groupId}</groupId>
            <artifactId>${project.artifactId}-bus</artifactId>
        </dependency>
        <dependency>
            <groupId>com.carlos</groupId>
            <artifactId>carlos-spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <build>
        <finalName>${r'${project.parent.artifactId}'}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>$devkit$, 'TEXT', 1642, '072442a22492e0dbfaa1ccd7b2991817', 0, 1),
    (2110000000000020097, 2110000000000010006, '[ArtifactId]/[ArtifactId]-boot/src/main/java/[Package]/[CamelName]BootApplication.java.ftl', '[CamelName]BootApplication.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>
 * 启动类
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@SpringBootApplication
public class ${project.camelName}BootApplication {

    public static void main(String[] args) {
        SpringApplication.run(${project.camelName}BootApplication.class, args);
    }
}
$devkit$, 'TEXT', 471, 'a004849626f8785747a869dd7f7cd88b', 0, 1),
    (2110000000000020098, 2110000000000010006, '[ArtifactId]/[ArtifactId]-boot/src/main/resources/application-boot.yml.ftl', 'application-boot.yml', 'ftl', 1, FALSE, $devkit$$devkit$, 'TEXT', 0, 'd41d8cd98f00b204e9800998ecf8427e', 0, 1),
    (2110000000000020099, 2110000000000010006, '[ArtifactId]/[ArtifactId]-boot/src/main/resources/application-env.yml.ftl', 'application-env.yml', 'ftl', 1, FALSE, $devkit$## 数据库配置
MYSQL_URL: '100.127.6.234:13001/carlos_bbt'
MYSQL_USERNAME: 'root'
MYSQL_PWD: 'carlos@123456'


MYSQL_URL_SLAVE: '100.127.6.234:13306/carlos_bbt_dy'
MYSQL_USERNAME_SLAVE: 'root'
MYSQL_PWD_SLAVE: 'carlos@123456'

#minio配置
MINIO_HOST: 'http://100.127.6.234:19000'
MINIO_PUBLIC_HOST: 'http://100.127.6.234:19000'
MINIO_USERNAME: 'carlos'
MINIO_PWD: 'carlos@123456'

#redis配置
REDIS_IP: '100.127.6.234'
REDIS_PORT: '16379'
REDIS_DB: '1'
REDIS_PWD: 'carlos@123456'
$devkit$, 'TEXT', 507, '314a099ebb1fa7928011cd6898a38606', 0, 1),
    (2110000000000020100, 2110000000000010006, '[ArtifactId]/[ArtifactId]-boot/src/main/resources/application.yml.ftl', 'application.yml', 'ftl', 1, FALSE, $devkit$server:
port: 8080
compression:
enabled: true
mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
min-response-size: 128KB
tomcat:
threads:
max: 32
servlet:
context-path: /
spring:
profiles:
active: dm,bootdm
servlet:
multipart:
enabled: true
max-file-size: 200MB # 单文件最大限制
max-request-size: 200MB # 一次上传所有文件的最大限制. 如果接口只支持单文件,则该值与上面相同即可
application:
name: carlos-test
# spring配置
datasource:
druid:
stat-view-servlet:
enabled: true
loginUsername: admin
loginPassword: 123456
dynamic:
primary: master
druid:
break-after-acquire-failure: false
fail-fast: true
connection-error-retry-attempts: 10
initial-size: 5
min-idle: 5
max-active: 600
max-wait: 60000
time-between-eviction-runs-millis: 60000
min-evictable-idle-time-millis: 300000
validation-query: SELECT 1
test-while-idle: true
test-on-borrow: false
test-on-return: false
pool-prepared-statements: true
max-pool-prepared-statement-per-connection-size: 20
#                filters: stat,slf4j
connection-properties:
druid.stat.mergeSql: false
druid.stat.slowSqlMillis: 5000
filter:
stat:
merge-sql: false
wall:
enabled: false

############################## application start ##############################
carlos:
boot:
cors:
enable: true
enums:
scan-package: com.carlos.test
enabled: true
# 自定义项目信息
info:
project-name: carlos-test
groupId: com.carlos
author: Carlos
description:
sourceEncoding: utf-8
version: 1.0.0-SNAPSHOT
domain: www.zcarlos.com

############################ application end ###############################

management:
endpoints:
web:
exposure:
include: "*"


##############################  日志配置 start  ###########################
logging:
config: classpath:config/logback.xml
level:
root: info
com.carlos: debug
file:
path: logs/carlos-test/
##############################  日志配置 end   ###########################


knife4j: # 是否开启Knife4j增强模式 默认 false
enable: true
# 是否是生产环境
production: false
# knife4j配置
cors: false
# 个性化配置 详情配置参照源码
setting: # 是否开启Debug调试
enableDebug: true
language: zh-CN
enableSwaggerModels: true
enableDocumentManage: true
swaggerModelName: 实体类列表
enableVersion: true
enableReloadCacheParameter: false
enableAfterScript: true
enableFilterMultipartApiMethodType: POST
enableFilterMultipartApis: false
enableRequestCache: true
enableHost: false
enableHostText: 192.168.0.193:8000
# 是否开启自定义主页内容
enableHomeCustom: false
# 主页内容Markdown文件路径
homeCustomLocation: classpath:markdown/home.md
# 开启搜索
enableSearch: true
# 显示页底信息
enableFooter: false
enableFooterCustom: true
footerCustomContent: 云津智慧科技有限公司
# 动态参数调试
enableDynamicParameter: true
# 显示OpenAPI规范
enableOpenApi: true
enableGroup: true
# 是否开启BasicHttp验证
basic:
enable: false
username: carlos
password: hadoop543216
#################################### 接口文档 Swagger end ###################################

$devkit$, 'TEXT', 3124, '6a5ea51d35f97ba679ca1bee3a50f2e5', 0, 1),
    (2110000000000020101, 2110000000000010006, '[ArtifactId]/[ArtifactId]-bus/pom.xml.ftl', 'pom.xml', 'ftl', 1, FALSE, $devkit$<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>${project.groupId}</groupId>
        <artifactId>${project.artifactId}</artifactId>
        <version>${r'${revision}'}</version>
    </parent>

    <packaging>jar</packaging>

    <artifactId>${project.artifactId}-bus</artifactId>


    <dependencies>

        <dependency>
            <groupId>com.carlos</groupId>
            <artifactId>carlos-spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>${project.groupId}</groupId>
            <artifactId>${project.artifactId}-api</artifactId>
        </dependency>
    </dependencies>


</project>$devkit$, 'TEXT', 954, '2cc63448a9e42aa2a84f3b42f4634cb2', 0, 1),
    (2110000000000020102, 2110000000000010006, '[ArtifactId]/[ArtifactId]-bus/src/main/java/[Package]/config/[CamelName]Config.java.ftl', '[CamelName]Config.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName}.config;

import org.springframework.context.annotation.Configuration;

/**
 * 配置类
 *
 * @author carlos
 */
@Configuration
public class ${project.camelName}Config {


}
$devkit$, 'TEXT', 219, 'd0e6cd358f1a72c9a37e03933be7bc04', 0, 1),
    (2110000000000020103, 2110000000000010006, '[ArtifactId]/[ArtifactId]-bus/src/main/java/[Package]/exceptions/[CamelName]Exception.java.ftl', '[CamelName]Exception.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName}.exceptions;

import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 服务主异常
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
public class ${project.camelName}Exception extends ComponentException {

    public ${project.camelName}Exception() {
        super("${project.camelName}  exception!");
    }

    public ${project.camelName}Exception(String message) {
        super(message);
    }

    public ${project.camelName}Exception(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ${project.camelName}Exception(StatusCode errorCode) {
        super(errorCode);
    }

    public ${project.camelName}Exception(Throwable cause) {
        super(cause);
    }

    public ${project.camelName}Exception(String message, Throwable cause) {
        super(message, cause);
    }

}
$devkit$, 'TEXT', 967, 'e610505a617aa52fe60e92cf8926c04b', 0, 1),
    (2110000000000020104, 2110000000000010006, '[ArtifactId]/[ArtifactId]-cloud/pom.xml.ftl', 'pom.xml', 'ftl', 1, FALSE, $devkit$<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>${project.groupId}</groupId>
        <artifactId>${project.artifactId}</artifactId>
        <version>${r'${revision}'}</version>
    </parent>

    <artifactId>${project.artifactId}-cloud</artifactId>

    <dependencies>
        <dependency>
            <groupId>${project.groupId}</groupId>
            <artifactId>${project.artifactId}-bus</artifactId>
        </dependency>
        <dependency>
            <groupId>com.carlos</groupId>
            <artifactId>carlos-spring-cloud-starter</artifactId>
        </dependency>
    </dependencies>

    <build>
        <finalName>${r'${project.parent.artifactId}'}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>$devkit$, 'TEXT', 1638, '57fd53a713ec8e99c219dee46ef4860d', 0, 1),
    (2110000000000020105, 2110000000000010006, '[ArtifactId]/[ArtifactId]-cloud/src/main/java/[Package]/[CamelName]CloudApplication.java.ftl', '[CamelName]CloudApplication.java', 'ftl', 1, FALSE, $devkit$package ${project.packageName};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * <p>
 * 微服务启动类
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ${project.camelName}CloudApplication {

    public static void main ( final String[] args){
        SpringApplication.run(${project.camelName}CloudApplication.class, args);
    }
}
$devkit$, 'TEXT', 692, '0e37f8217f517d2c7231df57b2cdfff1', 0, 1),
    (2110000000000020106, 2110000000000010006, '[ArtifactId]/[ArtifactId]-cloud/src/main/resources/application.yml.ftl', 'application.yml', 'ftl', 1, FALSE, $devkit$server:
port: 8080
spring:
  profiles:
    active: ${r'${PROFILE:local}'}
  application:
name: ${project.artifactId}
config:
import:
# 1. 公共配置（所有环境共享）
#      - optional:nacos:${spring.application.name}.yaml?group=DEFAULT_GROUP&refreshEnabled=true
# 2. 环境专属配置（覆盖公共配置）
#      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yaml?group=DEFAULT_GROUP&refreshEnabled=true
# 3. 额外共享配置（如 Redis、MySQL 配置）
- optional:nacos:common.yml?DEFAULT_GROUP&refreshEnabled=true
- optional:nacos:redis.yml?DEFAULT_GROUP&refreshEnabled=true
- optional:nacos:db.yml?DEFAULT_GROUP&refreshEnabled=true
  cloud:
    nacos:
      discovery:
        enabled: true
      config:
        enabled: true
        file-extension: yml
        refresh-enabled: true

# 本地调试环境
---
spring:
  config:
    activate:
      on-profile: local
  cloud:
    nacos:
      username: ${r'${NACOS_USERNAME:@nacos.username@}'}
      password: ${r'${NACOS_PASSWORD:@nacos.password@}'}
      discovery:
        server-addr: ${r'${NACOS_ADDR:@nacos.addr@}'}
        namespace: ${r'${NACOS_NAMESPACE:@nacos.namespace@}'}
        group: ${r'${NACOS_GROUP:@nacos.group@}'}
      config:
        server-addr: ${r'${NACOS_ADDR:@nacos.addr@}'}
        namespace: ${r'${NACOS_NAMESPACE:@nacos.namespace@}'}
        group: ${r'${NACOS_GROUP:@nacos.group@}'}


# 部署采用配置文件
---
spring:
  config:
    activate:
      on-profile: deploy
  cloud:
    nacos:
      username: ${r'${NACOS_USERNAME}'}
      password: ${r'${NACOS_PASSWORD}'}
      discovery:
        server-addr: ${r'${NACOS_ADDR:127.0.0.1:8848}'}
        namespace: ${r'${NACOS_NAMESPACE}'}
        group: ${r'${NACOS_GROUP}'}
      config:
        namespace: ${r'${NACOS_NAMESPACE}'}
        server-addr: ${r'${NACOS_ADDR}'}
        group: ${r'${NACOS_GROUP}'}
$devkit$, 'TEXT', 1886, 'b31c4f07356f678f65e22fe1b9c7a893', 0, 1),
    (2110000000000020107, 2110000000000010006, '[ArtifactId]/deploy/deploy.yaml.ftl', 'deploy.yaml', 'ftl', 1, FALSE, $devkit$kind: Deployment
apiVersion: apps/v1
metadata:
  name: #APP_NAME
  namespace: #NAMESPACE
  labels:
    app: #APP_NAME
spec:
  replicas: 1
  selector:
    matchLabels:
      app: #APP_NAME
  template:
    metadata:
      labels:
        app: #APP_NAME
    spec:
      automountServiceAccountToken: false
      containers:
        -
          name: #APP_NAME
          image: #APP_IMAGE_NAME
          ports:
            -
              containerPort: 8080
              protocol: TCP
          resources:
            requests:
              cpu: "500m"
              memory: "500m"
            limits:
              cpu: "500m"
              memory: "1Gi"
          env: #ENV_LIST
            -
              name: TZ
              value: Asia/Shanghai
            -
              name: PROFILE
              value: deploy






$devkit$, 'TEXT', 874, '463f346a995d9f9226ab6ad2abfc84df', 0, 1),
    (2110000000000020108, 2110000000000010006, '[ArtifactId]/pom.xml.ftl', 'pom.xml', 'ftl', 1, FALSE, $devkit$<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.carlos</groupId>
        <artifactId>carlos-parent</artifactId>
        <version>3.0.0-SNAPSHOT</version>
    </parent>
    <packaging>pom</packaging>

    <version>${r'${revision}'}</version>
    <groupId>${project.groupId}</groupId>
    <artifactId>${project.artifactId}</artifactId>
    <name>${project.artifactId}</name>
    <description>${project.describe}</description>


    <modules>
        <module>${project.artifactId}-bus</module>
        <module>${project.artifactId}-api</module>
        <module>${project.artifactId}-cloud</module>
        <module>${project.artifactId}-boot</module>
    </modules>
    <modelVersion>4.0.0</modelVersion>

    <properties>
        <revision>${project.version}</revision>
    </properties>


    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>${project.groupId}</groupId>
                <artifactId>${project.artifactId}-bus</artifactId>
                <version>${r'${revision}'}</version>
            </dependency>
            <dependency>
                <groupId>${project.groupId}</groupId>
                <artifactId>${project.artifactId}-api</artifactId>
                <version>${r'${revision}'}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

</project>$devkit$, 'TEXT', 1594, '34cf5b4776958b846dfdffa9ca04edee', 0, 1),
    (2110000000000020109, 2110000000000010006, '[ArtifactId]/sql/[UnderLineName].sql', '[UnderLineName].sql', 'sql', 2, FALSE, $devkit$-- ----------------------------
-- Table structure for test
-- ----------------------------
DROP TABLE IF EXISTS test;
CREATE TABLE test
(
    id           BIGINT      NOT NULL COMMENT '主键ID',
    test_1       VARCHAR(32) NOT NULL COMMENT '模板字段1',
    test_2       VARCHAR(32) NOT NULL COMMENT '模板字段2',
    test_3       VARCHAR(32) NOT NULL COMMENT '模板字段3',
    test_4       VARCHAR(32) NOT NULL COMMENT '模板字段4',
    is_deleted   TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除，0：未删除，1：已删除',
    create_by    CHAR(32)             DEFAULT NULL COMMENT '创建者编号',
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_by    CHAR(32)             DEFAULT NULL COMMENT '更新者编号',
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '模板表名'
  ROW_FORMAT = DYNAMIC;


$devkit$, 'TEXT', 1098, 'c3b18d79b6d53b08a5ed4313dc1e9385', 0, 1)
) AS v(id, group_id, file_path, file_name, file_ext, file_type, loop_flag,
       content, content_encoding, file_size, md5, sort_num, status)
WHERE NOT EXISTS (SELECT 1 FROM dev_template t
                  WHERE t.group_id = v.group_id AND t.file_path = v.file_path AND t.is_deleted = 0);
