package com.carlos.workflow.handler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * {@link DesignerHandlerType} 与 {@link DesignerStorageId} 的「写法 ↔ 拆解」往返测试。
 * </p>
 * <p>
 * 与词汇表的一致性不在本类断言（那是 {@link DesignerAssigneeContractTest} 的职责），
 * 本类只回答一个问题：设计器产出的字符串，回显侧能不能拆回同一个类型和同一个主键。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class DesignerStorageIdTest {

    // ==================== 页签与 storageId ====================

    @Test
    void formatsStorageIdForEveryTab() {
        assertThat(DesignerHandlerType.USER.storageId(1001L))
            .as("「用户」页签产出裸用户 id（官方约定，无 user: 前缀）")
            .isEqualTo("1001");
        assertThat(DesignerHandlerType.ROLE.storageId(5L)).isEqualTo("role:5");
        assertThat(DesignerHandlerType.DEPT.storageId(12L))
            .as("「部门」页签是默认不含下级的语义，只写 dept:{id}")
            .isEqualTo("dept:12");
        assertThat(DesignerHandlerType.DEPT_RECURSIVE.storageId(12L))
            .as("「部门(含下级)」页签必须带上 rec 标记，两个页签不能产出同一种写法")
            .isEqualTo("dept:12:rec");
        assertThat(DesignerHandlerType.POSITION.storageId(7L)).isEqualTo("position:7");
    }

    @Test
    void returnsNullStorageIdForNullId() {
        for (DesignerHandlerType type : DesignerHandlerType.values()) {
            assertThat(type.storageId(null)).as("%s 的主键为空时不应拼出一个假 storageId", type.label()).isNull();
        }
    }

    @Test
    void labelsAreListedInDesignerTabOrder() {
        assertThat(DesignerHandlerType.labels())
            .containsExactly("用户", "角色", "部门", "部门(含下级)", "岗位");
    }

    @Test
    void ofLabelDispatchesByExactLabelAndRejectsUnknown() {
        assertThat(DesignerHandlerType.ofLabel("部门(含下级)")).isEqualTo(DesignerHandlerType.DEPT_RECURSIVE);
        assertThat(DesignerHandlerType.ofLabel("  角色 ")).isEqualTo(DesignerHandlerType.ROLE);
        assertThat(DesignerHandlerType.ofLabel("用户")).isEqualTo(DesignerHandlerType.USER);
        assertThat(DesignerHandlerType.ofLabel(null)).isNull();
        assertThat(DesignerHandlerType.ofLabel("")).isNull();
        assertThat(DesignerHandlerType.ofLabel("部门(含子部门)")).isNull();
        assertThat(DesignerHandlerType.ofLabel("USER")).as("标签页名是中文，枚举名不接受").isNull();
    }

    // ==================== 拆解 ====================

    @Test
    void parsesEveryTabStorageIdBackToTypeAndId() {
        assertParsed("1001", DesignerHandlerType.USER, 1001L);
        assertParsed("role:5", DesignerHandlerType.ROLE, 5L);
        assertParsed("dept:12", DesignerHandlerType.DEPT, 12L);
        assertParsed("dept:12:rec", DesignerHandlerType.DEPT_RECURSIVE, 12L);
        assertParsed("position:7", DesignerHandlerType.POSITION, 7L);
    }

    @Test
    void parsesHandWrittenUserPrefixIncludingCommaSeparatedIds() {
        DesignerStorageId single = DesignerStorageId.parse("user:1001");
        assertThat(single).isNotNull();
        assertThat(single.type()).isEqualTo(DesignerHandlerType.USER);
        assertThat(single.ids()).containsExactly(1001L);

        DesignerStorageId parsed = DesignerStorageId.parse("user:1001,1002");
        assertThat(parsed).isNotNull();
        assertThat(parsed.type()).isEqualTo(DesignerHandlerType.USER);
        assertThat(parsed.ids())
            .as("user:{id,id} 是规范 §3 的静态词汇，手写配置会遇到，回显要能拆出多个 id")
            .containsExactly(1001L, 1002L);
    }

    @Test
    void deptAndDeptRecursivePointAtTheSameDepartmentButAreDifferentTokens() {
        DesignerStorageId plain = DesignerStorageId.parse("dept:12");
        DesignerStorageId recursive = DesignerStorageId.parse("dept:12:rec");

        assertThat(plain).isNotNull();
        assertThat(recursive).isNotNull();
        assertThat(plain.ids()).isEqualTo(recursive.ids());
        assertThat(plain.type()).isNotEqualTo(recursive.type());
    }

    @Test
    void returnsNullForTokensThisDesignerDoesNotProduce() {
        assertThat(DesignerStorageId.parse(null)).isNull();
        assertThat(DesignerStorageId.parse("")).isNull();
        assertThat(DesignerStorageId.parse("   ")).isNull();
        assertThat(DesignerStorageId.parse("dept:")).as("缺参数").isNull();
        assertThat(DesignerStorageId.parse("dept:12:xxx")).as("第二个参数不是 rec").isNull();
        assertThat(DesignerStorageId.parse("dept:12:rec:extra")).as("参数过多").isNull();
        assertThat(DesignerStorageId.parse("role:abc")).isNull();
        assertThat(DesignerStorageId.parse("user:1001,abc")).isNull();
        assertThat(DesignerStorageId.parse("manager:5")).as("未知类型").isNull();
        assertThat(DesignerStorageId.parse("deptLeader:12"))
            .as("deptLeader 没有页签产出，回显留空由 SPA 退回显示原文")
            .isNull();
        assertThat(DesignerStorageId.parse("roleInDept:5:12")).isNull();
        assertThat(DesignerStorageId.parse("#{@workflowAssignees.upLeader(#applicantId)}"))
            .as("SpEL 表达式不是 storageId")
            .isNull();
        assertThat(DesignerStorageId.parse("zhangsan")).as("裸的非数字字面量").isNull();
    }

    private static void assertParsed(String storageId, DesignerHandlerType type, Long id) {
        DesignerStorageId parsed = DesignerStorageId.parse(storageId);
        assertThat(parsed).as("storageId [%s] 应能拆解", storageId).isNotNull();
        assertThat(parsed.type()).isEqualTo(type);
        assertThat(parsed.ids()).containsExactly(id);
        assertThat(type.storageId(id)).as("拆解结果重新拼回应与原文逐字相同").isEqualTo(storageId);
    }

    @Test
    void parsesEveryTabFormattedTokenBackToItself() {
        List<Long> ids = List.of(1L, 12L, 1001L, 9007199254740993L);
        for (DesignerHandlerType type : DesignerHandlerType.values()) {
            for (Long id : ids) {
                String storageId = type.storageId(id);
                DesignerStorageId parsed = DesignerStorageId.parse(storageId);
                assertThat(parsed)
                    .as("[%s] 页签产出的 [%s] 应能被回显侧拆解", type.label(), storageId)
                    .isNotNull();
                assertThat(parsed.type()).isEqualTo(type);
                assertThat(parsed.ids()).containsExactly(id);
            }
        }
    }

}
