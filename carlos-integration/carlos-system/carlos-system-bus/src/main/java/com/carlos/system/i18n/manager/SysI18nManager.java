package com.carlos.system.i18n.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.system.i18n.pojo.dto.SysI18nMessageDTO;
import com.carlos.system.i18n.pojo.entity.SysI18nMessage;
import com.carlos.system.i18n.pojo.param.SysI18nPageParam;
import com.carlos.system.i18n.pojo.vo.SysI18nMessageVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 国际化消息 查询封装接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
public interface SysI18nManager extends BaseService<SysI18nMessage> {

    /**
     * 新增国际化消息
     *
     * @param dto 数据
     * @return boolean
     */
    boolean add(SysI18nMessageDTO dto);

    /**
     * 删除国际化消息
     *
     * @param id 主键
     * @return boolean
     */
    boolean delete(Long id);

    /**
     * 修改国际化消息
     *
     * @param dto 数据
     * @return boolean
     */
    boolean modify(SysI18nMessageDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键
     * @return SysI18nMessageDTO
     */
    SysI18nMessageDTO getDtoById(Long id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @return Paging<SysI18nMessageVO>
     */
    Paging<SysI18nMessageVO> getPage(SysI18nPageParam param);

    /**
     * 根据语言和模块编码获取消息映射
     *
     * @param lang         语言标识
     * @param moduleCodes  模块编码列表（可选）
     * @return Map<msgKey, msgValue>
     */
    Map<String, String> getMessagesByLangAndModules(String lang, List<String> moduleCodes);

    /**
     * 根据语言、模块ID和消息键判断是否存在
     *
     * @param lang     语言标识
     * @param moduleId 模块ID
     * @param msgKey   消息键
     * @return true 存在
     */
    boolean existsByLangModuleKey(String lang, Long moduleId, String msgKey);

    /**
     * 根据语言、模块ID和消息键判断是否存在（排除指定ID）
     *
     * @param lang     语言标识
     * @param moduleId 模块ID
     * @param msgKey   消息键
     * @param excludeId 排除的主键ID
     * @return true 存在
     */
    boolean existsByLangModuleKeyExcludeId(String lang, Long moduleId, String msgKey, Long excludeId);

    /**
     * 根据消息键获取消息
     *
     * @param lang   语言标识
     * @param msgKey 消息键
     * @return SysI18nMessageDTO
     */
    SysI18nMessageDTO getByLangAndKey(String lang, String msgKey);

}
