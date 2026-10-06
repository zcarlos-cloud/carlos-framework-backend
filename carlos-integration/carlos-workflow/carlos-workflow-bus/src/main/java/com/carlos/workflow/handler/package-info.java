/**
 * <p>
 * 与 Warm-Flow 引擎的缝合层：引擎在流转过程中回调的扩展点实现。
 * </p>
 * <p>
 * 本包当前只放办理人解析相关实现：
 * </p>
 * <ul>
 *   <li>{@code OrgPermissionHandler} —— {@code PermissionHandler} 实现，把设计器里配置的
 *       办理人表达式（{@code role:5} / {@code dept:12} 等）展开成用户 id；</li>
 *   <li>{@code WorkflowAssignees} —— SpEL 助手 Bean（{@code @workflowAssignees}），
 *       承载依赖上下文的动态办理人词汇。</li>
 * </ul>
 * <p>
 * 词汇表以 {@code docs/design/workflow-assignee-expression-spec.md} 为准。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
package com.carlos.workflow.handler;
