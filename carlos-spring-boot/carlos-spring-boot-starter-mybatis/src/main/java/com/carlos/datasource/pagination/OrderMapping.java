package com.carlos.datasource.pagination;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.carlos.datasource.utils.PropertyColumnUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 排序列 数据库字段名和属性名 映射
 *
 * @author carlos
 * @date 2020/3/14
 **/
@Data
@Accessors(chain = true)
public class OrderMapping {

    /**
     * 是否将驼峰命名转为下划线
     */
    private boolean underLineMode;

    public OrderMapping(boolean underLineMode) {
        this.underLineMode = underLineMode;
    }

    private Map<String, String> map = new ConcurrentHashMap<>();

    /**
     * 属性和表字段映射
     *
     * @param property 属性名称
     * @param column   行字段名称
     * @author carlos
     * @date 2020/4/14 1:39
     */
    public OrderMapping mapping(String property, String column) {
        map.put(property, column);
        return this;
    }

    /**
     * 属性和表字段映射
     *
     * @param property    属性名称
     * @param tablePrefix 表前缀
     * @param column      行字段名称
     * @author carlos
     * @date 2020-06-15 09:56:58
     */
    public void mapping(String property, String tablePrefix, String column) {
        if (StrUtil.isNotBlank(tablePrefix)) {
            column = tablePrefix + "." + column;
        }
        map.put(property, column);
    }

    /**
     * 属性和表字段映射
     *
     * @param property 属性名称
     * @param clazz    对象Class
     * @author carlos
     * @date 2020/4/14 1:26
     */
    public OrderMapping mapping(String property, Class<?> clazz) {
        String column = PropertyColumnUtil.getColumn(clazz, property);
        map.put(property, column);
        return this;
    }

    public OrderMapping mapping(String property, String tablePrefix, Class<?> clazz) {
        String column = PropertyColumnUtil.getColumn(clazz, property);
        mapping(property, tablePrefix, column);
        return this;
    }

    /**
     * 获取属性名对应的列
     *
     * @param property 属性名
     * @author carlos
     * @date 2020/4/14 18:10
     */
    public String getMappingColumn(String property) {
        if (StrUtil.isBlank(property)) {
            return null;
        }
        return map.get(property);
    }

    /**
     * 处理排序字段
     *
     * @param orderItems 排序字段
     * @author carlos
     * @date 2020/4/14 17:35
     */
    public void mappingOrderItems(List<OrderItem> orderItems) {
        // 如果集合不为空，则按照PropertyColumnUtil映射
        if (MapUtil.isNotEmpty(map)) {
            orderItems.forEach(item -> item.setColumn(this.getMappingColumn(item.getColumn())));
        } else if (underLineMode) {
            // 如果开启下划线模式，自动转换成下划线
            orderItems.forEach(item -> {
                String column = item.getColumn();
                if (StrUtil.isNotBlank(column)) {
                    // 驼峰转换成下划线
                    item.setColumn(StrUtil.toUnderlineCase(column));
                }
            });
        }
    }

}
