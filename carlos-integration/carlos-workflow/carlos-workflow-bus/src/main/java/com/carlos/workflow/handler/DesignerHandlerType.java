package com.carlos.workflow.handler;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * <p>
 * 流程设计器（{@code /warm-flow-ui/**}）的办理人标签页，以及每个标签页点选后产出的
 * {@code storageId}。
 * </p>
 *
 * <h2>标签页名字是 opaque 的</h2>
 * <p>
 * {@link org.dromara.warm.flow.ui.service.HandlerSelectService#getHandlerType()} 返回的字符串由设计器
 * SPA 原样放进 {@code HandlerQuery#handlerType} 再发回来（引擎只做透传，既不解释它也不落库），
 * 因此它只是<b>一次请求内的往返标识</b>。取中文名与官方示例一致，省掉一层「名字 → 语义」的映射表。
 * </p>
 *
 * <h2>storageId 就是 flow_node.permission_flag 里的表达式</h2>
 * <p>
 * 设计器把选中的办理人按 {@code storageId} 用 {@code @@}（引擎 {@code FlowCons.SPLIT_AT}，
 * 也是 {@link WorkflowAssigneeVocabulary#EXPRESSION_SEPARATOR}）拼进 {@code flow_node.permission_flag}，
 * 运行期由 {@link OrgPermissionHandler} 交给 {@link WorkflowAssigneeVocabulary} 解析后展开成用户 id。
 * 因此每个标签页产出的 {@code storageId} <b>必须逐字等于</b>词汇表里的写法
 * （规范见 {@code docs/design/workflow-assignee-expression-spec.md} §3）：
 * </p>
 * <table border="1">
 *   <caption>标签页与 storageId 的对应关系</caption>
 *   <tr><th>标签页</th><th>storageId</th><th>词汇表判定</th><th>运行期展开</th></tr>
 *   <tr><td>用户</td><td>{@code 1001}</td><td>{@code Kind.LITERAL}（裸数字原样透传）</td><td>字面量，与当前用户 id 直接比较</td></tr>
 *   <tr><td>角色</td><td>{@code role:5}</td><td>{@code Type.ROLE}</td><td>角色下的用户</td></tr>
 *   <tr><td>部门</td><td>{@code dept:12}</td><td>{@code Type.DEPT}（单参数）</td><td>本部门用户，<b>不含</b>下级</td></tr>
 *   <tr><td>部门(含下级)</td><td>{@code dept:12:rec}</td><td>{@code Type.DEPT}（两参数，第二个为 rec）</td><td>本部门及全部子孙部门用户</td></tr>
 *   <tr><td>岗位</td><td>{@code position:7}</td><td>{@code Type.POSITION}</td><td>岗位上的用户</td></tr>
 * </table>
 *
 * <h2>两个刻意的取舍</h2>
 * <ul>
 *   <li><b>部门拆成两个标签页</b>：{@code dept:12} 与 {@code dept:12:rec} 是两种不同的语义，
 *       设计者点选时必须自己决定要不要含下级——只给一种再在运行期改语义会让「配了什么」不可预测；</li>
 *   <li><b>「用户」产出裸 id</b>（无 {@code user:} 前缀）：裸数字被词汇表判为 {@code Kind.LITERAL}，
 *       与 {@link OrgPermissionHandler#permissions()} 返回的当前用户 id 逐字相同，引擎求交集时直接命中；
 *       裸数字同时被保存期校验（{@link WorkflowAssigneeValidator}）判为合法。
 *       {@code user:1001} 同样能展开（{@code Type.USER}），但没必要多写一层前缀。</li>
 * </ul>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public enum DesignerHandlerType {

    /**
     * 用户：storageId 为裸用户 id
     */
    USER("用户", id -> String.valueOf(id)),

    /**
     * 角色：storageId 为 {@code role:{角色id}}
     */
    ROLE("角色", id -> DesignerStorageId.PREFIX_ROLE + DesignerStorageId.PARAM_SEPARATOR + id),

    /**
     * 部门：storageId 为 {@code dept:{部门id}}，展开语义为「本部门，不含下级」
     */
    DEPT("部门", id -> DesignerStorageId.PREFIX_DEPT + DesignerStorageId.PARAM_SEPARATOR + id),

    /**
     * 部门(含下级)：storageId 为 {@code dept:{部门id}:rec}，展开语义为「本部门及其全部子孙部门」
     */
    DEPT_RECURSIVE("部门(含下级)", id -> DesignerStorageId.PREFIX_DEPT + DesignerStorageId.PARAM_SEPARATOR
        + id + DesignerStorageId.PARAM_SEPARATOR + DesignerStorageId.RECURSIVE_FLAG),

    /**
     * 岗位：storageId 为 {@code position:{岗位id}}
     */
    POSITION("岗位", id -> DesignerStorageId.PREFIX_POSITION + DesignerStorageId.PARAM_SEPARATOR + id);

    private final String label;

    private final Function<Long, String> storageId;

    DesignerHandlerType(String label, Function<Long, String> storageId) {
        this.label = label;
        this.storageId = storageId;
    }

    /**
     * 标签页名称，即 {@code getHandlerType()} 的元素
     *
     * @return 标签页名称
     */
    public String label() {
        return label;
    }

    /**
     * 该标签页下某个主键对应的 {@code storageId}
     *
     * @param id 用户 / 角色 / 部门 / 岗位主键
     * @return storageId；id 为 null 时返回 null
     */
    public String storageId(Long id) {
        return id == null ? null : storageId.apply(id);
    }

    /**
     * 全部标签页名称，顺序即设计器里的页签顺序
     *
     * @return 标签页名称列表
     */
    public static List<String> labels() {
        return Arrays.stream(values()).map(DesignerHandlerType::label).toList();
    }

    /**
     * 按标签页名称分派
     *
     * @param label 设计器回传的 {@code HandlerQuery#handlerType}
     * @return 标签页类型；不认识时返回 null
     */
    public static DesignerHandlerType ofLabel(String label) {
        if (label == null) {
            return null;
        }
        String trimmed = label.trim();
        for (DesignerHandlerType type : values()) {
            if (type.label.equals(trimmed)) {
                return type;
            }
        }
        return null;
    }

}
