package com.carlos.auth.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.auth.app.pojo.entity.AppCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用分类 Mapper
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Mapper
public interface AppCategoryMapper extends BaseMapper<AppCategory> {
}
