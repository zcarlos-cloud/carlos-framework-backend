/**
 * <p>
 * 工作流 Feign 契约实现：服务间调用端点（{@code /api/workflow/**}），网关不路由 {@code /api/**}。
 * </p>
 * <p>
 * 管理端（前端经网关）的端点在同模块的 {@code com.carlos.workflow.controller} 包下，
 * 前缀 {@code /workflow/**}，两套命名空间互不混入。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
package com.carlos.workflow.apiimpl;
