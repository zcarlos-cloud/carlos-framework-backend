package com.carlos.workflow.handler;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 把一个 {@code storageId} 拆回「标签页类型 + 主键」，供设计器回显办理人名称
 * （{@code handlerFeedback}）时按类型分流批量查名。
 * </p>
 *
 * <h2>为什么不用 {@link WorkflowAssigneeVocabulary} 来拆</h2>
 * <p>
 * 两者目的不同，刻意不互相复用：
 * </p>
 * <ul>
 *   <li>词汇表把 {@code dept:12} 与 {@code dept:12:rec} 都判成 {@code Type.DEPT}（运行期展开时
 *       再按参数个数分流），而回显只需要「查到名字」，两个形态查的是同一个部门；</li>
 *   <li>词汇表把裸数字判成 {@code Kind.LITERAL}（它无从知道那是用户 id），而回显必须知道
 *       「裸数字 = 用户」才能去 org 查名字。</li>
 * </ul>
 * <p>
 * 因此本类按<b>设计器实际会产出的 storageId 形态</b>（见 {@link DesignerHandlerType}）来拆，
 * 并额外兼容手写配置里常见的 {@code user:1001} / {@code user:1001,1002}。
 * 与词汇表的一致性由 {@code DesignerAssigneeContractTest} 保证：本类产出的每一种写法都要能被
 * 词汇表判成预期类型；反过来，本类拆不出的写法（{@code deptLeader:12}、{@code roleInDept:5:12}、
 * {@code manager:5} 等）回显为空串，设计器会退回显示 storageId 原文。
 * </p>
 *
 * @param type 标签页类型
 * @param ids  该 storageId 指向的主键（单元素；{@code user:1001,1002} 这类手写写法会有多个）
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public record DesignerStorageId(DesignerHandlerType type, List<Long> ids) {

    /**
     * {@code user:} 前缀，设计器不产出，但规范 §3 的静态词汇里有，手写配置会遇到
     */
    public static final String PREFIX_USER = "user";

    /**
     * {@code role:} 前缀，与 {@link WorkflowAssigneeVocabulary.Type#ROLE} 的字面量一致
     */
    public static final String PREFIX_ROLE = "role";

    /**
     * {@code dept:} 前缀，与 {@link WorkflowAssigneeVocabulary.Type#DEPT} 的字面量一致
     */
    public static final String PREFIX_DEPT = "dept";

    /**
     * {@code position:} 前缀，与 {@link WorkflowAssigneeVocabulary.Type#POSITION} 的字面量一致
     */
    public static final String PREFIX_POSITION = "position";

    /**
     * {@code dept:{id}:rec} 的递归标记。
     *
     * <p>必须与 {@link WorkflowAssigneeVocabulary} 里判定合法性的递归标记逐字相同
     * （那边是 private 常量，引用不到），因此由 {@code DesignerAssigneeContractTest}
     * 用词汇表反查「{@code dept:12:rec} 是不是合法表达式」来守住这个常量——
     * 词汇表换了写法，测试立刻变红，而不是等到设计者配好流程才发现展开不了。</p>
     */
    public static final String RECURSIVE_FLAG = "rec";

    /**
     * {@code 类型:参数} 的分隔符，同时也是 {@code dept:{id}:rec} 里参数与标记的分隔符
     */
    public static final String PARAM_SEPARATOR = ":";

    /**
     * 解析一个 storageId
     *
     * @param storageId 入库主键，如 {@code role:5}、{@code dept:12:rec}、{@code 1001}
     * @return 解析结果；为空、非数字、前缀不认识或参数形状不认识时返回 null（调用方按「查不到名字」处理）
     */
    public static DesignerStorageId parse(String storageId) {
        String raw = StrUtil.trimToEmpty(storageId);
        if (raw.isEmpty()) {
            return null;
        }
        int separatorIndex = raw.indexOf(PARAM_SEPARATOR);
        if (separatorIndex < 0) {
            // 无冒号：设计器「用户」页签产出的是裸 id（词汇表里属于 Kind.LITERAL）
            Long userId = id(raw);
            return userId == null ? null : new DesignerStorageId(DesignerHandlerType.USER, List.of(userId));
        }
        String prefix = raw.substring(0, separatorIndex);
        String params = raw.substring(separatorIndex + 1);
        return switch (prefix) {
            case PREFIX_USER -> of(DesignerHandlerType.USER, ids(params));
            case PREFIX_ROLE -> of(DesignerHandlerType.ROLE, singleId(params));
            case PREFIX_POSITION -> of(DesignerHandlerType.POSITION, singleId(params));
            case PREFIX_DEPT -> dept(params);
            default -> null;
        };
    }

    /**
     * 参数解析不出来时返回 null，而不是留一个 {@code ids} 为空的半成品——
     * 调用方只判断 {@code == null}，半成品会让「查不到名字」变成「拿一个空 list 去查」。
     *
     * @param type 标签页类型
     * @param ids  解析出的主键；null 表示参数不合法
     * @return 解析结果
     */
    private static DesignerStorageId of(DesignerHandlerType type, List<Long> ids) {
        return ids == null ? null : new DesignerStorageId(type, ids);
    }

    /**
     * {@code dept:{id}} 与 {@code dept:{id}:rec}：两种写法查的是同一个部门，只是展开语义不同
     *
     * <p>按「第一个冒号切两段」的思路：前缀是 {@code dept}，参数是 {@code 12} 或 {@code 12:rec}，
     * 取参数的第一段作为部门 id；带 {@code rec} 的写法映射到
     * {@link DesignerHandlerType#DEPT_RECURSIVE}，回显时两者查同一个名字。</p>
     *
     * @param params {@code dept:} 之后的全部内容
     * @return 解析结果；参数形状不认识时返回 null
     */
    private static DesignerStorageId dept(String params) {
        String[] segments = params.split(PARAM_SEPARATOR);
        if (segments.length == 1) {
            return of(DesignerHandlerType.DEPT, singleId(segments[0]));
        }
        if (segments.length == 2 && RECURSIVE_FLAG.equals(segments[1])) {
            return of(DesignerHandlerType.DEPT_RECURSIVE, singleId(segments[0]));
        }
        return null;
    }

    private static List<Long> singleId(String value) {
        Long id = id(value);
        return id == null ? null : List.of(id);
    }

    private static List<Long> ids(String value) {
        List<Long> ids = new ArrayList<>();
        for (String segment : value.split(WorkflowAssigneeVocabulary.USER_ID_SEPARATOR)) {
            Long id = id(segment);
            if (id == null) {
                return null;
            }
            ids.add(id);
        }
        return ids;
    }

    /**
     * 数字参数，与 {@link WorkflowAssigneeVocabulary#id(String)} 同一口径
     *
     * @param value 待解析的片段
     * @return 主键；不是数字时为 null
     */
    private static Long id(String value) {
        return Convert.toLong(value, null);
    }

}
