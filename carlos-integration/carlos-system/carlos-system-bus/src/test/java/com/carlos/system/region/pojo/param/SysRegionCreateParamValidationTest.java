package com.carlos.system.region.pojo.param;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SysRegionCreateParam 校验回归
 *
 * <p>
 * 背景：{@code parentCode} 此前带 {@code @NotBlank}，而库中 {@code parent_code} 可空、
 * 后端全链路（{@code RegionConstant.TOP_REGION_PARENT_CODE}、逐级加载、按 code 导入、
 * 区域树取根）都以字面量 "0" 认根 —— 结果是**顶级区域在接口层就被拒**，
 * 前端无论怎么做都建不出第一个根节点。
 * </p>
 *
 * <p>
 * 第 2 个用例是有意「反向」的：先证明合法参数确实零违规，
 * 防止第 1 个用例因为校验器没装配好（violations 恒为空）而被误判为通过。
 * </p>
 *
 * @author Carlos
 */
class SysRegionCreateParamValidationTest {

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
    @DisplayName("parentCode 为空时必须放行——否则顶级区域永远建不出来")
    void should_allow_blank_parent_code_for_top_level_region() {
        SysRegionCreateParam param = validParam();
        param.setParentCode(null);

        Set<ConstraintViolation<SysRegionCreateParam>> violations = validator.validate(param);

        assertTrue(
            violations.stream().noneMatch(v -> v.getPropertyPath().toString().contains("parentCode")),
            () -> "parentCode 为空表示顶级区域，不该被拒，实际 violations=" + describe(violations)
        );
    }

    @Test
    @DisplayName("合法参数零违规——避免上一个断言被误判为恒真")
    void should_have_no_violation_when_param_valid() {
        Set<ConstraintViolation<SysRegionCreateParam>> violations = validator.validate(validParam());

        assertTrue(violations.isEmpty(), () -> "合法参数不该报错，实际 violations=" + describe(violations));
    }

    @Test
    @DisplayName("经度越界必须报错——NUMERIC(20,10) 存得下但地理上无意义")
    void should_report_violation_on_longitude_out_of_range() {
        SysRegionCreateParam param = validParam();
        param.setLongitude(new BigDecimal("200"));

        Set<ConstraintViolation<SysRegionCreateParam>> violations = validator.validate(param);

        assertTrue(
            violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("longitude")),
            () -> "经度 200 必须报错，实际 violations=" + describe(violations)
        );
    }

    @Test
    @DisplayName("纬度越界必须报错")
    void should_report_violation_on_latitude_out_of_range() {
        SysRegionCreateParam param = validParam();
        param.setLatitude(new BigDecimal("-91"));

        Set<ConstraintViolation<SysRegionCreateParam>> violations = validator.validate(param);

        assertTrue(
            violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("latitude")),
            () -> "纬度 -91 必须报错，实际 violations=" + describe(violations)
        );
    }

    @Test
    @DisplayName("区域编码超长必须报错——region_code 是 VARCHAR(50)")
    void should_report_violation_on_region_code_too_long() {
        SysRegionCreateParam param = validParam();
        param.setRegionCode("R".repeat(51));

        Set<ConstraintViolation<SysRegionCreateParam>> violations = validator.validate(param);

        assertTrue(
            violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("regionCode")),
            () -> "51 位的区域编码必须报错，实际 violations=" + describe(violations)
        );
    }

    private static SysRegionCreateParam validParam() {
        SysRegionCreateParam param = new SysRegionCreateParam();
        param.setRegionCode("330100");
        param.setRegionName("杭州市");
        param.setParentCode("330000");
        param.setLongitude(new BigDecimal("120.1550700000"));
        param.setLatitude(new BigDecimal("30.2740840000"));
        return param;
    }

    private static String describe(Set<ConstraintViolation<SysRegionCreateParam>> violations) {
        return violations.stream()
            .map(violation -> violation.getPropertyPath() + "=" + violation.getMessage())
            .toList()
            .toString();
    }
}
