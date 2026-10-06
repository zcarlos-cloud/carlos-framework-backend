package com.carlos.workflow.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 办理人表达式的<b>词汇表唯一实现</b>：把 {@code flow_node.permission_flag} 拆成表达式，
 * 识别类型，并判定参数是否合法。规范见 {@code docs/design/workflow-assignee-expression-spec.md} §3。
 * </p>
 * <p>
 * 本类刻意做成无状态静态工具：{@link OrgPermissionHandler}（运行期展开）与
 * {@link WorkflowAssigneeValidator}（保存期校验）必须共用<b>同一份</b>词汇知识，
 * 各自维护一份类型表迟早会漂移——比如新增一个类型只改了展开侧，保存侧就会把它判成非法。
 * </p>
 *
 * <h2>三种 token 形态</h2>
 * <ul>
 *   <li>{@link Kind#KNOWN}：形如 {@code 类型:参数}，类型在 {@link Type} 内。参数是否合法由
 *       {@link Expression#malformed()} 给出，展开侧据此直接跳过，不再各自重复校验；</li>
 *   <li>{@link Kind#UNKNOWN_TYPE}：有 {@code 类型:参数} 形状但类型不在词汇表内（如 {@code manager:5}）。
 *       这类标识参与交集永远不会命中办理人，既不展开也不应放过；</li>
 *   <li>{@link Kind#LITERAL}：无冒号，即引擎侧 {@code #}/{@code $}/无前缀三种策略里的「无前缀字面量」——
 *       SpEL 与流程变量求值后的用户 id、或裸用户 id 都会落到这里，原样交给引擎比较。</li>
 * </ul>
 *
 * <h2>为什么不解析 {@code #} / {@code $}</h2>
 * <p>
 * 引擎按前缀分派策略（{@code #} = SpEL，{@code $} = 流程变量，无前缀 = 字面量），
 * 求值发生在 {@code ExpressionUtil#evalVariable} 里，早于本词汇表的展开时机。
 * 因此 {@code #}/{@code $} 开头的 token 到这里时<b>已经求值完毕</b>：
 * 求值成功是用户 id（{@link Kind#LITERAL}），求值结果为空则被引擎<span>原样保留为表达式字符串</span>
 * （见 {@link #isDynamicExpression(String)} 的用法），本类不做二次求值。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public final class WorkflowAssigneeVocabulary {

    /**
     * 表达式分隔符，与引擎 {@code FlowCons.SPLIT_AT} 一致
     */
    public static final String EXPRESSION_SEPARATOR = "@@";

    /**
     * {@code user:} 多个用户 id 之间的分隔符
     */
    public static final String USER_ID_SEPARATOR = ",";

    /**
     * 类型与参数、参数与参数之间的分隔符
     */
    private static final String PARAM_SEPARATOR = ":";

    /**
     * {@code dept:{id}:rec} 的递归标记
     */
    private static final String DEPT_RECURSIVE_FLAG = "rec";

    /**
     * 向上解析部门层级的步数上限，防御 {@code parentId} 成环或配置离谱的层级参数
     */
    private static final int MAX_DEPT_LEVELS = 20;

    private static final Map<String, Type> LITERAL_TYPES = new HashMap<>();

    static {
        for (Type type : Type.values()) {
            LITERAL_TYPES.put(type.literal, type);
        }
    }

    private WorkflowAssigneeVocabulary() {
    }

    /**
     * 词法种类，决定 token 是「可展开」「类型不认识」还是「字面量透传」
     */
    public enum Kind {

        /**
         * 类型在词汇表内
         */
        KNOWN,

        /**
         * 有 {@code 类型:参数} 形状，但类型不在词汇表内
         */
        UNKNOWN_TYPE,

        /**
         * 无 {@code 类型:参数} 形状，原样交给引擎比较
         */
        LITERAL
    }

    /**
     * 静态词汇表（规范 §3）的类型
     */
    public enum Type {

        USER("user"),
        ROLE("role"),
        DEPT("dept"),
        DEPT_LEADER("deptLeader"),
        POSITION("position"),
        ROLE_IN_DEPT("roleInDept");

        private final String literal;

        Type(String literal) {
            this.literal = literal;
        }

        /**
         * 表达式里书写的类型名
         *
         * @return 类型名
         */
        public String literal() {
            return literal;
        }

        /**
         * 按类型名查词汇表
         *
         * @param literal 表达式里书写的类型名
         * @return 类型；不在词汇表内时为 null
         */
        public static Type of(String literal) {
            return LITERAL_TYPES.get(literal);
        }
    }

    /**
     * 解析后的单条表达式
     *
     * @param raw       原始 token（已去空白）
     * @param kind      词法种类
     * @param type      静态词汇类型；仅 {@link Kind#KNOWN} 时非空
     * @param params    {@code 类型:} 之后的冒号分隔参数；仅 {@link Kind#KNOWN} 时非空
     * @param malformed 参数不合法（非数字、缺参、多参、层级越界等）；仅 {@link Kind#KNOWN} 时可能为 true
     */
    public record Expression(String raw, Kind kind, Type type, List<String> params, boolean malformed) {
    }

    /**
     * 拆分并解析一个 {@code permission_flag}（可含多个 {@code @@} 表达式）
     *
     * @param permissionFlag 节点上配置的办理人表达式串
     * @return 解析结果；空串或纯空白返回空列表
     */
    public static List<Expression> parse(String permissionFlag) {
        List<Expression> expressions = new ArrayList<>();
        if (StrUtil.isBlank(permissionFlag)) {
            return expressions;
        }
        for (String token : permissionFlag.split(EXPRESSION_SEPARATOR)) {
            Expression expression = parseToken(token);
            if (expression != null) {
                expressions.add(expression);
            }
        }
        return expressions;
    }

    /**
     * 解析引擎传入的表达式列表，容忍单个元素内仍含 {@code @@} 分隔符
     *
     * @param permissions 引擎传入的表达式列表
     * @return 解析结果
     */
    public static List<Expression> parseAll(List<String> permissions) {
        List<Expression> expressions = new ArrayList<>();
        if (CollUtil.isEmpty(permissions)) {
            return expressions;
        }
        for (String permission : permissions) {
            expressions.addAll(parse(permission));
        }
        return expressions;
    }

    /**
     * 挑出配置里无法解析的表达式，供保存期校验使用
     *
     * @param permissionFlag 节点上配置的办理人表达式串
     * @return 有问题的原始表达式（非空串 {@code permissionFlag} 却一个 token 都解析不出时，返回该串本身）
     */
    public static List<String> invalidExpressions(String permissionFlag) {
        List<String> invalid = new ArrayList<>();
        if (StrUtil.isBlank(permissionFlag)) {
            return invalid;
        }
        List<Expression> expressions = parse(permissionFlag);
        if (expressions.isEmpty()) {
            invalid.add(permissionFlag);
            return invalid;
        }
        for (Expression expression : expressions) {
            if (!acceptable(expression)) {
                invalid.add(expression.raw());
            }
        }
        return invalid;
    }

    /**
     * 是否为 SpEL（{@code #}）或流程变量（{@code $}）形式的动态表达式，与引擎按前缀分派策略的口径一致
     *
     * <p>另一个用途：求值后的办理人列表里若仍带着这个前缀，说明引擎求值为空后回退保留了原表达式字符串
     * （{@code ExpressionUtil#evalVariable} 的兜底），该 token 谁也匹配不上。</p>
     *
     * @param token 表达式或求值结果
     * @return 以 {@code #} 或 {@code $} 开头时为 true
     */
    public static boolean isDynamicExpression(String token) {
        if (StrUtil.isEmpty(token)) {
            return false;
        }
        return token.startsWith("#") || token.startsWith("$");
    }

    /**
     * 是否为裸用户 id（无前缀字面量里唯一可被引擎交集命中的形态）
     *
     * @param token 表达式
     * @return 可解析为数字时为 true
     */
    public static boolean isPlainUserId(String token) {
        return Convert.toLong(token, null) != null;
    }

    /**
     * 逗号分隔的用户 id 是否全部可解析（{@code user:1001,1002}）
     *
     * @param param {@code user:} 之后的参数
     * @return 用户 id 列表；任一段不是数字时为 null
     */
    public static List<Long> userIds(String param) {
        if (StrUtil.isBlank(param)) {
            return null;
        }
        List<Long> userIds = new ArrayList<>();
        for (String value : param.split(USER_ID_SEPARATOR)) {
            Long id = Convert.toLong(value, null);
            if (id == null) {
                return null;
            }
            userIds.add(id);
        }
        return userIds;
    }

    /**
     * 单个数字参数（{@code role:5} 这类一参一 id 的形状）
     *
     * @param param 参数
     * @return 参数值；不是数字时为 null
     */
    public static Long id(String param) {
        return Convert.toLong(param, null);
    }

    /**
     * {@code deptLeader:{id}:{n}} 的 n，即自 deptId 起向上第 n 级
     *
     * @param param 参数
     * @return 层级；非数字或超出 {@value #MAX_DEPT_LEVELS} 时为 null
     */
    public static Integer level(String param) {
        Integer level = Convert.toInt(param, null);
        if (level == null || level < 1 || level > MAX_DEPT_LEVELS) {
            return null;
        }
        return level;
    }

    private static Expression parseToken(String token) {
        String raw = StrUtil.trimToEmpty(token);
        if (raw.isEmpty()) {
            return null;
        }
        int separatorIndex = raw.indexOf(PARAM_SEPARATOR);
        if (separatorIndex < 0) {
            return new Expression(raw, Kind.LITERAL, null, null, false);
        }
        Type type = Type.of(raw.substring(0, separatorIndex));
        if (type == null) {
            return new Expression(raw, Kind.UNKNOWN_TYPE, null, null, false);
        }
        List<String> params = List.of(raw.substring(separatorIndex + 1).split(PARAM_SEPARATOR));
        return new Expression(raw, Kind.KNOWN, type, params, !paramsValid(type, params));
    }

    private static boolean paramsValid(Type type, List<String> params) {
        return switch (type) {
            case USER -> params.size() == 1 && userIds(params.get(0)) != null;
            case ROLE, POSITION -> singleId(params);
            case ROLE_IN_DEPT -> params.size() == 2 && id(params.get(0)) != null && id(params.get(1)) != null;
            case DEPT -> singleId(params)
                || (params.size() == 2 && id(params.get(0)) != null && DEPT_RECURSIVE_FLAG.equals(params.get(1)));
            case DEPT_LEADER -> singleId(params)
                || (params.size() == 2 && id(params.get(0)) != null && level(params.get(1)) != null);
        };
    }

    private static boolean singleId(List<String> params) {
        return params.size() == 1 && id(params.get(0)) != null;
    }

    private static boolean acceptable(Expression expression) {
        return switch (expression.kind()) {
            case KNOWN -> !expression.malformed();
            case UNKNOWN_TYPE -> false;
            case LITERAL -> isDynamicExpression(expression.raw()) || isPlainUserId(expression.raw());
        };
    }

}
