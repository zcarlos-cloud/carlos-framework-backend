/**
 * <p>
 * 数据查询封装层：把流程定义表的读取收敛到一处，Service 层只消费 DTO。
 * </p>
 * <p>
 * 引擎实体 {@code org.dromara.warm.flow.orm.entity.FlowDefinition} 本身就是 MyBatis-Plus 的
 * 表映射对象（{@code @TableName("flow_definition")}），因此本层不需要自己的 Entity，
 * 也不需要 XML 映射文件——查询一律用条件构造器拼装。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
package com.carlos.workflow.manager;
