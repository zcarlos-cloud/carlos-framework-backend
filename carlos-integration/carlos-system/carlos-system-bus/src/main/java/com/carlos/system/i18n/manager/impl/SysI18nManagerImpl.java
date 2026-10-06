package com.carlos.system.i18n.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.system.i18n.convert.SysI18nConvert;
import com.carlos.system.i18n.manager.SysI18nManager;
import com.carlos.system.i18n.mapper.SysI18nMapper;
import com.carlos.system.i18n.pojo.dto.SysI18nMessageDTO;
import com.carlos.system.i18n.pojo.entity.SysI18nMessage;
import com.carlos.system.i18n.pojo.param.SysI18nPageParam;
import com.carlos.system.i18n.pojo.vo.SysI18nMessageVO;
import com.carlos.system.menu.manager.SysModuleManager;
import com.carlos.system.menu.pojo.entity.SysModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 国际化消息 查询封装实现类
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysI18nManagerImpl extends BaseServiceImpl<SysI18nMapper, SysI18nMessage> implements SysI18nManager {

    private final SysModuleManager moduleManager;

    @Override
    public boolean add(SysI18nMessageDTO dto) {
        SysI18nMessage entity = SysI18nConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'SysI18nMessage' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        log.debug("Insert 'SysI18nMessage' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        boolean success = removeById(id);
        if (!success) {
            log.warn("Remove 'SysI18nMessage' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'SysI18nMessage' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(SysI18nMessageDTO dto) {
        SysI18nMessage entity = SysI18nConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'SysI18nMessage' data fail, entity:{}", entity);
            return false;
        }
        log.debug("Update 'SysI18nMessage' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public SysI18nMessageDTO getDtoById(Long id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        SysI18nMessage entity = getBaseMapper().selectById(id);
        return SysI18nConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<SysI18nMessageVO> getPage(SysI18nPageParam param) {
        LambdaQueryWrapper<SysI18nMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                SysI18nMessage::getId,
                SysI18nMessage::getLang,
                SysI18nMessage::getModuleId,
                SysI18nMessage::getMsgKey,
                SysI18nMessage::getMsgValue,
                SysI18nMessage::getDescription,
                SysI18nMessage::getMsgStatus,
                SysI18nMessage::getCreateBy,
                SysI18nMessage::getCreateTime,
                SysI18nMessage::getUpdateBy,
                SysI18nMessage::getUpdateTime
        );
        wrapper.orderByDesc(SysI18nMessage::getCreateTime);

        if (StrUtil.isNotBlank(param.getLang())) {
            wrapper.eq(SysI18nMessage::getLang, param.getLang());
        }
        if (param.getModuleId() != null) {
            wrapper.eq(SysI18nMessage::getModuleId, param.getModuleId());
        }
        if (StrUtil.isNotBlank(param.getMsgKey())) {
            wrapper.like(SysI18nMessage::getMsgKey, param.getMsgKey());
        }
        if (param.getMsgStatus() != null) {
            wrapper.eq(SysI18nMessage::getMsgStatus, param.getMsgStatus());
        }

        PageInfo<SysI18nMessage> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, SysI18nConvert.INSTANCE::do2vo);
    }

    @Override
    public Map<String, String> getMessagesByLangAndModules(String lang, List<String> moduleCodes) {
        LambdaQueryWrapper<SysI18nMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysI18nMessage::getMsgKey, SysI18nMessage::getMsgValue)
                .eq(SysI18nMessage::getLang, lang)
                .eq(SysI18nMessage::getMsgStatus, true);

        if (moduleCodes != null && !moduleCodes.isEmpty()) {
            List<Long> moduleIds = moduleCodes.stream()
                    .map(code -> {
                        SysModule module = moduleManager.getByModuleCode(code);
                        return module != null ? module.getId() : null;
                    })
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            if (moduleIds.isEmpty()) {
                // 指定了模块编码但全部无效，返回空结果
                return new LinkedHashMap<>();
            }
            wrapper.in(SysI18nMessage::getModuleId, moduleIds);
        }

        List<SysI18nMessage> list = list(wrapper);
        Map<String, String> messages = new LinkedHashMap<>();
        for (SysI18nMessage msg : list) {
            String previous = messages.put(msg.getMsgKey(), msg.getMsgValue());
            if (previous != null) {
                log.warn("Duplicate i18n key found: lang={}, moduleId={}, key={}",
                        lang, msg.getModuleId(), msg.getMsgKey());
            }
        }
        return messages;
    }

    @Override
    public SysI18nMessageDTO getByLangAndKey(String lang, String msgKey) {
        if (StrUtil.isBlank(lang) || StrUtil.isBlank(msgKey)) {
            return null;
        }
        SysI18nMessage entity = lambdaQuery()
                .eq(SysI18nMessage::getLang, lang)
                .eq(SysI18nMessage::getMsgKey, msgKey)
                .eq(SysI18nMessage::getMsgStatus, true)
                .one();
        return SysI18nConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public boolean existsByLangModuleKey(String lang, Long moduleId, String msgKey) {
        if (StrUtil.isBlank(lang) || StrUtil.isBlank(msgKey)) {
            return false;
        }
        LambdaQueryWrapper<SysI18nMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysI18nMessage::getLang, lang)
                .eq(SysI18nMessage::getMsgKey, msgKey);
        if (moduleId != null) {
            wrapper.eq(SysI18nMessage::getModuleId, moduleId);
        } else {
            wrapper.isNull(SysI18nMessage::getModuleId);
        }
        return count(wrapper) > 0;
    }

    @Override
    public boolean existsByLangModuleKeyExcludeId(String lang, Long moduleId, String msgKey, Long excludeId) {
        if (StrUtil.isBlank(lang) || StrUtil.isBlank(msgKey) || excludeId == null) {
            return false;
        }
        LambdaQueryWrapper<SysI18nMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysI18nMessage::getLang, lang)
                .eq(SysI18nMessage::getMsgKey, msgKey)
                .ne(SysI18nMessage::getId, excludeId);
        if (moduleId != null) {
            wrapper.eq(SysI18nMessage::getModuleId, moduleId);
        } else {
            wrapper.isNull(SysI18nMessage::getModuleId);
        }
        return count(wrapper) > 0;
    }

}
