package com.carlos.system.i18n.service;

import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.i18n.manager.SysI18nManager;
import com.carlos.system.i18n.pojo.dto.SysI18nMessageDTO;
import com.carlos.system.i18n.pojo.param.SysI18nCreateParam;
import com.carlos.system.i18n.pojo.param.SysI18nUpdateParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 国际化消息 业务服务
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysI18nService {

    private final SysI18nManager i18nManager;

    /**
     * 新增国际化消息
     *
     * @param param 新增参数
     */
    public void add(SysI18nCreateParam param) {
        if (i18nManager.existsByLangModuleKey(param.getLang(), param.getModuleId(), param.getMsgKey())) {
            throw SystemErrorCode.SYS_I18N_KEY_ALREADY_EXISTS.exception();
        }
        SysI18nMessageDTO dto = new SysI18nMessageDTO()
                .setLang(param.getLang())
                .setModuleId(param.getModuleId())
                .setMsgKey(param.getMsgKey())
                .setMsgValue(param.getMsgValue())
                .setDescription(param.getDescription())
                .setMsgStatus(param.getMsgStatus() != null ? param.getMsgStatus() : true);
        i18nManager.add(dto);
    }

    /**
     * 删除国际化消息
     *
     * @param ids 主键集合
     */
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            i18nManager.delete(id);
        }
    }

    /**
     * 更新国际化消息
     *
     * @param param 更新参数
     */
    public void update(SysI18nUpdateParam param) {
        SysI18nMessageDTO existing = i18nManager.getDtoById(param.getId());
        if (existing == null) {
            throw SystemErrorCode.SYS_I18N_NOT_FOUND.exception();
        }
        if (!Objects.equals(param.getLang(), existing.getLang())
                || !Objects.equals(param.getModuleId(), existing.getModuleId())
                || !Objects.equals(param.getMsgKey(), existing.getMsgKey())) {
            if (i18nManager.existsByLangModuleKeyExcludeId(param.getLang(), param.getModuleId(), param.getMsgKey(), param.getId())) {
                throw SystemErrorCode.SYS_I18N_KEY_ALREADY_EXISTS.exception();
            }
        }
        SysI18nMessageDTO dto = new SysI18nMessageDTO()
                .setId(param.getId())
                .setLang(param.getLang())
                .setModuleId(param.getModuleId())
                .setMsgKey(param.getMsgKey())
                .setMsgValue(param.getMsgValue())
                .setDescription(param.getDescription())
                .setMsgStatus(param.getMsgStatus());
        i18nManager.modify(dto);
    }

}
