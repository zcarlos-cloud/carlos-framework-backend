/**
 * <p>
 * 工作流管理端 Web 控制器：给前端用的 {@code /workflow/**} 端点，经网关暴露。
 * </p>
 * <p>
 * 与 {@code com.carlos.workflow.apiimpl} 的 Feign 契约实现（{@code /api/workflow/**}）是两套命名空间：
 * 网关只路由 {@code /<服务>/**}，{@code /api/**} 不经网关、只给服务间 Feign 调用。
 * 本包下的类不得 implements {@code com.carlos.workflow.api} 包里的任何接口。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
package com.carlos.workflow.controller;
