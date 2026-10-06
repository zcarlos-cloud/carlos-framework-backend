<?xml version="1.0" encoding="UTF-8"?>
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
