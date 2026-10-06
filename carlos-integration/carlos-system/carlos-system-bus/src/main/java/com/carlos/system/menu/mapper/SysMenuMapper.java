package com.carlos.system.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.system.menu.pojo.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 系统菜单 查询接口
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {


}
