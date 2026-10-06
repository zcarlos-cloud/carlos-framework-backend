package com.carlos.message.pojo.param;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MessageCreateParam 嵌套校验回归
 *
 * <p>
 * 背景：{@code receivers} 是 {@code List<ReceiverParam>}，而
 * {@code ReceiverParam.receiverId} 上有 {@code @NotBlank}。
 * Bean Validation **不会**自动级联到集合元素 —— 必须在集合字段上显式加
 * {@code @Valid}，否则那条 {@code @NotBlank} 是条死规则，
 * 接口能建出 receiverId 为空的消息。
 * </p>
 *
 * <p>
 * 这个测试在第 2 个用例上是有意「反向」的：先证明合法参数确实零违规，
 * 防止第 1 个用例因为校验器没装配好而恒真。
 * </p>
 *
 * @author Carlos
 */
class MessageCreateParamValidationTest {

    private static ValidatorFactory validatorFactory;

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("接收者ID为空时必须报错——receivers 上的 @Valid 级联真的生效")
    void should_report_violation_on_receiver_id_when_blank() {
        MessageCreateParam param = validParam();
        param.getReceivers().getFirst().setReceiverId("   ");

        Set<ConstraintViolation<MessageCreateParam>> violations = validator.validate(param);

        assertTrue(
            violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("receiverId")),
            () -> "接收者ID为空白时必须报错，实际 violations=" + describe(violations)
        );
    }

    @Test
    @DisplayName("合法参数零违规——避免上一个断言被误判为恒真")
    void should_have_no_violation_when_param_valid() {
        Set<ConstraintViolation<MessageCreateParam>> violations = validator.validate(validParam());

        assertTrue(violations.isEmpty(), () -> "合法参数不该报错，实际 violations=" + describe(violations));
    }

    private static MessageCreateParam validParam() {
        MessageCreateParam param = new MessageCreateParam();
        param.setTemplateCode("TEMPLATE_A");
        param.setSenderId("1001");
        param.setSenderSystem("carlos-test");

        MessageCreateParam.ReceiverParam receiver = new MessageCreateParam.ReceiverParam();
        receiver.setReceiverId("2002");
        param.setReceivers(List.of(receiver));

        return param;
    }

    private static String describe(Set<ConstraintViolation<MessageCreateParam>> violations) {
        return violations.stream()
            .map(violation -> violation.getPropertyPath() + "=" + violation.getMessage())
            .toList()
            .toString();
    }
}
