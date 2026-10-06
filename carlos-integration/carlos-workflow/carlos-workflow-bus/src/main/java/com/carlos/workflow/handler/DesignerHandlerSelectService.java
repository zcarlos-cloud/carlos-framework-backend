package com.carlos.workflow.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgDepartment;
import com.carlos.org.api.ApiOrgRole;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.pojo.ao.OrgDepartmentAO;
import com.carlos.org.api.pojo.ao.OrgRoleAO;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.position.api.ApiOrgPosition;
import com.carlos.org.position.api.pojo.ao.OrgPositionAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.ui.dto.HandlerQuery;
import org.dromara.warm.flow.ui.service.HandlerSelectService;
import org.dromara.warm.flow.ui.vo.HandlerAuth;
import org.dromara.warm.flow.ui.vo.HandlerFeedBackVo;
import org.dromara.warm.flow.ui.vo.HandlerSelectVo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 流程设计器（{@code /warm-flow-ui/**}）的办理人选择器：把「用户 / 角色 / 部门 / 部门(含下级) / 岗位」
 * 五个页签的候选数据从 {@code carlos-org} 取回来，并给出每个候选在
 * {@code flow_node.permission_flag} 里的写法（{@code storageId}）。
 * </p>
 *
 * <h2>装配方式</h2>
 * <p>
 * 本类靠组件扫描注册（{@code com.carlos.workflow.**} 在启动类的扫描范围内），
 * 引擎用 {@code FrameInvoker.getBean(HandlerSelectService.class)} 按类型取到就会用；
 * <b>不能</b>配 {@code warm-flow.handler-select-path} 之类的路径——那条路走
 * {@code Class#getConstructor()}，绕过 Spring 注入。
 * </p>
 * <p>
 * org 契约用 {@link ObjectProvider} 而不是构造器直连：宿主未开启
 * {@code @EnableFeignClients}（例如只装配自动配置的上下文）时容器里没有这些 Feign 客户端，
 * 直连会让<b>整个上下文启动失败</b>；用 ObjectProvider 则本 Bean 照常装配、设计器降级为空列表。
 * 这一点与 {@link OrgPermissionHandler} 用 {@code @ConditionalOnBean} 退让的做法殊途同归，
 * 区别在于本类不归 {@code WorkflowAutoConfiguration} 管（那个类被并行任务占用），
 * 只能靠自身具备「org 缺席也能装配」的能力。
 * </p>
 *
 * <h2>storageId：本类的唯一硬契约</h2>
 * <p>
 * 五个页签产出的写法见 {@link DesignerHandlerType} 的类注释——它们必须逐字等于
 * {@link WorkflowAssigneeVocabulary} 认识的表达式，否则设计者点选出来的办理人在运行期
 * 展开不了（还会被 {@code WorkflowAssigneeFailFastListener} 在创建待办前拦下）。
 * 由 {@code DesignerAssigneeContractTest} 守着这条契约。
 * </p>
 *
 * <h2>没有做的事（刻意）</h2>
 * <ul>
 *   <li><b>不返回左侧树</b>（{@code treeSelections} 一律为空）：本实现把「选人」交给关键字检索 +
 *       分页，避免每次翻页都额外拉一次全量部门树；{@code HandlerQuery#groupId} 因此也不参与筛选。
 *       部门的层级信息仍然可用（部门页签的行本身就是部门，含 {@code groupName} = 上级部门名）；</li>
 *   <li><b>不支持创建时间区间</b>（{@code beginTime} / {@code endTime}）：org 侧的分页参数只有
 *       用户与岗位带创建时间区间，角色与部门没有，五个页签统一忽略比「两个页签生效、三个静默失效」
 *       更可预期；需要时要在 org 侧补带时间条件的分页能力；</li>
 *   <li><b>不猜语义</b>：{@code dept:12} 与 {@code dept:12:rec} 分别由「部门」和「部门(含下级)」
 *       两个页签产出，运行期不做任何推断。</li>
 * </ul>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DesignerHandlerSelectService implements HandlerSelectService {

    /**
     * 每页默认条数，与设计器 SPA 的默认页大小一致
     */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 每页上限：设计器传入的 {@code pageSize} 直接透传给 org，不设上限等于允许一次拉空整张表
     */
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * 批量查名的单次 id 数量上限（org 侧按「建议单次不超过 500 个」约定，避免 GET 查询串过长）
     */
    private static final int ID_BATCH_SIZE = 500;

    /**
     * 一个 storageId 指向多人时（{@code user:1001,1002}）名称的连接符
     */
    private static final String NAME_SEPARATOR = "、";

    private final ObjectProvider<ApiOrgUser> apiOrgUserProvider;

    private final ObjectProvider<ApiOrgRole> apiOrgRoleProvider;

    private final ObjectProvider<ApiOrgDepartment> apiOrgDepartmentProvider;

    private final ObjectProvider<ApiOrgPosition> apiOrgPositionProvider;

    /**
     * 设计器页签，顺序即 SPA 里的页签顺序
     *
     * @return 页签名称列表
     */
    @Override
    public List<String> getHandlerType() {
        return DesignerHandlerType.labels();
    }

    /**
     * 按 {@code handlerType} 分派到对应页签的查询
     *
     * @param query 查询参数（{@code handlerType} 即 {@link #getHandlerType()} 里的某个页签名）
     * @return 候选列表；页签不认识、org 不可用或查询失败时返回空结果（<b>不返回 null</b>，
     *     引擎会把 null 原样塞进响应，SPA 取 {@code data.handlerAuths.rows} 时会直接报错）
     */
    @Override
    public HandlerSelectVo getHandlerSelect(HandlerQuery query) {
        DesignerHandlerType handlerType = query == null ? null : DesignerHandlerType.ofLabel(query.getHandlerType());
        if (handlerType == null) {
            log.warn("设计器办理人列表：handlerType [{}] 不在页签范围内，返回空结果",
                query == null ? null : query.getHandlerType());
            return emptySelect();
        }
        try {
            return switch (handlerType) {
                case USER -> users(query);
                case ROLE -> roles(query);
                case DEPT, DEPT_RECURSIVE -> departments(query, handlerType);
                case POSITION -> positions(query);
            };
        } catch (Exception e) {
            // 引擎会把异常包成 FlowException 抛出，SPA 的 promise 直接 reject，弹窗停在加载态；
            // 降级成空列表 + ERROR 日志，页面至少可用，原因留在日志里
            log.error("设计器办理人列表：[{}] 页签查询失败，返回空结果", handlerType.label(), e);
            return emptySelect();
        }
    }

    /**
     * 办理人名称回显：把 {@code storageIds} 按类型分流后<b>每个类型只查一次</b>，
     * 再按入参顺序逐个返回。
     *
     * <p>默认实现会把每个页签全量拉一遍（{@code getHandlerSelect} 不带分页参数）再建 map，
     * 人一多就是全表扫描；本实现改成按 id 批量查名：用户走
     * {@link ApiOrgUser#listUsersByIds}，角色 / 部门 / 岗位各走一次
     * {@code listXxxOptions}。查不到名字的给空串——设计器按<b>入参顺序</b>对齐显示，
     * 顺序错了比没有名字更糟。</p>
     *
     * @param storageIds 入库主键集合（{@code flow_node.permission_flag} 按 {@code @@} 拆开后的元素）
     * @return 与入参等长、同序的回显对象；入参为空时返回空列表
     */
    @Override
    public List<HandlerFeedBackVo> handlerFeedback(List<String> storageIds) {
        List<HandlerFeedBackVo> feedback = new ArrayList<>();
        if (CollUtil.isEmpty(storageIds)) {
            return feedback;
        }
        Map<String, DesignerStorageId> parsed = new LinkedHashMap<>();
        Map<DesignerHandlerType, Set<Long>> idsByType = new LinkedHashMap<>();
        for (String storageId : storageIds) {
            DesignerStorageId id = DesignerStorageId.parse(storageId);
            if (id == null) {
                continue;
            }
            parsed.put(storageId, id);
            idsByType.computeIfAbsent(id.type(), key -> new LinkedHashSet<>()).addAll(id.ids());
        }

        Map<Long, String> userNames = userNames(ids(idsByType, DesignerHandlerType.USER));
        Map<Long, String> roleNames = roleNames(ids(idsByType, DesignerHandlerType.ROLE));
        Set<Long> deptIds = new LinkedHashSet<>(ids(idsByType, DesignerHandlerType.DEPT));
        deptIds.addAll(ids(idsByType, DesignerHandlerType.DEPT_RECURSIVE));
        Map<Long, String> deptNames = departmentNames(deptIds);
        Map<Long, String> positionNames = positionNames(ids(idsByType, DesignerHandlerType.POSITION));

        for (String storageId : storageIds) {
            feedback.add(new HandlerFeedBackVo(storageId,
                resolveName(parsed.get(storageId), userNames, roleNames, deptNames, positionNames)));
        }
        return feedback;
    }

    // ==================== 各页签的候选查询 ====================

    /**
     * 「用户」页签：分页查询用户，{@code storageId} 是<b>裸用户 id</b>
     *
     * @param query 查询参数
     * @return 候选列表
     */
    private HandlerSelectVo users(HandlerQuery query) {
        ApiOrgUser client = apiOrgUserProvider.getIfAvailable();
        if (client == null) {
            return unavailable(DesignerHandlerType.USER);
        }
        Result<Paging<OrgUserAO>> result = client.pageUserOptions(
            keyword(query.getHandlerCode()), keyword(query.getHandlerName()), pageNum(query), pageSize(query));
        List<HandlerAuth> rows = rows(result).stream()
            .filter(user -> user.getId() != null)
            .map(user -> new HandlerAuth()
                .setStorageId(DesignerHandlerType.USER.storageId(user.getId()))
                .setHandlerCode(user.getAccount())
                .setHandlerName(displayName(user))
                .setCreateTime(createTime(user.getCreateTime())))
            .toList();
        return getResult(rows, total(result)).setTreeSelections(List.of());
    }

    /**
     * 「角色」页签：分页查询角色，{@code storageId} 是 {@code role:{角色id}}
     *
     * @param query 查询参数
     * @return 候选列表
     */
    private HandlerSelectVo roles(HandlerQuery query) {
        ApiOrgRole client = apiOrgRoleProvider.getIfAvailable();
        if (client == null) {
            return unavailable(DesignerHandlerType.ROLE);
        }
        Result<Paging<OrgRoleAO>> result = client.pageRoleOptions(
            keyword(query.getHandlerName()), keyword(query.getHandlerCode()), pageNum(query), pageSize(query));
        List<HandlerAuth> rows = rows(result).stream()
            .filter(role -> role.getId() != null)
            .map(role -> new HandlerAuth()
                .setStorageId(DesignerHandlerType.ROLE.storageId(role.getId()))
                .setHandlerCode(role.getRoleCode())
                .setHandlerName(role.getRoleName())
                .setCreateTime(createTime(role.getCreateTime())))
            .toList();
        return getResult(rows, total(result)).setTreeSelections(List.of());
    }

    /**
     * 「部门」与「部门(含下级)」两个页签：部门清单与关键字过滤都一致，只有 {@code storageId} 不同
     * （{@code dept:{id}} 与 {@code dept:{id}:rec}）——语义差别交给设计者选，不在这里猜。
     *
     * <p>部门一次性取回后在本层过滤、分页：部门量级在组织架构规模内，而 org 侧的部门分页参数
     * 没有「按名称 + 按编码」这套设计器检索条件，多一次往返不如一次取回。</p>
     *
     * @param query       查询参数
     * @param handlerType 页签类型（决定 storageId 的写法）
     * @return 候选列表
     */
    private HandlerSelectVo departments(HandlerQuery query, DesignerHandlerType handlerType) {
        ApiOrgDepartment client = apiOrgDepartmentProvider.getIfAvailable();
        if (client == null) {
            return unavailable(handlerType);
        }
        List<OrgDepartmentAO> matched = list(client.listAllDepartmentOptions()).stream()
            .filter(department -> department.getId() != null)
            .filter(department -> matches(keyword(query.getHandlerName()), department.getDeptName()))
            .filter(department -> matches(keyword(query.getHandlerCode()), department.getDeptCode()))
            .toList();
        Map<Long, String> deptNames = matched.stream()
            .filter(department -> department.getId() != null && StrUtil.isNotBlank(department.getDeptName()))
            .collect(Collectors.toMap(OrgDepartmentAO::getId, OrgDepartmentAO::getDeptName, (left, right) -> left));
        List<HandlerAuth> rows = page(matched, query).stream()
            .map(department -> new HandlerAuth()
                .setStorageId(handlerType.storageId(department.getId()))
                .setHandlerCode(department.getDeptCode())
                .setHandlerName(department.getDeptName())
                .setGroupName(deptNames.get(department.getParentId()))
                .setCreateTime(createTime(department.getCreateTime())))
            .toList();
        return getResult(rows, matched.size()).setTreeSelections(List.of());
    }

    /**
     * 「岗位」页签：分页查询岗位，{@code storageId} 是 {@code position:{岗位id}}
     *
     * @param query 查询参数
     * @return 候选列表
     */
    private HandlerSelectVo positions(HandlerQuery query) {
        ApiOrgPosition client = apiOrgPositionProvider.getIfAvailable();
        if (client == null) {
            return unavailable(DesignerHandlerType.POSITION);
        }
        Result<Paging<OrgPositionAO>> result = client.pagePositionOptions(
            keyword(query.getHandlerName()), keyword(query.getHandlerCode()), pageNum(query), pageSize(query));
        List<HandlerAuth> rows = rows(result).stream()
            .filter(position -> position.getId() != null)
            .map(position -> new HandlerAuth()
                .setStorageId(DesignerHandlerType.POSITION.storageId(position.getId()))
                .setHandlerCode(position.getPositionCode())
                .setHandlerName(position.getPositionName())
                .setCreateTime(createTime(position.getCreateTime())))
            .toList();
        return getResult(rows, total(result)).setTreeSelections(List.of());
    }

    // ==================== 批量查名（回显） ====================

    private Map<Long, String> userNames(Collection<Long> ids) {
        ApiOrgUser client = apiOrgUserProvider.getIfAvailable();
        if (client == null || CollUtil.isEmpty(ids)) {
            return Map.of();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        for (List<Long> batch : batches(ids)) {
            try {
                merge(names, list(client.listUsersByIds(batch)), OrgUserAO::getId,
                    DesignerHandlerSelectService::displayName);
            } catch (Exception e) {
                log.error("设计器办理人回显：批量查询用户 [{}] 失败，这些 storageId 的名字回显为空", batch, e);
            }
        }
        return names;
    }

    private Map<Long, String> roleNames(Collection<Long> ids) {
        ApiOrgRole client = apiOrgRoleProvider.getIfAvailable();
        if (client == null || CollUtil.isEmpty(ids)) {
            return Map.of();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        for (List<Long> batch : batches(ids)) {
            try {
                merge(names, list(client.listRoleOptions(batch)), OrgRoleAO::getId, OrgRoleAO::getRoleName);
            } catch (Exception e) {
                log.error("设计器办理人回显：批量查询角色 [{}] 失败，这些 storageId 的名字回显为空", batch, e);
            }
        }
        return names;
    }

    private Map<Long, String> departmentNames(Collection<Long> ids) {
        ApiOrgDepartment client = apiOrgDepartmentProvider.getIfAvailable();
        if (client == null || CollUtil.isEmpty(ids)) {
            return Map.of();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        for (List<Long> batch : batches(ids)) {
            try {
                merge(names, list(client.listDepartmentOptions(batch)), OrgDepartmentAO::getId,
                    OrgDepartmentAO::getDeptName);
            } catch (Exception e) {
                log.error("设计器办理人回显：批量查询部门 [{}] 失败，这些 storageId 的名字回显为空", batch, e);
            }
        }
        return names;
    }

    private Map<Long, String> positionNames(Collection<Long> ids) {
        ApiOrgPosition client = apiOrgPositionProvider.getIfAvailable();
        if (client == null || CollUtil.isEmpty(ids)) {
            return Map.of();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        for (List<Long> batch : batches(ids)) {
            try {
                merge(names, list(client.listPositionOptions(batch)), OrgPositionAO::getId,
                    OrgPositionAO::getPositionName);
            } catch (Exception e) {
                log.error("设计器办理人回显：批量查询岗位 [{}] 失败，这些 storageId 的名字回显为空", batch, e);
            }
        }
        return names;
    }

    /**
     * 取某个 storageId 的显示名：一个 token 指向多人时（{@code user:1001,1002}）用顿号连接，
     * 一个都查不到时返回空串
     *
     * @param id             解析结果；null 表示这个 token 不是本设计器认识的形态
     * @param userNames      用户 id → 名称
     * @param roleNames      角色 id → 名称
     * @param deptNames      部门 id → 名称
     * @param positionNames  岗位 id → 名称
     * @return 显示名（可能为空串）
     */
    private static String resolveName(DesignerStorageId id, Map<Long, String> userNames, Map<Long, String> roleNames,
                                      Map<Long, String> deptNames, Map<Long, String> positionNames) {
        if (id == null) {
            return StrUtil.EMPTY;
        }
        Map<Long, String> names = switch (id.type()) {
            case USER -> userNames;
            case ROLE -> roleNames;
            case DEPT, DEPT_RECURSIVE -> deptNames;
            case POSITION -> positionNames;
        };
        return id.ids().stream()
            .map(names::get)
            .filter(StrUtil::isNotBlank)
            .collect(Collectors.joining(NAME_SEPARATOR));
    }

    private static <T> void merge(Map<Long, String> names, List<T> rows, Function<T, Long> idGetter,
                                  Function<T, String> nameGetter) {
        for (T row : rows) {
            Long id = idGetter.apply(row);
            String name = nameGetter.apply(row);
            if (id != null && StrUtil.isNotBlank(name)) {
                names.putIfAbsent(id, name);
            }
        }
    }

    private static List<List<Long>> batches(Collection<Long> ids) {
        return CollUtil.split(new ArrayList<>(ids), ID_BATCH_SIZE);
    }

    private static Set<Long> ids(Map<DesignerHandlerType, Set<Long>> idsByType, DesignerHandlerType type) {
        return idsByType.getOrDefault(type, Set.of());
    }

    // ==================== 取值与小工具 ====================

    /**
     * 用户显示名：优先昵称，缺了退回账号（org 的昵称不是必填）
     */
    private static String displayName(OrgUserAO user) {
        return StrUtil.blankToDefault(user.getNickname(), user.getAccount());
    }

    private static String createTime(LocalDateTime time) {
        return time == null ? null : LocalDateTimeUtil.formatNormal(time);
    }

    /**
     * 空结果：{@code code}/{@code msg} 由接口默认实现 {@link HandlerSelectService#getResult} 填，
     * 与官方实现保持一致（SPA 不校验它们，但别让字段空着）
     */
    private HandlerSelectVo emptySelect() {
        return getResult(List.of(), 0).setTreeSelections(List.of());
    }

    /**
     * 某个页签依赖的 org 契约未注册时的降级：页签照常显示，列表为空
     */
    private HandlerSelectVo unavailable(DesignerHandlerType handlerType) {
        log.warn("设计器办理人列表：[{}] 页签依赖的 org 契约未注册，返回空结果", handlerType.label());
        return emptySelect();
    }

    private static <T> List<T> rows(Result<Paging<T>> result) {
        if (!succeeded(result) || result.getData().getRecords() == null) {
            return List.of();
        }
        return result.getData().getRecords().stream().filter(Objects::nonNull).toList();
    }

    private static <T> long total(Result<Paging<T>> result) {
        return succeeded(result) ? result.getData().getTotal() : 0L;
    }

    private static <T> List<T> list(Result<List<T>> result) {
        if (result == null || !Boolean.TRUE.equals(result.getSuccess()) || CollUtil.isEmpty(result.getData())) {
            return List.of();
        }
        return result.getData().stream().filter(Objects::nonNull).toList();
    }

    /**
     * 调用是否成功且带回了分页数据；降级工厂（{@code Result.error}）与 org 不可用都走这里
     */
    private static boolean succeeded(Result<?> result) {
        return result != null && Boolean.TRUE.equals(result.getSuccess()) && result.getData() != null;
    }

    private static String keyword(String value) {
        return StrUtil.trimToNull(value);
    }

    private static boolean matches(String keyword, String value) {
        return keyword == null || StrUtil.containsIgnoreCase(value, keyword);
    }

    private static int pageNum(HandlerQuery query) {
        Integer value = query == null ? null : query.getPageNum();
        return value == null || value < 1 ? 1 : value;
    }

    private static int pageSize(HandlerQuery query) {
        Integer value = query == null ? null : query.getPageSize();
        if (value == null || value < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(value, MAX_PAGE_SIZE);
    }

    /**
     * 内存分页：部门列表一次性取回，分页在这里做
     */
    private static <T> List<T> page(List<T> all, HandlerQuery query) {
        int from = Math.min((pageNum(query) - 1) * pageSize(query), all.size());
        return all.subList(from, Math.min(from + pageSize(query), all.size()));
    }

}
