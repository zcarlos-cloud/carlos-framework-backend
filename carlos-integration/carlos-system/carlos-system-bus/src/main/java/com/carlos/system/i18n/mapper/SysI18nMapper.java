package com.carlos.system.i18n.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.system.i18n.pojo.entity.SysI18nMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 国际化消息 查询接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Mapper
public interface SysI18nMapper extends BaseMapper<SysI18nMessage> {

}
