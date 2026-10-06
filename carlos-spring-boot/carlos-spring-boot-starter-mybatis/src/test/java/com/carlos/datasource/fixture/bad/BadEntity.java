package com.carlos.datasource.fixture.bad;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 校验器测试用的「违规」实体：fill 落在无策略字段上（Boolean enabled）
 */
@Data
@TableName("fixture_bad_entity")
public class BadEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "enabled", fill = FieldFill.INSERT)
    private Boolean enabled;

    @TableField(value = "version", fill = FieldFill.INSERT_UPDATE)
    private Integer version;

    /**
     * 瞬态字段（exist = false）不参与 SQL，即使有 fill 注解也不算违规
     */
    @TableField(value = "transient_flag", fill = FieldFill.INSERT, exist = false)
    private Boolean transientFlag;
}
