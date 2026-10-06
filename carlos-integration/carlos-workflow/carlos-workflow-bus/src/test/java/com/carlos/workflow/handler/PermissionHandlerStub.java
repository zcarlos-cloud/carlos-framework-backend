package com.carlos.workflow.handler;

import org.dromara.warm.flow.core.handler.PermissionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 办理人权限处理器桩实现，<b>仅测试作用域</b>，用于证明「宿主注册了 PermissionHandler 时引擎能取到」。
 * </p>
 * <p>
 * <b>本类不得作为生产 Bean 注册。</b>{@code permissions()} 返回空列表，而引擎的
 * {@code TaskServiceImpl#checkAuth} 会取「当前用户权限标识」与任务办理人求交集；
 * 节点一旦配了办理人，审批动作必然以 20704 失败——一个看起来能跑、实际全坏的兜底实现，
 * 比不注册更难排查。生产装配由 {@code WorkflowAutoConfiguration} 以
 * {@code @Bean + @ConditionalOnMissingBean} 注册真实的 {@code OrgPermissionHandler}，
 * 本类只在测试里用它证明「宿主注册了处理器时引擎能取到，且生产实现会退让」。
 * </p>
 * <p>
 * 本类没有无参构造器，因此也不能通过 {@code warm-flow.permission-handler-path} 反射实例化，
 * 该配置项必须保持为空。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public class PermissionHandlerStub implements PermissionHandler {

    private final String handlerId;

    public PermissionHandlerStub(String handlerId) {
        this.handlerId = handlerId;
    }

    @Override
    public List<String> permissions() {
        return new ArrayList<>();
    }

    @Override
    public String getHandler() {
        return handlerId;
    }

    @Override
    public List<String> convertPermissions(List<String> permissions) {
        return permissions == null ? new ArrayList<>() : new ArrayList<>(permissions);
    }
}
