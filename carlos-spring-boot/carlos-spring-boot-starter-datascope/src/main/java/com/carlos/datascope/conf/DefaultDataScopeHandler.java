package com.carlos.datascope.conf;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.caller.CallerUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.carlos.core.exception.ComponentException;
import com.carlos.datascope.*;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 默认数据权限处理器实现
 *
 * @author Carlos
 * @date 2022/11/21 15:41
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultDataScopeHandler implements DataScopeHandler {

    private static final TransmittableThreadLocal<List<DataScope>> THREAD_LOCAL = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<Map<String, Object>> PARAM_THREAD_LOCAL = new TransmittableThreadLocal<>();

    private final DataScopeProvider provider;

    @Override
    public void handle(DataScope[] scopes) {
        if (ArrayUtil.isEmpty(scopes)) {
            return;
        }

        THREAD_LOCAL.set(Arrays.asList(scopes));
    }

    @Override
    public void exit() {
        THREAD_LOCAL.remove();
        PARAM_THREAD_LOCAL.remove();
    }

    @Override
    public DataScopeInfo getScopeInfo(String methodPoint) {
        List<DataScope> scopes = THREAD_LOCAL.get();

        if (CollUtil.isEmpty(scopes)) {
            return null;
        }

        List<String> split = StrUtil.split(methodPoint, StrUtil.DOT);
        int index = split.size();
        // 获取mapper调用的方法
        String method = split.get(index - 1);
        // 获取mapper对象的名字
        String mapper = split.get(index - 2);

        for (DataScope scope : scopes) {
            Class<?> caller = scope.caller();
            // 注解匹配
            if (caller != null && !CallerUtil.isCalledBy(caller)) {
                continue;
            }
            String point = scope.methodPoint();
            List<String> splitMethod = StrUtil.split(point, StrUtil.DOT);
            int size = splitMethod.size();
            // 获取mapper调用的方法
            String methodName = splitMethod.get(size - 1);
            // 获取mapper对象的名字
            String mapperName = splitMethod.get(size - 2);
            if (!mapper.equals(mapperName)) {
                continue;
            }
            if (!method.equals(methodName)) {
                continue;
            }
            log.info("Point {} match @DataScope [{}]", methodPoint, scope);
            return buildScopeInfo(scope);
        }
        return null;
    }

    @Override
    public void handleParam(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DefaultParameterNameDiscoverer u =
            new DefaultParameterNameDiscoverer();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = u.getParameterNames(method);
        HashMap<String, Object> map = Maps.newHashMap();
        if (args != null && paramNames != null) {
            for (int i = 0; i < args.length; i++) {
                map.put(paramNames[i], args[i]);
            }
        }
        PARAM_THREAD_LOCAL.set(map);
    }

    private DataScopeInfo buildScopeInfo(DataScope scope) {
        DataScopeInfo info = new DataScopeInfo();
        info.setColumn(scope.field());

        Set<Serializable> sets = Sets.newHashSet();
        switch (scope.type()) {
            case NONE -> {
                info.setColumn("0");
                sets = Sets.newHashSet("1");
            }
            case ALL -> {
                info.setColumn("1");
                sets = Sets.newHashSet("1");
            }
            case CURRENT_DEPT ->
                // 获取当前部门的所有人
                sets = provider.currentDeptUserIds();
            case DEPT_AND_SUB ->
                // 当前部门以及所有子部门所有人
                sets = provider.currentDeptAllUserIds(null);
            case CURRENT_ROLE ->
                // 当前角色下所有人
                sets = provider.currentRoleUserIds();
            case CURRENT_USER ->
                // 当前用户
                sets = Sets.newHashSet(provider.currentUserId());
            case CUSTOM -> {
                Class<? extends CustomScope> handler = scope.handler();
                CustomScope customScope;
                try {
                    customScope = handler.getDeclaredConstructor().newInstance();
                } catch (Throwable e) {
                    throw new ComponentException("自定义权限处理器实例创建失败", e);
                }
                sets = customScope.accept(PARAM_THREAD_LOCAL.get());
            }
            default -> {
            }
        }
        info.setItems(sets);
        return info;
    }
}
